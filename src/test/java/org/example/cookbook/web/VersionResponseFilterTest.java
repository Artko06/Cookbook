package org.example.cookbook.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class VersionResponseFilterTest {

    @Test
    @DisplayName("Фильтр добавляет версию и имя пода в ответ")
    void addsVersionHeaders() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .addFilters(new VersionResponseFilter("v1.0.0", "cookbook-abc"))
                .build();

        mockMvc.perform(get("/test"))
                .andExpect(status().isOk())
                .andExpect(header().string(VersionResponseFilter.VERSION_HEADER, "v1.0.0"))
                .andExpect(header().string(VersionResponseFilter.INSTANCE_HEADER, "cookbook-abc"));
    }

    @RestController
    static class TestController {

        @GetMapping("/test")
        String test() {
            return "ok";
        }
    }
}
