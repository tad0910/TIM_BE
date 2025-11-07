package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.dto.AddMemberDTO;
import com.tim.appTim.dto.ClassDTO;
import com.tim.appTim.entity.ClassMember;
import com.tim.appTim.service.KeycloakSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
public class ClassIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private KeycloakSyncService keycloakSyncService;

    private final String BASE_URL = "/classes";


    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createClass_WhenValidData_ShouldReturn201() throws Exception {
        ClassDTO dto = new ClassDTO(null, "Lớp 11A", "Mô tả lớp 11A", new ArrayList<>(), 100, null);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.className").value("Lớp 11A"))
                .andExpect(jsonPath("$.programId").value(100));
    }

    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void createClass_WhenUserIsTeacher_ShouldReturn403() throws Exception {
        ClassDTO dto = new ClassDTO(null, "Lớp 12B", "Không được tạo", new ArrayList<>(), 100, null);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getClassInfo_WhenUserIsMember_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.className").value("BE Class K10"))
                .andExpect(jsonPath("$.members").isArray())
                .andExpect(jsonPath("$.members[0].userId").value(1L));
    }

    @Test
    @WithUserDetails(value = "giaovien2", userDetailsServiceBeanName = "userService")
    void getClassInfo_WhenUserNotMember_ShouldReturn403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void updateClass_WhenUserIsTeacherOfClass_ShouldReturn200() throws Exception {
        ClassDTO update = new ClassDTO(10L, "Lớp 10A Updated", "Mô tả mới", new ArrayList<>(), 100, null);

        mockMvc.perform(put(BASE_URL + "/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.className").value("Lớp 10A Updated"));
    }

    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void addMember_WhenTeacherAddsStudent_ShouldReturn200() throws Exception {
        AddMemberDTO dto = new AddMemberDTO(3L, "sinh_vien");

        mockMvc.perform(post(BASE_URL + "/10/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Thêm thành viên vào lớp học thành công"));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void removeSelfFromClass_ShouldReturn200() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/10/members/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa thành viên khỏi lớp học thành công"));
    }

    @Test
    @WithUserDetails(value = "giaovien2", userDetailsServiceBeanName = "userService")
    void getClassInfo_WhenUserIsNotMemberOrAdmin_ShouldReturn403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "giaovien2", userDetailsServiceBeanName = "userService")
    void updateClass_WhenUserIsNotTeacherOfClass_ShouldReturn403() throws Exception {
        ClassDTO update = new ClassDTO(10L, "Cố gắng sửa", "Thử", new ArrayList<>(), 100, null);

        mockMvc.perform(put(BASE_URL + "/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void deleteClass_WhenClassDoesNotExist_ShouldReturn404() throws Exception {
        Long nonExistentClassId = 999L;

        mockMvc.perform(delete(BASE_URL + "/" + nonExistentClassId))
                .andExpect(status().isNotFound());
    }


}
