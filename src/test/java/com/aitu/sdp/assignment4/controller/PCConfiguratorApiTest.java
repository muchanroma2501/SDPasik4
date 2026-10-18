package com.aitu.sdp.assignment4.controller;

import org.junit.jupiter.api.Test;
import com.aitu.sdp.assignment2.Main;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Main.class)
@AutoConfigureMockMvc
class PCConfiguratorApiTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void servesPresetsAndBothPlatformCatalogs() throws Exception {
        mvc.perform(get("/api/pc/presets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].specs.CPU").value("Intel Core i9-14900K"));
        mvc.perform(get("/api/pc/catalog").param("ecosystem", "intel_nvidia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpus", hasSize(3)))
                .andExpect(jsonPath("$.coolingSystems", hasSize(4)));
        mvc.perform(get("/api/pc/catalog").param("ecosystem", "amd"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gpus", hasSize(5)));
    }

    @Test
    void diagnosesBuildsAndRejectsInvalidRequests() throws Exception {
        String valid = """
                {"ecosystem":"intel_nvidia","cpu":"Intel Core i7-14700K",
                 "gpu":"NVIDIA GeForce RTX 4070 Ti 12GB","motherboard":"Z790 ATX",
                 "ram":"32GB DDR5","storage":"1TB NVMe SSD","powerSupply":"850W 80+ Gold",
                 "coolingSystem":"280mm AIO Liquid Cooling","hasRGB":true}
                """;
        mvc.perform(post("/api/pc/diagnose").contentType(MediaType.APPLICATION_JSON).content(valid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.requiredPowerWatts").value(640))
                .andExpect(jsonPath("$.buildAllowed").value(true));
        mvc.perform(post("/api/pc/custom").contentType(MediaType.APPLICATION_JSON).content(valid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specs['Cooling System']").value("280mm AIO Liquid Cooling"))
                .andExpect(jsonPath("$.diagnostics.status").value("READY"));
        mvc.perform(post("/api/pc/build").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ecosystem":"intel_nvidia","buildType":"gaming",
                                 "ram":"32GB DDR5","storage":"1TB NVMe SSD","hasRGB":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specs.CPU").value("Intel Core i9-14900K"));
        mvc.perform(post("/api/pc/custom").contentType(MediaType.APPLICATION_JSON)
                        .content(valid.replace("Intel Core i7-14700K", "Unknown CPU")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.diagnostics.status").value("BLOCKED"));
        mvc.perform(post("/api/pc/custom").contentType(MediaType.APPLICATION_JSON)
                        .content(valid.replace("intel_nvidia", "unsupported")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.diagnostics.checks[1].id").value("platform-selection"));
        mvc.perform(post("/api/pc/custom").contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
        mvc.perform(post("/api/pc/custom").contentType(MediaType.TEXT_PLAIN).content("not json"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void exportsTheRequestedConfigurationThroughTheBridge() throws Exception {
        mvc.perform(post("/api/reports/export").contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reportType":"summary","format":"json","configuration":{
                                  "ecosystem":"intel_nvidia","cpu":"Intel Core i5-13400",
                                  "gpu":"NVIDIA GeForce RTX 3050 8GB","motherboard":"B760 Micro-ATX",
                                  "ram":"16GB DDR4","storage":"512GB NVMe SSD",
                                  "powerSupply":"500W 80+ Bronze","hasRGB":false}}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("Intel Core i5-13400")))
                .andExpect(content().string(containsString("512GB NVMe SSD")));
        mvc.perform(get("/api/reports/export").param("reportType", "summary").param("format", "html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Summary PC Specifications")));
    }
}
