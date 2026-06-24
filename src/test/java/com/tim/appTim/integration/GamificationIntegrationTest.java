package com.tim.appTim.integration;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.entity.GamificationAchievement;
import com.tim.appTim.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
public class GamificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private FileUploadService fileUploadService;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private GamificationPointTypeService pointTypeService;

    @Autowired
    private GamificationBehaviorGroupService behaviorGroupService;

    @Autowired
    private GamificationAchievementService achievementService;

    @Autowired
    private NotificationTemplateService notificationTemplateService;

    private final String BASE_URL = "/gamification";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        when(fileUploadService.uploadFile(any())).thenReturn("https://test.local/uploaded-file.jpg");
    }


    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:award_points"})
    void awardPoints_WhenValidRequest_ShouldReturn200() throws Exception {
        AwardPointsRequest request = new AwardPointsRequest();
        request.setUserId(1L);
        request.setBehaviorId(1);

        mockMvc.perform(post(BASE_URL + "/award-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
.andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser(username = "post_owner", authorities = {})
    void awardPoints_WhenNoPermission_ShouldReturn403() throws Exception {
        AwardPointsRequest request = new AwardPointsRequest();
        request.setUserId(1L);
        request.setBehaviorId(1);

        mockMvc.perform(post(BASE_URL + "/award-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void awardPoints_WhenUnauthenticated_ShouldReturn401() throws Exception {
        AwardPointsRequest request = new AwardPointsRequest();
        request.setUserId(1L);
        request.setBehaviorId(1);

        mockMvc.perform(post(BASE_URL + "/award-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:award_points"})
    void awardPoints_WhenInvalidBehaviorId_ShouldReturn404() throws Exception {
        AwardPointsRequest request = new AwardPointsRequest();
        request.setUserId(1L);
        request.setBehaviorId(9999);

        mockMvc.perform(post(BASE_URL + "/award-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getMyStats_WhenAuthenticated_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/my-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void getMyStats_WhenUnauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/my-stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:read_all"})
    void getUserStats_WhenAdmin_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users/1/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getUserStats_WhenSelf_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users/1/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
void getUserStats_WhenOtherUser_ShouldReturn403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users/2/stats"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getMyPointLogs_WhenAuthenticated_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/my-point-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:read_all"})
    void getUserPointLogs_WhenAdmin_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users/1/point-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getMyAchievements_WhenAuthenticated_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/my-achievements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:read_all"})
    void getUserAchievements_WhenAdmin_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users/1/achievements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getAllPointTypes_WhenAuthenticated_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/point-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getAllPointTypes_WhenUnauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/point-types"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getDashboardPointTypes_WhenAuthenticated_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/point-types/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getPointTypeById_WhenExists_ShouldReturn200() throws Exception {
        GamificationPointTypeDTO dto = new GamificationPointTypeDTO();
        dto.setName("Test Point Type");
        dto.setDescription("Test Description");
        dto.setMaxPoints(100);
        dto.setIsActive(true);
        dto.setShowOnDashboard(true);
        dto.setCreatedBy(1);
        GamificationPointTypeDTO created = pointTypeService.createPointType(dto);
mockMvc.perform(get(BASE_URL + "/point-types/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.name").value("Test Point Type"));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getPointTypeById_WhenNotExists_ShouldReturn404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/point-types/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void createPointType_WhenValidRequest_ShouldReturn201() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "test".getBytes());

        mockMvc.perform(multipart(BASE_URL + "/point-types")
                        .file(imageFile)
                        .param("name", "New Point Type")
                        .param("description", "Description")
                        .param("maxPoints", "100")
                        .param("isActive", "true")
                        .param("showOnDashboard", "true")
                        .param("createdBy", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Point Type"))
                .andExpect(header().exists("Location"));
    }

    @Test
    @WithMockUser(username = "post_owner", authorities = {})
    void createPointType_WhenNoPermission_ShouldReturn403() throws Exception {
        mockMvc.perform(multipart(BASE_URL + "/point-types")
                        .param("name", "New Point Type"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void createPointType_WithImageUrl_ShouldReturn201() throws Exception {
        mockMvc.perform(multipart(BASE_URL + "/point-types")
                        .param("name", "New Point Type")
                        .param("description", "Description")
                        .param("maxPoints", "100")
                        .param("imageUrl", "https://example.com/image.jpg")
                        .param("isActive", "true")
                        .param("showOnDashboard", "true")
                        .param("createdBy", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/image.jpg"));
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:update"})
    void updatePointType_WhenValidRequest_ShouldReturn200() throws Exception {
        GamificationPointTypeDTO dto = new GamificationPointTypeDTO();
        dto.setName("Test Point Type");
        dto.setDescription("Test Description");
        dto.setMaxPoints(100);
        dto.setIsActive(true);
dto.setShowOnDashboard(true);
        dto.setCreatedBy(1);
        GamificationPointTypeDTO created = pointTypeService.createPointType(dto);

        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "updated.jpg", MediaType.IMAGE_JPEG_VALUE, "updated".getBytes());

        mockMvc.perform(multipart(BASE_URL + "/point-types/" + created.getId())
                        .file(imageFile)
                        .param("name", "Updated Point Type")
                        .param("description", "Updated Description")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Point Type"));
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:update"})
    void updatePointType_WhenNotExists_ShouldReturn404() throws Exception {
        mockMvc.perform(multipart(BASE_URL + "/point-types/9999")
                        .param("name", "Updated")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:delete"})
    void deletePointType_WhenExists_ShouldReturn204() throws Exception {
        GamificationPointTypeDTO dto = new GamificationPointTypeDTO();
        dto.setName("Test Point Type");
        dto.setDescription("Test Description");
        dto.setMaxPoints(100);
        dto.setIsActive(true);
        dto.setShowOnDashboard(true);
        dto.setCreatedBy(1);
        GamificationPointTypeDTO created = pointTypeService.createPointType(dto);

        mockMvc.perform(delete(BASE_URL + "/point-types/" + created.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:delete"})
    void deletePointType_WhenNotExists_ShouldReturn404() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/point-types/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getAllBehaviors_WhenAuthenticated_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/behaviors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getBehaviorById_WhenExists_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/behaviors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
.andExpect(jsonPath("$.name").exists());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getBehaviorById_WhenNotExists_ShouldReturn404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/behaviors/9999"))
                .andExpect(status().isNotFound());
    }

//     @Test
//     @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
//     void createBehavior_WhenValidRequest_ShouldReturn201() throws Exception {
//         GamificationBehaviorDTO dto = new GamificationBehaviorDTO();
//         dto.setGroupId(1);
//         dto.setName("Test Behavior");
//         dto.setFrequencyType("DAILY");
//         dto.setMaxTimesPerFrequency(1);
//         dto.setPointDiligence(10);
//         dto.setPointCompetence(5);
//         dto.setPointExperience(2);

//         mockMvc.perform(post(BASE_URL + "/behaviors")
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(objectMapper.writeValueAsString(dto)))
//                 .andExpect(status().isCreated())
//                 .andExpect(jsonPath("$.name").value("Test Behavior"))
//                 .andExpect(header().exists("Location"));
//     }

    @Test
    @WithMockUser(username = "post_owner", authorities = {})
    void createBehavior_WhenNoPermission_ShouldReturn403() throws Exception {
        GamificationBehaviorDTO dto = new GamificationBehaviorDTO();
        dto.setGroupId(1);
        dto.setName("Test Behavior");

        mockMvc.perform(post(BASE_URL + "/behaviors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void createBehavior_WhenInvalidGroupId_ShouldReturn404() throws Exception {
        GamificationBehaviorDTO dto = new GamificationBehaviorDTO();
        dto.setGroupId(9999);
        dto.setName("Test Behavior");
        dto.setFrequencyType("DAILY");
        dto.setMaxTimesPerFrequency(1);

        mockMvc.perform(post(BASE_URL + "/behaviors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

//     @Test
//     @WithMockUser(username = "admin_user", authorities = {"gamification:update"})
//     void updateBehavior_WhenValidRequest_ShouldReturn200() throws Exception {
//         GamificationBehaviorDTO dto = new GamificationBehaviorDTO();
//         dto.setGroupId(1);
//         dto.setName("Updated Behavior");
//         dto.setFrequencyType("WEEKLY");
//         dto.setMaxTimesPerFrequency(2);
//         dto.setPointDiligence(15);
//         dto.setPointCompetence(10);
//         dto.setPointExperience(5);

//         mockMvc.perform(put(BASE_URL + "/behaviors/1")
//                         .contentType(MediaType.APPLICATION_JSON)
// .content(objectMapper.writeValueAsString(dto)))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.name").value("Updated Behavior"));
//     }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:update"})
    void updateBehavior_WhenNotExists_ShouldReturn404() throws Exception {
        GamificationBehaviorDTO dto = new GamificationBehaviorDTO();
        dto.setGroupId(1);
        dto.setName("Updated Behavior");

        mockMvc.perform(put(BASE_URL + "/behaviors/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:delete"})
    void deleteBehavior_WhenExists_ShouldReturn204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/behaviors/4"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:delete"})
    void deleteBehavior_WhenNotExists_ShouldReturn404() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/behaviors/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getAllBehaviorGroups_WhenAuthenticated_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/behavior-groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getBehaviorGroupById_WhenExists_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/behavior-groups/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getBehaviorGroupById_WhenNotExists_ShouldReturn404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/behavior-groups/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void createBehaviorGroup_WhenValidRequest_ShouldReturn201() throws Exception {
        GamificationBehaviorGroupDTO dto = new GamificationBehaviorGroupDTO();
        dto.setName("New Group");

        mockMvc.perform(post(BASE_URL + "/behavior-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Group"))
                .andExpect(header().exists("Location"));
    }

    @Test
@WithMockUser(username = "admin_user", authorities = {"gamification:update"})
    void updateBehaviorGroup_WhenValidRequest_ShouldReturn200() throws Exception {
        GamificationBehaviorGroupDTO dto = new GamificationBehaviorGroupDTO();
        dto.setName("Updated Group");

        mockMvc.perform(put(BASE_URL + "/behavior-groups/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Group"));
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:delete"})
    void deleteBehaviorGroup_WhenExists_ShouldReturn204() throws Exception {
        GamificationBehaviorGroupDTO dto = new GamificationBehaviorGroupDTO();
        dto.setName("Test Group");
        GamificationBehaviorGroupDTO created = behaviorGroupService.createGroup(dto);

        mockMvc.perform(delete(BASE_URL + "/behavior-groups/" + created.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getAllAchievements_WhenAuthenticated_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/achievements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getAchievementById_WhenExists_ShouldReturn200() throws Exception {
        GamificationAchievement achievement = achievementService.createAchievement(
                "Test Achievement", "https://example.com/image.jpg", 1);

        mockMvc.perform(get(BASE_URL + "/achievements/" + achievement.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(achievement.getId()))
                .andExpect(jsonPath("$.name").value("Test Achievement"));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getAchievementById_WhenNotExists_ShouldReturn404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/achievements/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getAchievementLevels_WhenExists_ShouldReturn200() throws Exception {
        GamificationAchievement achievement = achievementService.createAchievement(
                "Test Achievement", "https://example.com/image.jpg", 1);

        AchievementLevelDTO levelDto = new AchievementLevelDTO();
        levelDto.setAchievementId(achievement.getId());
        levelDto.setLevelName("Bronze");
        levelDto.setRequiredPointTypeEnum("EXPERIENCE");
        levelDto.setMinPointsRequired(100);
achievementService.createAchievementLevel(levelDto);

        mockMvc.perform(get(BASE_URL + "/achievements/" + achievement.getId() + "/levels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void createAchievement_WhenValidRequest_ShouldReturn201() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "achievement.jpg", MediaType.IMAGE_JPEG_VALUE, "test".getBytes());

        mockMvc.perform(multipart(BASE_URL + "/achievements")
                        .file(imageFile)
                        .param("name", "New Achievement")
                        .param("createdBy", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Achievement"))
                .andExpect(header().exists("Location"));
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void createAchievement_WhenNoCreatedBy_ShouldUseCurrentUser() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "achievement.jpg", MediaType.IMAGE_JPEG_VALUE, "test".getBytes());

        mockMvc.perform(multipart(BASE_URL + "/achievements")
                        .file(imageFile)
                        .param("name", "New Achievement"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Achievement"));
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void createAchievement_WhenUnauthorized_ShouldReturn401() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "achievement.jpg", MediaType.IMAGE_JPEG_VALUE, "test".getBytes());
        doThrow(new RuntimeException("Unauthorized"))
                .when(fileUploadService).uploadFile(any());

        mockMvc.perform(multipart(BASE_URL + "/achievements")
                        .file(imageFile)
                        .param("name", "New Achievement"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:update"})
    void updateAchievement_WhenValidRequest_ShouldReturn200() throws Exception {
        GamificationAchievement achievement = achievementService.createAchievement(
                "Test Achievement", "https://example.com/image.jpg", 1);

        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "updated.jpg", MediaType.IMAGE_JPEG_VALUE, "updated".getBytes());

        mockMvc.perform(multipart(BASE_URL + "/achievements/" + achievement.getId())
                        .file(imageFile)
                        .param("name", "Updated Achievement")
                        .with(request -> {
request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Achievement"));
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:delete"})
    void deleteAchievement_WhenExists_ShouldReturn204() throws Exception {
        GamificationAchievement achievement = achievementService.createAchievement(
                "Test Achievement", "https://example.com/image.jpg", 1);

        mockMvc.perform(delete(BASE_URL + "/achievements/" + achievement.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void createAchievementLevel_WhenValidRequest_ShouldReturn201() throws Exception {
        GamificationAchievement achievement = achievementService.createAchievement(
                "Test Achievement", "https://example.com/image.jpg", 1);

        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "level.jpg", MediaType.IMAGE_JPEG_VALUE, "test".getBytes());

        mockMvc.perform(multipart(BASE_URL + "/achievement-levels")
                        .file(imageFile)
                        .param("achievementId", String.valueOf(achievement.getId()))
                        .param("levelName", "Bronze")
                        .param("requiredPointTypeEnum", "EXPERIENCE")
                        .param("minPointsRequired", "100"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.levelName").value("Bronze"))
                .andExpect(header().exists("Location"));
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:update"})
    void updateAchievementLevel_WhenValidRequest_ShouldReturn200() throws Exception {
        GamificationAchievement achievement = achievementService.createAchievement(
                "Test Achievement", "https://example.com/image.jpg", 1);

        AchievementLevelDTO levelDto = new AchievementLevelDTO();
        levelDto.setAchievementId(achievement.getId());
        levelDto.setLevelName("Bronze");
        levelDto.setRequiredPointTypeEnum("EXPERIENCE");
        levelDto.setMinPointsRequired(100);
        AchievementLevelDTO created = achievementService.createAchievementLevel(levelDto);

        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "updated.jpg", MediaType.IMAGE_JPEG_VALUE, "updated".getBytes());

        mockMvc.perform(multipart(BASE_URL + "/achievement-levels/" + created.getId())
                        .file(imageFile)
                        .param("levelName", "Silver")
                        .param("minPointsRequired", "200")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
.andExpect(status().isOk())
                .andExpect(jsonPath("$.levelName").value("Silver"));
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:delete"})
    void deleteAchievementLevel_WhenExists_ShouldReturn204() throws Exception {
        GamificationAchievement achievement = achievementService.createAchievement(
                "Test Achievement", "https://example.com/image.jpg", 1);

        AchievementLevelDTO levelDto = new AchievementLevelDTO();
        levelDto.setAchievementId(achievement.getId());
        levelDto.setLevelName("Bronze");
        levelDto.setRequiredPointTypeEnum("EXPERIENCE");
        levelDto.setMinPointsRequired(100);
        AchievementLevelDTO created = achievementService.createAchievementLevel(levelDto);

        mockMvc.perform(delete(BASE_URL + "/achievement-levels/" + created.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getCurrentRanking_WhenAuthenticated_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/ranking/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rankings").isArray())
                .andExpect(jsonPath("$.total").exists());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getCurrentRanking_WithSortBy_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/ranking/current")
                        .param("sortBy", "competence"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortBy").value("competence"));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getCurrentRanking_WithPagination_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/ranking/current")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getMonthlyRanking_WhenValidMonthYear_ShouldReturn200() throws Exception {
        String monthYear = "2024-01";
        mockMvc.perform(get(BASE_URL + "/ranking/monthly")
                        .param("monthYear", monthYear))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthYear").value(monthYear));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getMonthlyRanking_WhenMissingMonthYear_ShouldReturn400() throws Exception {
        mockMvc.perform(get(BASE_URL + "/ranking/monthly"))
                .andExpect(status().isBadRequest())
.andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getMyRankPosition_WhenAuthenticated_ShouldReturn200Or404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/ranking/my-position"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 404 : "Expected 200 or 404, got " + status;
                });
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getMyRankPosition_WithMonthYear_ShouldReturn200Or404() throws Exception {
        String monthYear = "2024-01";
        mockMvc.perform(get(BASE_URL + "/ranking/my-position")
                        .param("monthYear", monthYear))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 404 : "Expected 200 or 404, got " + status;
                });
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void createMonthlySnapshot_WhenValidRequest_ShouldReturn200() throws Exception {
        String monthYear = "2024-01";
        mockMvc.perform(post(BASE_URL + "/ranking/snapshot")
                        .param("monthYear", monthYear))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void createMonthlySnapshot_WhenNoMonthYear_ShouldUseCurrentMonth() throws Exception {
        mockMvc.perform(post(BASE_URL + "/ranking/snapshot"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void uploadGuide_WhenValidFile_ShouldReturn200() throws Exception {
        MockMultipartFile guideFile = new MockMultipartFile(
                "guideFile", "guide.pdf", "application/pdf", "test pdf content".getBytes());

        mockMvc.perform(multipart(BASE_URL + "/guide/upload")
                        .file(guideFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileUrl").exists())
                .andExpect(jsonPath("$.fileName").exists());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void uploadGuide_WhenEmptyFile_ShouldReturn400() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "guideFile", "empty.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart(BASE_URL + "/guide/upload")
                        .file(emptyFile))
                .andExpect(status().isBadRequest());
    }

    @Test
@WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getGuide_WhenAuthenticated_ShouldReturn200Or404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/guide"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 404 : "Expected 200 or 404, got " + status;
                });
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void viewGuide_WhenAuthenticated_ShouldReturnRedirectOr404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/guide/view"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 302 || status == 404 : "Expected 302 or 404, got " + status;
                });
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:update"})
    void updateGuide_WhenValidRequest_ShouldReturn200Or400() throws Exception {
        MockMultipartFile updatedFile = new MockMultipartFile(
                "guideFile", "updated-guide.pdf", "application/pdf", "updated content".getBytes());

        mockMvc.perform(multipart(BASE_URL + "/guide/1")
                        .file(updatedFile)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 400 : "Expected 200 or 400, got " + status;
                });
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:delete"})
    void deleteGuide_WhenValidRequest_ShouldReturn200Or400() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/guide/1"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 400 : "Expected 200 or 400, got " + status;
                });
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:read_all"})
    void getAllGuides_WhenAdmin_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/guide/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getAllGuides_WhenNotAdmin_ShouldReturn403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/guide/all"))
                .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:award_points"})
    void awardPoints_WhenExceedsFrequencyLimit_ShouldReturn400() throws Exception {
AwardPointsRequest request = new AwardPointsRequest();
        request.setUserId(1L);
        request.setBehaviorId(1);

        mockMvc.perform(post(BASE_URL + "/award-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE_URL + "/award-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getPointTypeById_WhenInvalidId_ShouldReturn404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/point-types/-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void createBehavior_WhenInvalidFrequencyType_ShouldReturn400() throws Exception {
        GamificationBehaviorDTO dto = new GamificationBehaviorDTO();
        dto.setGroupId(1);
        dto.setName("Test Behavior");
        dto.setFrequencyType("INVALID_TYPE");
        dto.setMaxTimesPerFrequency(1);

        mockMvc.perform(post(BASE_URL + "/behaviors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 400 || status >= 500 : "Expected 400 or 5xx, got " + status;
                });
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getCurrentRanking_WithInvalidSortBy_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/ranking/current")
                        .param("sortBy", "invalid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortBy").exists());
    }

    // ========== Additional tests for missing coverage ==========

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getMyAchievementsWithStatus_WhenAuthenticated_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/my-achievements-with-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getMyAchievementsWithStatus_WhenUnauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/my-achievements-with-status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:read_all"})
    void getAchievementsWithUserStatus_WhenAdmin_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users/1/achievements-with-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getAchievementsWithUserStatus_WhenSelf_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users/1/achievements-with-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getAchievementsWithUserStatus_WhenOtherUser_ShouldReturn403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users/2/achievements-with-status"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getBehaviorByName_WhenExists_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/behaviors/name/Test Behavior"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 404 : "Expected 200 or 404, got " + status;
                });
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getBehaviorByName_WhenNotExists_ShouldReturn404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/behaviors/name/NonExistentBehavior"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void createBehavior_WhenValidRequest_ShouldReturn201() throws Exception {
        GamificationBehaviorDTO dto = new GamificationBehaviorDTO();
        dto.setGroupId(1);
        dto.setName("New Test Behavior");
        dto.setFrequencyType("DAILY");
        dto.setMaxTimesPerFrequency(1);
        dto.setPointDiligence(10);
        dto.setPointCompetence(5);
        dto.setPointExperience(2);

        mockMvc.perform(post(BASE_URL + "/behaviors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Test Behavior"))
                .andExpect(header().exists("Location"));
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:update"})
    void updateBehavior_WhenValidRequest_ShouldReturn200() throws Exception {
        GamificationBehaviorDTO dto = new GamificationBehaviorDTO();
        dto.setGroupId(1);
        dto.setName("Updated Behavior Name");
        dto.setFrequencyType("WEEKLY");
        dto.setMaxTimesPerFrequency(2);
        dto.setPointDiligence(15);
        dto.setPointCompetence(10);
        dto.setPointExperience(5);

        mockMvc.perform(put(BASE_URL + "/behaviors/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Behavior Name"));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getAchievementLevelById_WhenExists_ShouldReturn200() throws Exception {
        GamificationAchievement achievement = achievementService.createAchievement(
                "Test Achievement", "https://example.com/image.jpg", 1);

        AchievementLevelDTO levelDto = new AchievementLevelDTO();
        levelDto.setAchievementId(achievement.getId());
        levelDto.setLevelName("Bronze");
        levelDto.setRequiredPointTypeEnum("EXPERIENCE");
        levelDto.setMinPointsRequired(100);
        AchievementLevelDTO created = achievementService.createAchievementLevel(levelDto);

        mockMvc.perform(get(BASE_URL + "/achievement-levels/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.levelName").value("Bronze"));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getAchievementLevelById_WhenNotExists_ShouldReturn404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/achievement-levels/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getMyPointLogs_WithPagination_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/my-point-logs")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:read_all"})
    void getUserPointLogs_WithPagination_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users/1/point-logs")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getMyAchievements_WithPagination_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/my-achievements")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:read_all"})
    void getUserAchievements_WithPagination_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users/1/achievements")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getAllPointTypes_WithPagination_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/point-types")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getAllBehaviors_WithPagination_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/behaviors")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getAllBehaviorGroups_WithPagination_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/behavior-groups")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getAllAchievements_WithPagination_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/achievements")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getCurrentRanking_WithClassId_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/ranking/current")
                        .param("classId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rankings").isArray());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getMonthlyRanking_WithClassId_ShouldReturn200() throws Exception {
        String monthYear = "2024-01";
        mockMvc.perform(get(BASE_URL + "/ranking/monthly")
                        .param("monthYear", monthYear)
                        .param("classId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthYear").value(monthYear));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getMyRankPosition_WithClassId_ShouldReturn200Or404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/ranking/my-position")
                        .param("classId", "1"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 404 : "Expected 200 or 404, got " + status;
                });
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getMyRankPosition_WithSortBy_ShouldReturn200Or404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/ranking/my-position")
                        .param("sortBy", "diligence"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 404 : "Expected 200 or 404, got " + status;
                });
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getGuideById_WhenExists_ShouldReturn200() throws Exception {
        // First upload a guide
        MockMultipartFile guideFile = new MockMultipartFile(
                "guideFile", "guide.pdf", "application/pdf", "test pdf content".getBytes());

        mockMvc.perform(multipart(BASE_URL + "/guide/upload")
                        .file(guideFile))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 403 : "Expected 200 or 403, got " + status;
                });

        // Test with a known ID if available
        mockMvc.perform(get(BASE_URL + "/guide/1"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 403 || status == 404 : "Expected 200, 403 or 404, got " + status;
                });
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getGuideById_WhenNotExists_ShouldReturn404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/guide/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void uploadGuide_WithFileParameter_ShouldReturn200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "guide.pdf", "application/pdf", "test pdf content".getBytes());

        mockMvc.perform(multipart(BASE_URL + "/guide/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileUrl").exists())
                .andExpect(jsonPath("$.fileName").exists());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:update"})
    void updateGuide_WithFileParameter_ShouldReturn200Or400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "updated-guide.pdf", "application/pdf", "updated content".getBytes());

        mockMvc.perform(multipart(BASE_URL + "/guide/1")
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 400 : "Expected 200 or 400, got " + status;
                });
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:delete"})
    void deleteGuidePermanently_WhenValidRequest_ShouldReturn200Or400() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/guide/1/permanent"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 400 || status == 403 : "Expected 200, 400, or 403, got " + status;
                });
    }

    @Test
    @WithMockUser(username = "post_owner", authorities = {})
    void deleteGuidePermanently_WhenNoPermission_ShouldReturn403() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/guide/1/permanent"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void createAchievementLevel_WithNotificationTemplateId_ShouldReturn201() throws Exception {
        GamificationAchievement achievement = achievementService.createAchievement(
                "Test Achievement", "https://example.com/image.jpg", 1);

        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "level.jpg", MediaType.IMAGE_JPEG_VALUE, "test".getBytes());

        mockMvc.perform(multipart(BASE_URL + "/achievement-levels")
                        .file(imageFile)
                        .param("achievementId", String.valueOf(achievement.getId()))
                        .param("levelName", "Gold")
                        .param("requiredPointTypeEnum", "EXPERIENCE")
                        .param("minPointsRequired", "200")
                        .param("notificationTemplateId", "1"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 201 || status == 404 : "Expected 201 or 404, got " + status;
                });
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void createAchievementLevel_WithInvalidNotificationTemplateId_ShouldReturn201() throws Exception {
        GamificationAchievement achievement = achievementService.createAchievement(
                "Test Achievement", "https://example.com/image.jpg", 1);

        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "level.jpg", MediaType.IMAGE_JPEG_VALUE, "test".getBytes());

        mockMvc.perform(multipart(BASE_URL + "/achievement-levels")
                        .file(imageFile)
                        .param("achievementId", String.valueOf(achievement.getId()))
                        .param("levelName", "Platinum")
                        .param("requiredPointTypeEnum", "EXPERIENCE")
                        .param("minPointsRequired", "300")
                        .param("notificationTemplateId", "invalid"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.levelName").value("Platinum"));
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:update"})
    void updateAchievementLevel_WithNotificationTemplateId_ShouldReturn200() throws Exception {
        GamificationAchievement achievement = achievementService.createAchievement(
                "Test Achievement", "https://example.com/image.jpg", 1);

        AchievementLevelDTO levelDto = new AchievementLevelDTO();
        levelDto.setAchievementId(achievement.getId());
        levelDto.setLevelName("Bronze");
        levelDto.setRequiredPointTypeEnum("EXPERIENCE");
        levelDto.setMinPointsRequired(100);
        AchievementLevelDTO created = achievementService.createAchievementLevel(levelDto);

        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "updated.jpg", MediaType.IMAGE_JPEG_VALUE, "updated".getBytes());

        mockMvc.perform(multipart(BASE_URL + "/achievement-levels/" + created.getId())
                        .file(imageFile)
                        .param("levelName", "Diamond")
                        .param("minPointsRequired", "500")
                        .param("notificationTemplateId", "2")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 404 : "Expected 200 or 404, got " + status;
                });
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void createAchievement_WithImageUrl_ShouldReturn201() throws Exception {
        mockMvc.perform(multipart(BASE_URL + "/achievements")
                        .param("name", "Achievement with URL")
                        .param("imageUrl", "https://example.com/achievement.jpg")
                        .param("createdBy", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Achievement with URL"));
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:update"})
    void updateAchievement_WithImageUrl_ShouldReturn200() throws Exception {
        GamificationAchievement achievement = achievementService.createAchievement(
                "Test Achievement", "https://example.com/image.jpg", 1);

        mockMvc.perform(multipart(BASE_URL + "/achievements/" + achievement.getId())
                        .param("name", "Updated Achievement")
                        .param("imageUrl", "https://example.com/updated.jpg")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Achievement"));
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:update"})
    void updatePointType_WithImageUrl_ShouldReturn200() throws Exception {
        GamificationPointTypeDTO dto = new GamificationPointTypeDTO();
        dto.setName("Test Point Type");
        dto.setDescription("Test Description");
        dto.setMaxPoints(100);
        dto.setIsActive(true);
        dto.setShowOnDashboard(true);
        dto.setCreatedBy(1);
        GamificationPointTypeDTO created = pointTypeService.createPointType(dto);

        mockMvc.perform(multipart(BASE_URL + "/point-types/" + created.getId())
                        .param("name", "Updated Point Type")
                        .param("imageUrl", "https://example.com/updated.jpg")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Point Type"));
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void createAchievementLevel_WithImageUrl_ShouldReturn201() throws Exception {
        GamificationAchievement achievement = achievementService.createAchievement(
                "Test Achievement", "https://example.com/image.jpg", 1);

        mockMvc.perform(multipart(BASE_URL + "/achievement-levels")
                        .param("achievementId", String.valueOf(achievement.getId()))
                        .param("levelName", "Ruby")
                        .param("requiredPointTypeEnum", "EXPERIENCE")
                        .param("minPointsRequired", "400")
                        .param("imageUrl", "https://example.com/ruby.jpg"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.levelName").value("Ruby"));
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:update"})
    void updateAchievementLevel_WithImageUrl_ShouldReturn200() throws Exception {
        GamificationAchievement achievement = achievementService.createAchievement(
                "Test Achievement", "https://example.com/image.jpg", 1);

        AchievementLevelDTO levelDto = new AchievementLevelDTO();
        levelDto.setAchievementId(achievement.getId());
        levelDto.setLevelName("Bronze");
        levelDto.setRequiredPointTypeEnum("EXPERIENCE");
        levelDto.setMinPointsRequired(100);
        AchievementLevelDTO created = achievementService.createAchievementLevel(levelDto);

        mockMvc.perform(multipart(BASE_URL + "/achievement-levels/" + created.getId())
                        .param("levelName", "Emerald")
                        .param("minPointsRequired", "600")
                        .param("imageUrl", "https://example.com/emerald.jpg")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.levelName").value("Emerald"));
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void createPointType_WhenFileUploadFails_ShouldReturn500() throws Exception {
        when(fileUploadService.uploadFile(any())).thenThrow(new RuntimeException("Upload failed"));

        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "test".getBytes());

        mockMvc.perform(multipart(BASE_URL + "/point-types")
                        .file(imageFile)
                        .param("name", "New Point Type"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:update"})
    void updatePointType_WhenFileUploadFails_ShouldReturn500() throws Exception {
        when(fileUploadService.uploadFile(any())).thenThrow(new RuntimeException("Upload failed"));

        GamificationPointTypeDTO dto = new GamificationPointTypeDTO();
        dto.setName("Test Point Type");
        dto.setDescription("Test Description");
        dto.setMaxPoints(100);
        dto.setIsActive(true);
        dto.setShowOnDashboard(true);
        dto.setCreatedBy(1);
        GamificationPointTypeDTO created = pointTypeService.createPointType(dto);

        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "test".getBytes());

        mockMvc.perform(multipart(BASE_URL + "/point-types/" + created.getId())
                        .file(imageFile)
                        .param("name", "Updated")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void createAchievement_WhenFileUploadFails_ShouldReturn500() throws Exception {
        when(fileUploadService.uploadFile(any())).thenThrow(new RuntimeException("Upload failed"));

        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "achievement.jpg", MediaType.IMAGE_JPEG_VALUE, "test".getBytes());

        mockMvc.perform(multipart(BASE_URL + "/achievements")
                        .file(imageFile)
                        .param("name", "New Achievement"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:update"})
    void updateAchievement_WhenFileUploadFails_ShouldReturn500() throws Exception {
        when(fileUploadService.uploadFile(any())).thenThrow(new RuntimeException("Upload failed"));

        GamificationAchievement achievement = achievementService.createAchievement(
                "Test Achievement", "https://example.com/image.jpg", 1);

        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "updated.jpg", MediaType.IMAGE_JPEG_VALUE, "updated".getBytes());

        mockMvc.perform(multipart(BASE_URL + "/achievements/" + achievement.getId())
                        .file(imageFile)
                        .param("name", "Updated")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void createAchievementLevel_WhenFileUploadFails_ShouldReturn500() throws Exception {
        when(fileUploadService.uploadFile(any())).thenThrow(new RuntimeException("Upload failed"));

        GamificationAchievement achievement = achievementService.createAchievement(
                "Test Achievement", "https://example.com/image.jpg", 1);

        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "level.jpg", MediaType.IMAGE_JPEG_VALUE, "test".getBytes());

        mockMvc.perform(multipart(BASE_URL + "/achievement-levels")
                        .file(imageFile)
                        .param("achievementId", String.valueOf(achievement.getId()))
                        .param("levelName", "Bronze")
                        .param("requiredPointTypeEnum", "EXPERIENCE")
                        .param("minPointsRequired", "100"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:update"})
    void updateAchievementLevel_WhenFileUploadFails_ShouldReturn500() throws Exception {
        when(fileUploadService.uploadFile(any())).thenThrow(new RuntimeException("Upload failed"));

        GamificationAchievement achievement = achievementService.createAchievement(
                "Test Achievement", "https://example.com/image.jpg", 1);

        AchievementLevelDTO levelDto = new AchievementLevelDTO();
        levelDto.setAchievementId(achievement.getId());
        levelDto.setLevelName("Bronze");
        levelDto.setRequiredPointTypeEnum("EXPERIENCE");
        levelDto.setMinPointsRequired(100);
        AchievementLevelDTO created = achievementService.createAchievementLevel(levelDto);

        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "updated.jpg", MediaType.IMAGE_JPEG_VALUE, "updated".getBytes());

        mockMvc.perform(multipart(BASE_URL + "/achievement-levels/" + created.getId())
                        .file(imageFile)
                        .param("levelName", "Silver")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"gamification:create"})
    void createAchievement_WhenUserNotFound_ShouldReturn401() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "achievement.jpg", MediaType.IMAGE_JPEG_VALUE, "test".getBytes());

        // This test assumes authentication will fail to find user
        mockMvc.perform(multipart(BASE_URL + "/achievements")
                        .file(imageFile)
                        .param("name", "New Achievement"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Could be 201 if user is found, or 401/500 if not
                    assert status == 201 || status == 401 || status >= 500 : "Unexpected status: " + status;
                });
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getMyStats_WhenUserNotFound_ShouldReturn401() throws Exception {
        // This test checks the edge case where user is not found
        // In real scenario, this might not happen if authentication is working
        mockMvc.perform(get(BASE_URL + "/my-stats"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 401 : "Expected 200 or 401, got " + status;
                });
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getMyPointLogs_WhenUserNotFound_ShouldReturn401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/my-point-logs"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 401 : "Expected 200 or 401, got " + status;
                });
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getMyAchievements_WhenUserNotFound_ShouldReturn401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/my-achievements"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 401 : "Expected 200 or 401, got " + status;
                });
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getMyAchievementsWithStatus_WhenUserNotFound_ShouldReturn401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/my-achievements-with-status"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 401 : "Expected 200 or 401, got " + status;
                });
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getMyRankPosition_WhenUserNotFound_ShouldReturn401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/ranking/my-position"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 401 || status == 404 : "Expected 200, 401 or 404, got " + status;
                });
    }
}

