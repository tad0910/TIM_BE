package com.tim.appTim.integration;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
@Transactional
public class NewsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin_user")
    void testGetLatestBlogs_ShouldReturnOk() throws Exception {
        // NewsService likely fetches from external source or DB.
        // Assuming it works or returns empty list which is still 200 OK.
        mockMvc.perform(get("/news/latest"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin_user")
    void testGetFeaturedBlogs_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/news/featured"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin_user")
    void testGetTechNews_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/news/tech"))
                .andExpect(status().isOk());
    }
}

