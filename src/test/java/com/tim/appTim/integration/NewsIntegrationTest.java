package com.tim.appTim.integration;

import com.tim.appTim.dto.BlogDTO;
import com.tim.appTim.service.NewsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
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

    @MockBean
    private NewsService newsService;

    @Test
    @WithMockUser(username = "admin_user")
    void testGetLatestBlogs_ShouldReturnOk() throws Exception {
        List<BlogDTO> mockBlogs = Collections.emptyList();
        when(newsService.getLatestBlogs()).thenReturn(mockBlogs);
        
        mockMvc.perform(get("/news/latest"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin_user")
    void testGetFeaturedBlogs_ShouldReturnOk() throws Exception {
        List<BlogDTO> mockBlogs = Collections.emptyList();
        when(newsService.getFeaturedBlogs()).thenReturn(mockBlogs);
        
        mockMvc.perform(get("/news/featured"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin_user")
    void testGetTechNews_ShouldReturnOk() throws Exception {
        List<BlogDTO> mockBlogs = Collections.emptyList();
        when(newsService.getTechNews()).thenReturn(mockBlogs);
        
        mockMvc.perform(get("/news/tech"))
                .andExpect(status().isOk());
    }
}
