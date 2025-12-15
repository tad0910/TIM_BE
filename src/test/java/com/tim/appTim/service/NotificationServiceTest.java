package com.tim.appTim.service;

import com.tim.appTim.dto.NotificationDTO;
import com.tim.appTim.entity.Notification;
import com.tim.appTim.entity.User;
import com.tim.appTim.repository.NotificationRepository;
import com.tim.appTim.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private SseService sseService;

    private NotificationService notificationService;

    private Notification notification;
    private User sender;
    private LocalDateTime now;

    private Authentication auth(String name, boolean authenticated) {
        return new Authentication() {
            @Override public Collection<? extends GrantedAuthority> getAuthorities() { return Collections.emptyList(); }
            @Override public Object getCredentials() { return null; }
            @Override public Object getDetails() { return null; }
            @Override public Object getPrincipal() { return null; }
            @Override public boolean isAuthenticated() { return authenticated; }
            @Override public void setAuthenticated(boolean isAuthenticated) { }
            @Override public String getName() { return name; }
        };
    }

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository, userService, userRepository, sseService);
        
        now = LocalDateTime.now();
        
        sender = new User();
        sender.setId(2L);
        sender.setUsername("sender_user");
        sender.setProfileImage("avatar.jpg");

        notification = new Notification();
        notification.setId(1L);
        notification.setReceiverId(1L);
        notification.setSenderId(2L);
        notification.setSender(sender);
        notification.setNotificationType(Notification.NotificationType.POST_REACTION);
        notification.setTargetType("POST");
        notification.setTargetId(10L);
        notification.setTitle("Test Title");
        notification.setContent("Test Content");
        notification.setIsRead(false);
        notification.setCreatedAt(now);
        notification.setReadAt(null);
        
        User receiver = new User();
        receiver.setId(1L);
        notification.setReceiver(receiver);
    }

    // --- createNotification ---

    @Test
    void createNotification_Success() {
        // Arrange
        Notification savedNotification = new Notification();
        savedNotification.setId(1L);
        savedNotification.setReceiverId(1L);
        savedNotification.setSenderId(2L);
        savedNotification.setNotificationType(Notification.NotificationType.POST_REACTION);
        savedNotification.setTargetType("POST");
        savedNotification.setTargetId(10L);
        savedNotification.setTitle("Title");
        savedNotification.setContent("Content");
        savedNotification.setCreatedAt(now);

        when(notificationRepository.existsByReceiverIdAndSenderIdAndNotificationTypeAndTargetTypeAndTargetId(
                anyLong(), anyLong(), any(), anyString(), anyLong())).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(now);
            return saved;
        });
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(savedNotification));
        doNothing().when(sseService).sendNotification(anyLong(), any(NotificationDTO.class));

        // Act
        NotificationDTO result = notificationService.createNotification(
                1L, 2L, Notification.NotificationType.POST_REACTION, "POST", 10L, "Title", "Content");

        // Assert
        assertNotNull(result);
        assertEquals("Title", result.getTitle());
        assertEquals("Content", result.getContent());
        verify(notificationRepository).save(any(Notification.class));
        verify(sseService).sendNotification(eq(1L), any(NotificationDTO.class));
    }

    @Test
    void createNotification_WhenDuplicate_ShouldReturnNull() {
        // Arrange
        when(notificationRepository.existsByReceiverIdAndSenderIdAndNotificationTypeAndTargetTypeAndTargetId(
                eq(1L), eq(2L), eq(Notification.NotificationType.POST_REACTION), eq("POST"), eq(10L)))
                .thenReturn(true);

        // Act
        NotificationDTO result = notificationService.createNotification(
                1L, 2L, Notification.NotificationType.POST_REACTION, "POST", 10L, "Title", "Content");

        // Assert
        assertNull(result);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createNotification_WhenSenderIdIsNull_ShouldNotCheckDuplicate() {
        // Arrange
        Notification savedNotification = new Notification();
        savedNotification.setId(1L);
        savedNotification.setReceiverId(1L);
        savedNotification.setSenderId(null);
        savedNotification.setNotificationType(Notification.NotificationType.SYSTEM_ANNOUNCEMENT);
        savedNotification.setTargetType("SYSTEM");
        savedNotification.setTargetId(0L);
        savedNotification.setTitle("Title");
        savedNotification.setContent("Content");

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(savedNotification));
        doNothing().when(sseService).sendNotification(anyLong(), any(NotificationDTO.class));

        // Act
        NotificationDTO result = notificationService.createNotification(
                1L, null, Notification.NotificationType.SYSTEM_ANNOUNCEMENT, "SYSTEM", 0L, "Title", "Content");

        // Assert
        assertNotNull(result);
        verify(notificationRepository, never()).existsByReceiverIdAndSenderIdAndNotificationTypeAndTargetTypeAndTargetId(
                anyLong(), isNull(), any(), anyString(), anyLong());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void createNotification_WhenGradeNotification_ShouldNotCheckDuplicate() {
        // Arrange
        Notification savedNotification = new Notification();
        savedNotification.setId(1L);
        savedNotification.setReceiverId(1L);
        savedNotification.setSenderId(2L);
        savedNotification.setNotificationType(Notification.NotificationType.GRADE_NEW);
        savedNotification.setTargetType("GRADE");
        savedNotification.setTargetId(10L);
        savedNotification.setTitle("Title");
        savedNotification.setContent("Content");

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(savedNotification));
        doNothing().when(sseService).sendNotification(anyLong(), any(NotificationDTO.class));

        // Act
        NotificationDTO result = notificationService.createNotification(
                1L, 2L, Notification.NotificationType.GRADE_NEW, "GRADE", 10L, "Title", "Content");

        // Assert
        assertNotNull(result);
        verify(notificationRepository, never()).existsByReceiverIdAndSenderIdAndNotificationTypeAndTargetTypeAndTargetId(
                anyLong(), anyLong(), eq(Notification.NotificationType.GRADE_NEW), anyString(), anyLong());
        verify(notificationRepository).save(any(Notification.class));
    }

    // --- getNotificationsByUserId ---

    @Test
    void getNotificationsByUserId_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(notificationPage);

        // Act
        Page<NotificationDTO> result = notificationService.getNotificationsByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(notificationRepository).findByReceiverIdOrderByCreatedAtDesc(1L, pageable);
    }

    @Test
    void getNotificationsByUserId_Empty() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> emptyPage = Page.empty();
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(emptyPage);

        // Act
        Page<NotificationDTO> result = notificationService.getNotificationsByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    // --- getUnreadNotificationsByUserId ---

    @Test
    void getUnreadNotificationsByUserId_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(notificationPage);

        // Act
        Page<NotificationDTO> result = notificationService.getUnreadNotificationsByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertFalse(result.getContent().get(0).getIsRead());
        verify(notificationRepository).findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(1L, pageable);
    }

    @Test
    void getUnreadNotificationsByUserId_Empty() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> emptyPage = Page.empty();
        when(notificationRepository.findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(emptyPage);

        // Act
        Page<NotificationDTO> result = notificationService.getUnreadNotificationsByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    // --- getUnreadNotificationCount ---

    @Test
    void getUnreadNotificationCount_Success() {
        // Arrange
        when(notificationRepository.countByReceiverIdAndIsReadFalse(1L)).thenReturn(5L);

        // Act
        long result = notificationService.getUnreadNotificationCount(1L);

        // Assert
        assertEquals(5L, result);
        verify(notificationRepository).countByReceiverIdAndIsReadFalse(1L);
    }

    @Test
    void getUnreadNotificationCount_Zero() {
        // Arrange
        when(notificationRepository.countByReceiverIdAndIsReadFalse(1L)).thenReturn(0L);

        // Act
        long result = notificationService.getUnreadNotificationCount(1L);

        // Assert
        assertEquals(0L, result);
    }

    // --- markAllAsRead ---

    @Test
    void markAllAsRead_Success() {
        // Arrange
        doNothing().when(notificationRepository).markAllAsReadByReceiverId(1L);

        // Act
        notificationService.markAllAsRead(1L);

        // Assert
        verify(notificationRepository).markAllAsReadByReceiverId(1L);
    }

    // --- markAsRead ---

    @Test
    void markAsRead_Success() {
        // Arrange
        User receiver = new User();
        receiver.setId(1L);
        notification.setReceiver(receiver);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        org.springframework.security.core.Authentication auth = auth("user", true);

        // Act
        notificationService.markAsRead(1L, 1L, auth);

        // Assert
        assertTrue(notification.getIsRead());
        assertNotNull(notification.getReadAt());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_Fail_NotFound() {
        // Arrange
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());
        org.springframework.security.core.Authentication auth = auth("user", true);

        // Act & Assert
        assertThrows(com.tim.appTim.exception.ResourceNotFoundException.class, () -> {
            notificationService.markAsRead(999L, 1L, auth);
        });
    }

    @Test
    void markAsRead_Fail_Unauthorized() {
        // Arrange
        User receiver = new User();
        receiver.setId(1L);
        notification.setReceiver(receiver);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        org.springframework.security.core.Authentication auth = auth("user", true);

        // Act & Assert
        assertThrows(com.tim.appTim.exception.ForbiddenException.class, () -> {
            notificationService.markAsRead(1L, 2L, auth); // Different user
        });
    }

    // --- deleteOldNotifications ---

    @Test
    void deleteOldNotifications_Success() {
        // Arrange
        doNothing().when(notificationRepository).deleteOldNotifications(any(LocalDateTime.class));

        // Act
        notificationService.deleteOldNotifications();

        // Assert
        verify(notificationRepository).deleteOldNotifications(argThat(date -> 
            date.isBefore(LocalDateTime.now()) && date.isAfter(LocalDateTime.now().minusDays(31))
        ));
    }

    // --- getNotificationsByType ---

    @Test
    void getNotificationsByType_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByReceiverIdAndNotificationTypeOrderByCreatedAtDesc(
                1L, Notification.NotificationType.POST_REACTION, pageable))
                .thenReturn(notificationPage);

        // Act
        Page<NotificationDTO> result = notificationService.getNotificationsByType(
                1L, Notification.NotificationType.POST_REACTION, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(notificationRepository).findByReceiverIdAndNotificationTypeOrderByCreatedAtDesc(
                1L, Notification.NotificationType.POST_REACTION, pageable);
    }

    @Test
    void getNotificationsByType_Empty() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> emptyPage = Page.empty();
        when(notificationRepository.findByReceiverIdAndNotificationTypeOrderByCreatedAtDesc(
                1L, Notification.NotificationType.POST_COMMENT, pageable))
                .thenReturn(emptyPage);

        // Act
        Page<NotificationDTO> result = notificationService.getNotificationsByType(
                1L, Notification.NotificationType.POST_COMMENT, pageable);

        // Assert
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    // --- createReactionNotification ---

    @Test
    void createReactionNotification_POST_REACTION_Success() {
        // Arrange
        when(notificationRepository.existsByReceiverIdAndSenderIdAndNotificationTypeAndTargetTypeAndTargetId(
                anyLong(), anyLong(), any(), anyString(), anyLong())).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        Notification savedNotification = new Notification();
        savedNotification.setId(1L);
        savedNotification.setReceiverId(1L);
        savedNotification.setSenderId(2L);
        savedNotification.setNotificationType(Notification.NotificationType.POST_REACTION);
        savedNotification.setTargetType("POST");
        savedNotification.setTargetId(10L);
        savedNotification.setTitle("Bài viết của bạn được bày tỏ cảm xúc");
        savedNotification.setContent("content");
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(savedNotification));
        doNothing().when(sseService).sendNotification(anyLong(), any(NotificationDTO.class));

        // Act
        notificationService.createReactionNotification(
                1L, null, null, 2L, "sender_user",
                Notification.NotificationType.POST_REACTION, "POST", 10L);

        // Assert
        verify(notificationRepository).save(argThat(n -> 
            n.getReceiverId().equals(1L) &&
            n.getSenderId().equals(2L) &&
            n.getNotificationType() == Notification.NotificationType.POST_REACTION &&
            n.getTitle().equals("Bài viết của bạn được bày tỏ cảm xúc")
        ));
    }

    @Test
    void createReactionNotification_COMMENT_REACTION_Success() {
        // Arrange
        when(notificationRepository.existsByReceiverIdAndSenderIdAndNotificationTypeAndTargetTypeAndTargetId(
                anyLong(), anyLong(), any(), anyString(), anyLong())).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        Notification savedNotification = new Notification();
        savedNotification.setId(1L);
        savedNotification.setReceiverId(1L);
        savedNotification.setSenderId(2L);
        savedNotification.setNotificationType(Notification.NotificationType.COMMENT_REACTION);
        savedNotification.setTargetType("COMMENT");
        savedNotification.setTargetId(20L);
        savedNotification.setTitle("Bình luận của bạn được bày tỏ cảm xúc");
        savedNotification.setContent("content");
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(savedNotification));
        doNothing().when(sseService).sendNotification(anyLong(), any(NotificationDTO.class));

        // Act
        notificationService.createReactionNotification(
                null, 1L, null, 2L, "sender_user",
                Notification.NotificationType.COMMENT_REACTION, "COMMENT", 20L);

        // Assert
        verify(notificationRepository).save(argThat(n -> 
            n.getReceiverId().equals(1L) &&
            n.getNotificationType() == Notification.NotificationType.COMMENT_REACTION &&
            n.getTitle().equals("Bình luận của bạn được bày tỏ cảm xúc")
        ));
    }

    @Test
    void createReactionNotification_REPLY_REACTION_Success() {
        // Arrange
        when(notificationRepository.existsByReceiverIdAndSenderIdAndNotificationTypeAndTargetTypeAndTargetId(
                anyLong(), anyLong(), any(), anyString(), anyLong())).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        Notification savedNotification = new Notification();
        savedNotification.setId(1L);
        savedNotification.setReceiverId(1L);
        savedNotification.setSenderId(2L);
        savedNotification.setNotificationType(Notification.NotificationType.REPLY_REACTION);
        savedNotification.setTargetType("REPLY");
        savedNotification.setTargetId(30L);
        savedNotification.setTitle("Phản hồi của bạn được bày tỏ cảm xúc");
        savedNotification.setContent("content");
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(savedNotification));
        doNothing().when(sseService).sendNotification(anyLong(), any(NotificationDTO.class));

        // Act
        notificationService.createReactionNotification(
                null, null, 1L, 2L, "sender_user",
                Notification.NotificationType.REPLY_REACTION, "REPLY", 30L);

        // Assert
        verify(notificationRepository).save(argThat(n -> 
            n.getReceiverId().equals(1L) &&
            n.getNotificationType() == Notification.NotificationType.REPLY_REACTION &&
            n.getTitle().equals("Phản hồi của bạn được bày tỏ cảm xúc")
        ));
    }

    @Test
    void createReactionNotification_OtherTypes_ShouldNotCreate() {
        // Arrange

        // Act
        notificationService.createReactionNotification(
                1L, null, null, 2L, "sender_user",
                Notification.NotificationType.POST_COMMENT, "POST", 10L);

        // Assert
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createReactionNotification_WhenSenderIdIsNull_ShouldNotCreate() {
        // Act
        notificationService.createReactionNotification(
                1L, null, null, null, "sender_user",
                Notification.NotificationType.POST_REACTION, "POST", 10L);

        // Assert
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createReactionNotification_WhenReceiverEqualsSender_ShouldNotCreate() {
        // Act
        notificationService.createReactionNotification(
                2L, null, null, 2L, "sender_user",
                Notification.NotificationType.POST_REACTION, "POST", 10L);

        // Assert
        verify(notificationRepository, never()).save(any());
    }

    // --- createCommentNotification ---

    @Test
    void createCommentNotification_POST_Success() {
        // Arrange
        when(notificationRepository.existsByReceiverIdAndSenderIdAndNotificationTypeAndTargetTypeAndTargetId(
                anyLong(), anyLong(), any(), anyString(), anyLong())).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        Notification savedNotification = new Notification();
        savedNotification.setId(1L);
        savedNotification.setReceiverId(1L);
        savedNotification.setSenderId(2L);
        savedNotification.setNotificationType(Notification.NotificationType.POST_COMMENT);
        savedNotification.setTargetType("POST");
        savedNotification.setTargetId(10L);
        savedNotification.setTitle("Bài viết của bạn có bình luận mới");
        savedNotification.setContent("content");
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(savedNotification));
        doNothing().when(sseService).sendNotification(anyLong(), any(NotificationDTO.class));

        // Act
        notificationService.createCommentNotification(
                1L, null, 2L, "sender_user", "POST", 10L);

        // Assert
        verify(notificationRepository).save(argThat(n -> 
            n.getReceiverId().equals(1L) &&
            n.getNotificationType() == Notification.NotificationType.POST_COMMENT &&
            n.getTitle().equals("Bài viết của bạn có bình luận mới")
        ));
    }

    @Test
    void createCommentNotification_COMMENT_Success() {
        // Arrange
        when(notificationRepository.existsByReceiverIdAndSenderIdAndNotificationTypeAndTargetTypeAndTargetId(
                anyLong(), anyLong(), any(), anyString(), anyLong())).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        Notification savedNotification = new Notification();
        savedNotification.setId(1L);
        savedNotification.setReceiverId(1L);
        savedNotification.setSenderId(2L);
        savedNotification.setNotificationType(Notification.NotificationType.COMMENT_REPLY);
        savedNotification.setTargetType("COMMENT");
        savedNotification.setTargetId(20L);
        savedNotification.setTitle("Bình luận của bạn có phản hồi mới");
        savedNotification.setContent("content");
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(savedNotification));
        doNothing().when(sseService).sendNotification(anyLong(), any(NotificationDTO.class));

        // Act
        notificationService.createCommentNotification(
                null, 1L, 2L, "sender_user", "COMMENT", 20L);

        // Assert
        verify(notificationRepository).save(argThat(n -> 
            n.getReceiverId().equals(1L) &&
            n.getNotificationType() == Notification.NotificationType.COMMENT_REPLY &&
            n.getTitle().equals("Bình luận của bạn có phản hồi mới")
        ));
    }

    @Test
    void createCommentNotification_WhenSenderIdIsNull_ShouldNotCreate() {
        // Act
        notificationService.createCommentNotification(
                1L, null, null, "sender_user", "POST", 10L);

        // Assert
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createCommentNotification_WhenReceiverEqualsSender_ShouldNotCreate() {
        // Act
        notificationService.createCommentNotification(
                2L, null, 2L, "sender_user", "POST", 10L);

        // Assert
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createCommentNotification_WhenTargetTypeIsInvalid_ShouldNotCreate() {
        // Act
        notificationService.createCommentNotification(
                1L, null, 2L, "sender_user", "INVALID", 10L);

        // Assert
        verify(notificationRepository, never()).save(any());
    }

    // --- convertToDTO ---

    @Test
    void convertToDTO_WithSender() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(notificationPage);

        // Act
        Page<NotificationDTO> result = notificationService.getNotificationsByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("sender_user", result.getContent().get(0).getSenderUsername());
    }

    @Test
    void convertToDTO_WithoutSender() {
        // Arrange
        notification.setSender(null);
        notification.setSenderId(null);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(notificationPage);

        // Act
        Page<NotificationDTO> result = notificationService.getNotificationsByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Hệ thống", result.getContent().get(0).getSenderUsername());
    }

    @Test
    void convertToDTO_LoadSenderFromRepository() {
        // Arrange
        notification.setSender(null);
        notification.setSenderId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(sender));
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(notificationPage);

        // Act
        Page<NotificationDTO> result = notificationService.getNotificationsByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("sender_user", result.getContent().get(0).getSenderUsername());
        verify(userRepository).findById(2L);
    }

    @Test
    void convertToDTO_LoadSenderFromRepository_NotFound() {
        // Arrange
        notification.setSender(null);
        notification.setSenderId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(notificationPage);

        // Act
        Page<NotificationDTO> result = notificationService.getNotificationsByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Hệ thống", result.getContent().get(0).getSenderUsername());
    }

    // --- generateActionUrl ---

    @Test
    void generateActionUrl_POST_REACTION() {
        // Arrange
        notification.setNotificationType(Notification.NotificationType.POST_REACTION);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(notificationPage);

        // Act
        Page<NotificationDTO> result = notificationService.getNotificationsByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals("/posts/10", result.getContent().get(0).getActionUrl());
    }

    @Test
    void generateActionUrl_POST_COMMENT() {
        // Arrange
        notification.setNotificationType(Notification.NotificationType.POST_COMMENT);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(notificationPage);

        // Act
        Page<NotificationDTO> result = notificationService.getNotificationsByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals("/posts/10", result.getContent().get(0).getActionUrl());
    }

    @Test
    void generateActionUrl_COMMENT_REACTION() {
        // Arrange
        notification.setNotificationType(Notification.NotificationType.COMMENT_REACTION);
        notification.setTargetId(20L);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(notificationPage);

        // Act
        Page<NotificationDTO> result = notificationService.getNotificationsByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        // getPostIdFromComment returns null, so URL will be "/posts/null"
        assertTrue(result.getContent().get(0).getActionUrl().contains("/posts/"));
    }

    @Test
    void generateActionUrl_COMMENT_REPLY() {
        // Arrange
        notification.setNotificationType(Notification.NotificationType.COMMENT_REPLY);
        notification.setTargetId(20L);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(notificationPage);

        // Act
        Page<NotificationDTO> result = notificationService.getNotificationsByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        assertTrue(result.getContent().get(0).getActionUrl().contains("/posts/"));
    }

    @Test
    void generateActionUrl_REPLY_REACTION() {
        // Arrange
        notification.setNotificationType(Notification.NotificationType.REPLY_REACTION);
        notification.setTargetId(30L);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(notificationPage);

        // Act
        Page<NotificationDTO> result = notificationService.getNotificationsByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        assertTrue(result.getContent().get(0).getActionUrl().contains("/posts/"));
    }

    @Test
    void generateActionUrl_USER_FOLLOW() {
        // Arrange
        notification.setNotificationType(Notification.NotificationType.USER_FOLLOW);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(notificationPage);

        // Act
        Page<NotificationDTO> result = notificationService.getNotificationsByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals("/users/2", result.getContent().get(0).getActionUrl());
    }

    @Test
    void generateActionUrl_Default() {
        // Arrange
        notification.setNotificationType(Notification.NotificationType.POST_MENTION);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(notificationPage);

        // Act
        Page<NotificationDTO> result = notificationService.getNotificationsByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals("/", result.getContent().get(0).getActionUrl());
    }

    @Test
    void generateActionUrl_LATE_ATTENDANCE_OPENED() {
        // Arrange
        notification.setNotificationType(Notification.NotificationType.LATE_ATTENDANCE_OPENED);
        notification.setTargetId(100L);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(notificationPage);

        // Act
        Page<NotificationDTO> result = notificationService.getNotificationsByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals("/attendance/schedule/100", result.getContent().get(0).getActionUrl());
    }

    @Test
    void generateActionUrl_ATTENDANCE_REMINDER_LATE() {
        // Arrange
        notification.setNotificationType(Notification.NotificationType.ATTENDANCE_REMINDER_LATE);
        notification.setTargetId(100L);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(notificationPage);

        // Act
        Page<NotificationDTO> result = notificationService.getNotificationsByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals("/attendance/schedule/100", result.getContent().get(0).getActionUrl());
    }

    @Test
    void generateActionUrl_ATTENDANCE_REMINDER_ENDING() {
        // Arrange
        notification.setNotificationType(Notification.NotificationType.ATTENDANCE_REMINDER_ENDING);
        notification.setTargetId(100L);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(notificationPage);

        // Act
        Page<NotificationDTO> result = notificationService.getNotificationsByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals("/attendance/schedule/100", result.getContent().get(0).getActionUrl());
    }

    @Test
    void generateActionUrl_GRADE_NEW() {
        // Arrange
        notification.setNotificationType(Notification.NotificationType.GRADE_NEW);
        notification.setTargetId(100L);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(notificationPage);

        // Act
        Page<NotificationDTO> result = notificationService.getNotificationsByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals("/class-modules/100/my-grades", result.getContent().get(0).getActionUrl());
    }

    @Test
    void generateActionUrl_GRADE_UPDATED() {
        // Arrange
        notification.setNotificationType(Notification.NotificationType.GRADE_UPDATED);
        notification.setTargetId(100L);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(notificationPage);

        // Act
        Page<NotificationDTO> result = notificationService.getNotificationsByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals("/class-modules/100/my-grades", result.getContent().get(0).getActionUrl());
    }

    @Test
    void generateActionUrl_BLOG_NEW() {
        // Arrange
        notification.setNotificationType(Notification.NotificationType.BLOG_NEW);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(notificationPage);

        // Act
        Page<NotificationDTO> result = notificationService.getNotificationsByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals("/news", result.getContent().get(0).getActionUrl());
    }

    @Test
    void generateActionUrl_TUITION_OVERDUE() {
        // Arrange
        notification.setNotificationType(Notification.NotificationType.TUITION_OVERDUE);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(notificationPage);

        // Act
        Page<NotificationDTO> result = notificationService.getNotificationsByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals("/tuition/my-overview", result.getContent().get(0).getActionUrl());
    }

    // --- isReceiver ---

    @Test
    void isReceiver_Success() {
        // Arrange
        User receiver = new User();
        receiver.setId(1L);
        notification.setReceiver(receiver);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(userService.findByUsernameOrEmail("testuser")).thenReturn(receiver);
        org.springframework.security.core.Authentication auth = auth("testuser", true);

        // Act
        boolean result = notificationService.isReceiver(auth, 1L);

        // Assert
        assertTrue(result);
    }

    @Test
    void isReceiver_Fail_NotAuthenticated() {
        // Arrange
        org.springframework.security.core.Authentication auth = auth("any", false);

        // Act
        boolean result = notificationService.isReceiver(auth, 1L);

        // Assert
        assertFalse(result);
    }

    @Test
    void isReceiver_Fail_AuthNull() {
        // Act
        boolean result = notificationService.isReceiver(null, 1L);

        // Assert
        assertFalse(result);
    }

    @Test
    void isReceiver_Fail_UserNotFound() {
        // Arrange
        org.springframework.security.core.Authentication auth = auth("testuser", true);
        when(userService.findByUsernameOrEmail("testuser")).thenReturn(null);

        // Act
        boolean result = notificationService.isReceiver(auth, 1L);

        // Assert
        assertFalse(result);
    }

    @Test
    void isReceiver_Fail_NotificationNotFound() {
        // Arrange
        User receiver = new User();
        receiver.setId(1L);
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());
        when(userService.findByUsernameOrEmail("testuser")).thenReturn(receiver);
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("testuser");

        // Act & Assert
        assertThrows(java.util.NoSuchElementException.class, () -> {
            notificationService.isReceiver(auth, 999L);
        });
    }

    @Test
    void isReceiver_Fail_NotReceiver() {
        // Arrange
        User receiver = new User();
        receiver.setId(1L);
        User otherUser = new User();
        otherUser.setId(2L);
        notification.setReceiver(receiver);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(userService.findByUsernameOrEmail("otheruser")).thenReturn(otherUser);
        org.springframework.security.core.Authentication auth = auth("otheruser", true);

        // Act
        boolean result = notificationService.isReceiver(auth, 1L);

        // Assert
        assertFalse(result);
    }

    @Test
    void createReactionNotification_POST_MENTION_ShouldNotCreate() {
        // Act
        notificationService.createReactionNotification(
                1L, null, null, 2L, "sender_user",
                Notification.NotificationType.POST_MENTION, "POST", 10L);

        // Assert
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createReactionNotification_COMMENT_MENTION_ShouldNotCreate() {
        // Act
        notificationService.createReactionNotification(
                null, 1L, null, 2L, "sender_user",
                Notification.NotificationType.COMMENT_MENTION, "COMMENT", 20L);

        // Assert
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createReactionNotification_USER_FOLLOW_ShouldNotCreate() {
        // Act
        notificationService.createReactionNotification(
                1L, null, null, 2L, "sender_user",
                Notification.NotificationType.USER_FOLLOW, "USER", 2L);

        // Assert
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createReactionNotification_SYSTEM_ANNOUNCEMENT_ShouldNotCreate() {
        // Act
        notificationService.createReactionNotification(
                1L, null, null, 2L, "sender_user",
                Notification.NotificationType.SYSTEM_ANNOUNCEMENT, "SYSTEM", 0L);

        // Assert
        verify(notificationRepository, never()).save(any());
    }
}

