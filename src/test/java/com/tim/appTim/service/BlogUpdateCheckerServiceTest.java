package com.tim.appTim.service;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;

import com.tim.appTim.entity.Notification;
import com.tim.appTim.entity.User;
import com.tim.appTim.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlogUpdateCheckerServiceTest {

    @Mock
    private NewsService newsService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BlogUpdateCheckerService blogUpdateCheckerService;

    private BlogDTO blogDTO;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        blogDTO = new BlogDTO();
        blogDTO.setLink("https://blog.example.com/post1");
        blogDTO.setTitle("New Blog Post");

        user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");

        user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
    }

    @Test
    void checkForNewBlogs_WhenNoBlogs_ShouldNotSendNotifications() {
        // Arrange
        when(newsService.getLatestBlogs()).thenReturn(new ArrayList<>());

        // Act
        blogUpdateCheckerService.checkForNewBlogs();

        // Assert
        verify(newsService).getLatestBlogs();
        verify(notificationService, never()).createNotification(anyLong(), any(), any(), any(), anyLong(), anyString(), anyString(), any());
    }

    @Test
    void checkForNewBlogs_WhenFirstTime_ShouldInitialize() {
        // Arrange
        List<BlogDTO> blogs = List.of(blogDTO);
        when(newsService.getLatestBlogs()).thenReturn(blogs);

        // Act
        blogUpdateCheckerService.checkForNewBlogs();

        // Assert
        verify(newsService).getLatestBlogs();
        verify(notificationService, never()).createNotification(anyLong(), any(), any(), any(), anyLong(), anyString(), anyString(), any());
    }

    @Test
    void checkForNewBlogs_WhenNewBlogDetected_ShouldSendNotifications() {
        // Arrange
        List<BlogDTO> blogs = List.of(blogDTO);
        when(newsService.getLatestBlogs()).thenReturn(blogs);
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        // First call to initialize
        blogUpdateCheckerService.checkForNewBlogs();

        // Second blog
        BlogDTO newBlog = new BlogDTO();
        newBlog.setLink("https://blog.example.com/post2");
        newBlog.setTitle("Another Blog Post");
        when(newsService.getLatestBlogs()).thenReturn(List.of(newBlog));

        // Act
        blogUpdateCheckerService.checkForNewBlogs();

        // Assert
        verify(newsService, times(2)).getLatestBlogs();
        verify(notificationService, times(2)).createNotification(
                anyLong(),
                isNull(),
                eq(Notification.NotificationType.BLOG_NEW),
                eq("BLOG_POST"),
                eq(0L),
                eq("Có blog mới từ Codegym"),
                eq("Another Blog Post"),
                any()
        );
    }

    @Test
    void checkForNewBlogs_WhenSameBlog_ShouldNotSendNotifications() {
        // Arrange
        List<BlogDTO> blogs = List.of(blogDTO);
        when(newsService.getLatestBlogs()).thenReturn(blogs);

        // First call to initialize
        blogUpdateCheckerService.checkForNewBlogs();

        // Second call with same blog
        when(newsService.getLatestBlogs()).thenReturn(blogs);

        // Act
        blogUpdateCheckerService.checkForNewBlogs();

        // Assert
        verify(newsService, times(2)).getLatestBlogs();
        verify(notificationService, never()).createNotification(anyLong(), any(), any(), any(), anyLong(), anyString(), anyString(), any());
    }

    @Test
    void checkForNewBlogs_WhenNotificationFails_ShouldContinue() {
        // Arrange
        List<BlogDTO> blogs = List.of(blogDTO);
        when(newsService.getLatestBlogs()).thenReturn(blogs);
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));
        doThrow(new RuntimeException("Notification failed"))
                .when(notificationService).createNotification(eq(1L), any(), any(), any(), anyLong(), anyString(), anyString(), any());

        // First call to initialize
        blogUpdateCheckerService.checkForNewBlogs();

        // New blog
        BlogDTO newBlog = new BlogDTO();
        newBlog.setLink("https://blog.example.com/post2");
        newBlog.setTitle("Another Blog Post");
        when(newsService.getLatestBlogs()).thenReturn(List.of(newBlog));

        // Act - should not throw exception
        assertDoesNotThrow(() -> {
            blogUpdateCheckerService.checkForNewBlogs();
        });

        // Assert
        verify(notificationService, atLeastOnce()).createNotification(anyLong(), any(), any(), any(), anyLong(), anyString(), anyString(), any());
    }

    @Test
    void checkForNewBlogs_WithMultipleUsers_ShouldNotifyAll() {
        // Arrange
        List<User> users = List.of(user1, user2);
        when(userRepository.findAll()).thenReturn(users);
        
        List<BlogDTO> blogs = List.of(blogDTO);
        when(newsService.getLatestBlogs()).thenReturn(blogs);

        // First call to initialize
        blogUpdateCheckerService.checkForNewBlogs();

        // New blog
        BlogDTO newBlog = new BlogDTO();
        newBlog.setLink("https://blog.example.com/post2");
        newBlog.setTitle("Another Blog Post");
        when(newsService.getLatestBlogs()).thenReturn(List.of(newBlog));

        // Act
        blogUpdateCheckerService.checkForNewBlogs();

        // Assert
        verify(notificationService).createNotification(eq(1L), any(), any(), any(), anyLong(), anyString(), anyString(), any());
        verify(notificationService).createNotification(eq(2L), any(), any(), any(), anyLong(), anyString(), anyString(), any());
    }
}


