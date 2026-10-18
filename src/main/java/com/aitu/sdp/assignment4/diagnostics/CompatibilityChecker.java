package com.aitu.sdp.assignment4.diagnostics;

import com.aitu.sdp.assignment2.CPU;
import com.aitu.sdp.assignment2.GPU;
import com.aitu.sdp.assignment2.Motherboard;
import org.springframework.stereotype.Component;

@Component
public class CompatibilityChecker {
    public DiagnosticCheck platformCheck(boolean compatible) {
        return compatible
                ? pass("platform", "Платформа", "Компоненты соответствуют выбранной платформе.")
                : error("platform", "Платформа",
                        "Один или несколько компонентов не относятся к выбранной платформе.",
                        "Выберите компоненты одного семейства из каталога.");
    }

    public DiagnosticCheck socketCheck(CPU cpu, Motherboard motherboard) {
        if (cpu == null || motherboard == null) {
            return error("socket", "Сокет", "Невозможно проверить сокет: CPU или материнская плата не распознаны.",
                    "Выберите известные модели CPU и материнской платы.");
        }
        return cpu.getSocket().equals(motherboard.getSocket())
                ? pass("socket", "Совместимость сокета",
                        "Сокеты " + cpu.getSocket() + " совместимы.")
                : error("socket", "Несовместимый сокет",
                        cpu.getModel() + " требует " + cpu.getSocket() + ", а "
                                + motherboard.getModel() + " поддерживает " + motherboard.getSocket() + ".",
                        "Выберите материнскую плату с сокетом " + cpu.getSocket() + ".");
    }

    public DiagnosticCheck powerCheck(Integer selectedWatts, Integer requiredWatts) {
        if (requiredWatts == null) {
            return error("psu-capacity", "Мощность блока питания",
                    "Невозможно рассчитать мощность без распознанных CPU и GPU.",
                    "Выберите известные модели процессора и видеокарты.");
        }
        if (selectedWatts == null) {
            return error("psu-capacity", "Мощность блока питания",
                    "Модель блока питания не найдена в каталоге.",
                    "Выберите блок питания из каталога.");
        }
        if (selectedWatts < requiredWatts) {
            return error("psu-capacity", "Недостаточная мощность",
                    "Выбрано " + selectedWatts + " Вт, расчетный минимум — " + requiredWatts + " Вт.",
                    "Выберите блок питания не менее " + requiredWatts + " Вт.");
        }
        return pass("psu-capacity", "Мощность блока питания",
                "Выбрано " + selectedWatts + " Вт при расчетном минимуме " + requiredWatts + " Вт.");
    }

    public DiagnosticCheck headroomCheck(Integer selectedWatts, Integer recommendedWatts) {
        if (recommendedWatts == null || selectedWatts == null) {
            return error("psu-headroom", "Запас мощности",
                    "Запас мощности нельзя проверить для неизвестной конфигурации.",
                    "Сначала выберите известные CPU, GPU и блок питания.");
        }
        if (selectedWatts < recommendedWatts) {
            return new DiagnosticCheck("psu-headroom", "Запас мощности", DiagnosticSeverity.WARNING,
                    "Запас ниже рекомендуемого учебного ориентира " + recommendedWatts + " Вт.",
                    "Рассмотрите блок питания мощностью не менее " + recommendedWatts + " Вт.");
        }
        return pass("psu-headroom", "Запас мощности",
                "Блок питания соответствует рекомендуемому расчетному ориентиру " + recommendedWatts + " Вт.");
    }

    public DiagnosticCheck pass(String id, String title, String message) {
        return new DiagnosticCheck(id, title, DiagnosticSeverity.PASS, message, null);
    }

    public DiagnosticCheck error(String id, String title, String message, String recommendation) {
        return new DiagnosticCheck(id, title, DiagnosticSeverity.ERROR, message, recommendation);
    }
}
