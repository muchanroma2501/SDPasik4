(() => {
    "use strict";

    const $ = (selector, root = document) => root.querySelector(selector);
    const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
    const HISTORY_KEY = "pc-build-studio-history-v1";
    const catalogs = new Map();
    let activeCatalog = null;
    let catalogRequestId = 0;
    let diagnosisRequestId = 0;
    let diagnosisController = null;
    let diagnosisTimer = 0;
    let recentActions = loadHistory();
    let reportDownload = null;
    let lastDiagnostics = null;
    let buildInProgress = false;

    async function requestJson(url, options = {}) {
        const response = await fetch(url, options);
        const text = await response.text();
        let data = null;
        if (text) {
            try {
                data = JSON.parse(text);
            } catch {
                data = null;
            }
        }
        if (!response.ok) {
            const error = new Error(data?.message || data?.detail || `Ошибка запроса (${response.status}).`);
            error.payload = data;
            error.status = response.status;
            throw error;
        }
        if (text && data === null) {
            throw new Error("Сервер вернул ответ в неожиданном формате.");
        }
        return data;
    }

    function apiPost(url, body, signal) {
        return requestJson(url, {
            method: "POST",
            headers: { "Content-Type": "application/json", "Accept": "application/json" },
            body: JSON.stringify(body),
            signal
        });
    }

    function setNotice(message, isError = false) {
        const notice = $("#build-notice");
        notice.textContent = message;
        notice.classList.toggle("is-error", isError);
    }

    function configFromForm() {
        return {
            ecosystem: $('[name="ecosystem"]:checked')?.value || "",
            cpu: $("#cpu").value,
            gpu: $("#gpu").value,
            motherboard: $("#motherboard").value,
            ram: $("#ram").value,
            storage: $("#storage").value,
            powerSupply: $("#psu").value,
            coolingSystem: $("#cooling").value,
            hasRGB: $("#rgb").checked
        };
    }

    function addOption(select, value, label) {
        const option = document.createElement("option");
        option.value = value;
        option.textContent = label;
        select.append(option);
    }

    function setOptions(id, items, labelFor = item => item.model ?? item) {
        const select = $("#" + id);
        select.replaceChildren();
        for (const item of items) {
            const value = item.model ?? item;
            addOption(select, value, labelFor(item));
        }
        select.disabled = items.length === 0;
    }

    async function getCatalog(ecosystem) {
        if (!catalogs.has(ecosystem)) {
            catalogs.set(ecosystem, requestJson(`/api/pc/catalog?ecosystem=${encodeURIComponent(ecosystem)}`));
        }
        try {
            return await catalogs.get(ecosystem);
        } catch (error) {
            catalogs.delete(ecosystem);
            throw error;
        }
    }

    async function loadCatalog(ecosystem, preserve = null) {
        const requestId = ++catalogRequestId;
        clearTimeout(diagnosisTimer);
        diagnosisController?.abort();
        diagnosisRequestId++;
        lastDiagnostics = null;
        $("#diagnostics-loading").textContent = "Загружаем каталог компонентов…";
        $("#build-button").disabled = true;
        const catalog = await getCatalog(ecosystem);
        if (requestId !== catalogRequestId) return;
        activeCatalog = catalog;

        setOptions("cpu", catalog.cpus,
            item => `${item.model} · ${item.tdpWatts} Вт · ${item.socket}`);
        setOptions("gpu", catalog.gpus, item => `${item.model} · ${item.tdpWatts} Вт`);
        setOptions("motherboard", catalog.motherboards, item => `${item.model} · ${item.socket}`);
        setOptions("ram", catalog.ram);
        setOptions("storage", catalog.storage);
        setOptions("psu", catalog.powerSupplies);
        setOptions("cooling", catalog.coolingSystems);
        for (const key of ["cpu", "gpu", "motherboard", "ram", "storage", "powerSupply", "coolingSystem"]) {
            const id = key === "powerSupply" ? "psu" : key === "coolingSystem" ? "cooling" : key;
            if (preserve?.[key] && [...$("#" + id).options].some(option => option.value === preserve[key])) {
                $("#" + id).value = preserve[key];
            }
        }
        $("#diagnostics-loading").textContent = "Каталог загружен. Выполняется серверная проверка…";
        updateSummary();
        scheduleDiagnosis(0);
    }

    function updateSummary() {
        const values = {
            cpu: $("#cpu").value,
            gpu: $("#gpu").value,
            motherboard: $("#motherboard").value,
            psu: $("#psu").value
        };
        $("#summary-cpu").textContent = values.cpu || "—";
        $("#summary-gpu").textContent = values.gpu || "—";
        $("#summary-motherboard").textContent = values.motherboard || "—";
        $("#summary-psu").textContent = values.psu || "—";
    }

    function setOverallStatus(status, label) {
        const chip = $("#overall-status");
        chip.className = `status-chip ${status}`;
        chip.textContent = label;
    }

    function renderDiagnostics(diagnostics) {
        lastDiagnostics = diagnostics;
        const statusLabels = { READY: ["status-ready", "ГОТОВО"], REVIEW: ["status-review", "ЕСТЬ ЗАМЕЧАНИЯ"], BLOCKED: ["status-blocked", "ЗАБЛОКИРОВАНО"] };
        const [className, label] = statusLabels[diagnostics.status] || ["status-pending", "ОЖИДАНИЕ"];
        setOverallStatus(className, label);
        $("#diagnostics-loading").textContent = diagnostics.status === "BLOCKED"
            ? "Сборка заблокирована до устранения ошибок."
            : diagnostics.status === "REVIEW"
                ? "Ошибок нет, но рекомендуется учесть замечания."
                : "Проверка завершена без замечаний.";
        const power = $("#power-summary");
        power.hidden = diagnostics.requiredPowerWatts == null;
        power.replaceChildren();
        if (!power.hidden) {
            const metrics = [
                ["Расчётный минимум", diagnostics.requiredPowerWatts],
                ["Рекомендуемый ориентир", diagnostics.recommendedPowerWatts],
                ["Выбранный БП", diagnostics.selectedPowerWatts]
            ];
            for (const [labelText, value] of metrics) {
                const metric = document.createElement("div");
                metric.className = "power-metric";
                const labelNode = document.createElement("span");
                labelNode.textContent = labelText;
                const valueNode = document.createElement("strong");
                valueNode.textContent = value == null ? "—" : `${value} Вт`;
                metric.append(labelNode, valueNode);
                power.append(metric);
            }
        }

        const checks = $("#diagnostic-checks");
        checks.replaceChildren();
        for (const check of diagnostics.checks || []) {
            const item = document.createElement("li");
            const severityClass = check.severity === "PASS" ? "check-pass"
                : check.severity === "WARNING" ? "check-warning" : "check-error";
            item.className = `diagnostic-check ${severityClass}`;
            const mark = document.createElement("span");
            mark.className = "check-mark";
            mark.textContent = check.severity === "PASS" ? "✓" : check.severity === "WARNING" ? "!" : "×";
            const text = document.createElement("div");
            const title = document.createElement("strong");
            title.className = "check-title";
            title.textContent = check.title;
            const message = document.createElement("span");
            message.className = "check-message";
            message.textContent = check.message;
            text.append(title, message);
            if (check.recommendation) {
                const recommendation = document.createElement("span");
                recommendation.className = "check-recommendation";
                recommendation.textContent = `Рекомендация: ${check.recommendation}`;
                text.append(recommendation);
            }
            item.append(mark, text);
            checks.append(item);
        }
        $("#build-button").disabled = buildInProgress || !diagnostics.buildAllowed;
        if (!diagnostics.buildAllowed) setNotice("Устраните ошибки диагностики, чтобы собрать конфигурацию.", true);
        else setNotice($("#build-result").hidden
            ? "Конфигурация разрешена к сборке."
            : "Сборка прошла серверную проверку.");
        updateSummary();
    }

    function scheduleDiagnosis(delay = 320) {
        clearTimeout(diagnosisTimer);
        diagnosisController?.abort();
        lastDiagnostics = null;
        $("#build-button").disabled = true;
        const requestId = ++diagnosisRequestId;
        diagnosisTimer = window.setTimeout(() => runDiagnosis(requestId), delay);
    }

    async function runDiagnosis(requestId) {
        if (!activeCatalog || requestId !== diagnosisRequestId) return;
        diagnosisController = new AbortController();
        $("#build-button").disabled = true;
        $("#diagnostics-loading").textContent = "Проверяем совместимость на сервере…";
        try {
            const result = await apiPost("/api/pc/diagnose", configFromForm(), diagnosisController.signal);
            if (requestId === diagnosisRequestId) renderDiagnostics(result);
        } catch (error) {
            if (error.name === "AbortError" || requestId !== diagnosisRequestId) return;
            setOverallStatus("status-blocked", "НЕТ СВЯЗИ");
            $("#diagnostics-loading").textContent = error.message;
            $("#build-button").disabled = true;
            setNotice(error.message, true);
        }
    }

    function createSpecList(specs, labels) {
        const list = document.createElement("dl");
        list.className = "preset-specs";
        for (const [key, title] of labels) {
            const row = document.createElement("div");
            const term = document.createElement("dt");
            term.textContent = title;
            const value = document.createElement("dd");
            value.textContent = specs?.[key] || "—";
            row.append(term, value);
            list.append(row);
        }
        return list;
    }

    function renderPresets(presets) {
        const grid = $("#preset-grid");
        grid.replaceChildren();
        const labels = [["CPU", "Процессор"], ["GPU", "Видеокарта"], ["RAM", "Память"], ["Power Supply", "Питание"]];
        const purposes = [
            ["◈", "Игровая система"], ["▤", "Для работы и офиса"], ["◇", "Для создания контента"]
        ];
        presets.forEach((preset, index) => {
            const card = document.createElement("article");
            card.className = "preset-card";
            const head = document.createElement("div");
            head.className = "preset-card-head";
            const symbol = document.createElement("span");
            symbol.className = "preset-symbol";
            symbol.textContent = purposes[index]?.[0] || "▣";
            const title = document.createElement("div");
            const name = document.createElement("h3");
            name.textContent = preset.name || `Сборка ${index + 1}`;
            const purpose = document.createElement("p");
            purpose.textContent = purposes[index]?.[1] || "Готовая система";
            title.append(name, purpose);
            head.append(symbol, title);
            const specs = createSpecList(preset.specs, labels);
            const button = document.createElement("button");
            button.className = "button button-secondary";
            button.type = "button";
            button.textContent = "Настроить сборку";
            button.addEventListener("click", () => activatePreset(preset));
            card.append(head, specs, button);
            grid.append(card);
        });
    }

    async function activatePreset(preset) {
        const specs = preset.specs || {};
        try {
            const [intelCatalog, amdCatalog] = await Promise.all([
                getCatalog("intel_nvidia"), getCatalog("amd")
            ]);
            const ecosystem = intelCatalog.cpus.some(item => item.model === specs.CPU)
                ? "intel_nvidia" : amdCatalog.cpus.some(item => item.model === specs.CPU) ? "amd" : null;
            if (!ecosystem) throw new Error("Не удалось определить платформу готовой сборки.");
            const config = {
                cpu: specs.CPU,
                gpu: specs.GPU,
                motherboard: specs.Motherboard,
                ram: specs.RAM,
                storage: specs.Storage,
                powerSupply: specs["Power Supply"],
                coolingSystem: specs["Cooling System"]
            };
            $(`[name="ecosystem"][value="${ecosystem}"]`).checked = true;
            await loadCatalog(ecosystem, config);
            $("#rgb").checked = specs["RGB Lighting"] === "Enabled";
            addHistory(`Выбрана готовая сборка «${preset.name}».`, configFromForm());
            $("#builder").scrollIntoView({ behavior: "smooth" });
        } catch (error) {
            setNotice(error.message, true);
        }
    }

    function renderBuild(result, selectionIsCurrent) {
        $("#build-result").hidden = false;
        $("#build-result").classList.remove("is-blocked");
        $("#build-result-title").textContent = selectionIsCurrent
            ? "Сборка готова" : "Результат отправленной сборки";
        $(".result-check").textContent = "✓";
        $("#result-status").textContent = `${result.validationStatus} Расчёт: минимум ${result.requiredPowerWatts} Вт, рекомендовано ${result.recommendedPowerWatts} Вт, выбрано ${result.selectedPowerWatts} Вт.`;
        const specs = $("#result-specs");
        specs.replaceChildren();
        for (const [key, value] of Object.entries(result.specs || {})) {
            const row = document.createElement("div");
            const term = document.createElement("dt");
            term.textContent = key;
            const detail = document.createElement("dd");
            detail.textContent = value;
            row.append(term, detail);
            specs.append(row);
        }
        if (selectionIsCurrent && result.diagnostics) renderDiagnostics(result.diagnostics);
    }

    function renderBuildFailure(message) {
        const result = $("#build-result");
        result.hidden = false;
        result.classList.add("is-blocked");
        $("#build-result-title").textContent = "Сборка заблокирована";
        $(".result-check").textContent = "×";
        $("#result-status").textContent = message;
        $("#result-specs").replaceChildren();
    }

    async function submitBuild(event) {
        event.preventDefault();
        const button = $("#build-button");
        if (button.disabled) return;
        buildInProgress = true;
        button.disabled = true;
        $("#build-result").hidden = true;
        setNotice("Сервер проверяет и собирает выбранные компоненты…");
        const config = configFromForm();
        const submittedConfiguration = JSON.stringify(config);
        try {
            const result = await apiPost("/api/pc/custom", config);
            const selectionIsCurrent = JSON.stringify(configFromForm()) === submittedConfiguration;
            renderBuild(result, selectionIsCurrent);
            setNotice(selectionIsCurrent
                ? "Сборка успешно проверена и создана."
                : "Сборка создана для отправленного выбора; текущая конфигурация уже изменилась.");
            addHistory("Сборка успешно создана.", config);
        } catch (error) {
            const selectionIsCurrent = JSON.stringify(configFromForm()) === submittedConfiguration;
            if (selectionIsCurrent) {
                if (error.payload?.diagnostics) renderDiagnostics(error.payload.diagnostics);
                renderBuildFailure(error.message);
                setNotice(error.message, true);
            } else {
                setNotice("Сервер отклонил отправленный вариант; текущий выбор уже изменился.", true);
            }
        } finally {
            buildInProgress = false;
            if (lastDiagnostics) $("#build-button").disabled = !lastDiagnostics.buildAllowed;
        }
    }

    async function generateReport() {
        const button = $("#generate-report");
        const status = $("#report-status");
        const output = $("#report-output");
        const htmlFrame = $("#report-html");
        const jsonOutput = $("#report-json");
        button.disabled = true;
        output.hidden = true;
        status.textContent = "Формируем отчёт по текущему выбору…";
        try {
            const selection = configFromForm();
            const response = await fetch("/api/reports/export", {
                method: "POST",
                headers: { "Content-Type": "application/json", "Accept": $("#report-format").value === "html" ? "text/html" : "application/json" },
                body: JSON.stringify({
                    reportType: $("#report-type").value,
                    format: $("#report-format").value,
                    configuration: selection
                })
            });
            const content = await response.text();
            if (!response.ok) {
                let error;
                try { error = JSON.parse(content); } catch { error = null; }
                throw new Error(error?.message || error?.detail || `Не удалось сформировать отчёт (${response.status}).`);
            }
            reportDownload = {
                content,
                type: $("#report-format").value === "html" ? "text/html;charset=utf-8" : "application/json;charset=utf-8",
                extension: $("#report-format").value
            };
            output.hidden = false;
            if (reportDownload.extension === "html") {
                htmlFrame.srcdoc = content;
                htmlFrame.hidden = false;
                jsonOutput.hidden = true;
            } else {
                jsonOutput.textContent = JSON.stringify(JSON.parse(content), null, 2);
                jsonOutput.hidden = false;
                htmlFrame.hidden = true;
                htmlFrame.removeAttribute("srcdoc");
            }
            status.textContent = "Отчёт сформирован для текущей конфигурации.";
            addHistory("Сформирован отчёт по выбранной конфигурации.", selection);
        } catch (error) {
            status.textContent = error.message;
        } finally {
            button.disabled = false;
        }
    }

    function downloadReport() {
        if (!reportDownload) return;
        const blob = new Blob([reportDownload.content], { type: reportDownload.type });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = `pc-build-report.${reportDownload.extension}`;
        document.body.append(link);
        link.click();
        link.remove();
        window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    }

    async function runStressTest() {
        const button = $("#stress-test-button");
        const status = $("#stress-status");
        const report = $("#stress-report");
        button.disabled = true;
        report.hidden = true;
        status.textContent = "Запускается демонстрационный тест CPU, GPU и температурного контроля…";
        try {
            const response = await fetch("/api/diagnostics/run", {
                method: "POST",
                headers: { "Content-Type": "application/json", "Accept": "text/html" },
                body: JSON.stringify(configFromForm())
            });
            const content = await response.text();
            if (!response.ok) {
                let error;
                try { error = JSON.parse(content); } catch { error = null; }
                throw new Error(error?.message || error?.detail || `Не удалось запустить тест (${response.status}).`);
            }
            report.srcdoc = content;
            report.hidden = false;
            status.textContent = "Стресс-тест завершён. Результаты приведены в отчёте.";
            addHistory("Выполнен стресс-тест системы.", configFromForm());
        } catch (error) {
            status.textContent = error.message;
        } finally {
            button.disabled = false;
        }
    }

    function loadHistory() {
        try {
            const saved = JSON.parse(localStorage.getItem(HISTORY_KEY) || "[]");
            if (!Array.isArray(saved)) return [];
            return saved.filter(item => item && typeof item.message === "string"
                && typeof item.timestamp === "string"
                && (!item.configuration || typeof item.configuration === "object"))
                .slice(0, 8);
        } catch {
            return [];
        }
    }

    function persistHistory() {
        try {
            localStorage.setItem(HISTORY_KEY, JSON.stringify(recentActions.slice(0, 8)));
        } catch {
            $("#stress-status").textContent = "Историю нельзя сохранить в браузере. Проверьте настройки локального хранилища.";
        }
    }

    function addHistory(message, configuration = null) {
        recentActions.unshift({
            id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
            message,
            timestamp: new Date().toISOString(),
            configuration: configuration ? { ...configuration } : null
        });
        recentActions = recentActions.slice(0, 8);
        persistHistory();
        renderHistory();
    }

    function renderHistory() {
        const list = $("#history-list");
        list.replaceChildren();
        if (!recentActions.length) {
            const empty = document.createElement("li");
            empty.className = "empty-history";
            empty.textContent = "История пока пуста.";
            list.append(empty);
            return;
        }
        for (const action of recentActions) {
            const item = document.createElement("li");
            item.className = "history-entry";
            const text = document.createElement("div");
            if (action.configuration) {
                const load = document.createElement("button");
                load.type = "button";
                load.textContent = action.message;
                load.addEventListener("click", () => restoreConfiguration(action.configuration));
                text.append(load);
            } else {
                const label = document.createElement("span");
                label.textContent = action.message;
                text.append(label);
            }
            const date = document.createElement("time");
            date.className = "history-date";
            const dateValue = new Date(action.timestamp);
            date.dateTime = Number.isNaN(dateValue.getTime()) ? "" : dateValue.toISOString();
            date.textContent = Number.isNaN(dateValue.getTime())
                ? "Недавно" : dateValue.toLocaleString("ru-RU", { dateStyle: "short", timeStyle: "short" });
            text.append(date);
            item.append(text);
            list.append(item);
        }
    }

    async function restoreConfiguration(configuration) {
        const ecosystem = configuration.ecosystem;
        if (!["intel_nvidia", "amd"].includes(ecosystem)) {
            setNotice("Сохранённая конфигурация использует неизвестную платформу.", true);
            return;
        }
        $(`[name="ecosystem"][value="${ecosystem}"]`).checked = true;
        try {
            await loadCatalog(ecosystem, configuration);
            $("#rgb").checked = Boolean(configuration.hasRGB);
            scheduleDiagnosis(0);
            $("#builder").scrollIntoView({ behavior: "smooth" });
            setNotice("Сохранённая конфигурация загружена.");
        } catch (error) {
            setNotice(error.message, true);
        }
    }

    async function loadPresets() {
        try {
            renderPresets(await requestJson("/api/pc/presets"));
        } catch (error) {
            const grid = $("#preset-grid");
            grid.replaceChildren();
            const message = document.createElement("p");
            message.className = "loading-card";
            message.textContent = error.message;
            grid.append(message);
        }
    }

    function bindEvents() {
        $("#custom-form").addEventListener("submit", submitBuild);
        $$('[name="ecosystem"]').forEach(input => input.addEventListener("change", async event => {
            try {
                await loadCatalog(event.target.value);
                addHistory(`Выбрана платформа: ${event.target.value === "amd" ? "AMD" : "Intel + NVIDIA"}.`);
            } catch (error) {
                setNotice(error.message, true);
                $("#diagnostics-loading").textContent = error.message;
            }
        }));
        ["cpu", "gpu", "motherboard", "ram", "storage", "psu", "cooling", "rgb"].forEach(id => {
            $("#" + id).addEventListener("change", () => {
                updateSummary();
                scheduleDiagnosis();
                $("#build-result").hidden = true;
            });
        });
        $("#generate-report").addEventListener("click", generateReport);
        $("#download-report").addEventListener("click", downloadReport);
        $("#stress-test-button").addEventListener("click", runStressTest);
        $("#clear-history").addEventListener("click", () => {
            recentActions = [];
            persistHistory();
            renderHistory();
        });
    }

    async function initialize() {
        bindEvents();
        renderHistory();
        await Promise.all([loadPresets(), loadCatalog("intel_nvidia")]);
        addHistory("Конфигуратор готов к работе.");
    }

    initialize().catch(error => {
        setOverallStatus("status-blocked", "ОШИБКА");
        $("#diagnostics-loading").textContent = error.message;
        setNotice(error.message, true);
    });
})();
