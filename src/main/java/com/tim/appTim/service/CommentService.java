package com.tim.appTim.service;

import com.tim.appTim.dto.CommentDTO;
import com.tim.appTim.dto.ReplyCommentDTO;
import com.tim.appTim.entity.Comment;
import com.tim.appTim.entity.Post;
import com.tim.appTim.entity.ReplyComment;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.exception.ForbiddenException;
import com.tim.appTim.repository.CommentRepository;
import com.tim.appTim.repository.PostRepository;
import com.tim.appTim.repository.ReplyCommentRepository;
import com.tim.appTim.repository.UserRepository;
import com.tim.appTim.repository.ReactionRepository;
import com.tim.appTim.service.NotificationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service("commentService")
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final ReplyCommentRepository replyCommentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    @Autowired
    public CommentService(CommentRepository commentRepository,
                          ReplyCommentRepository replyCommentRepository,
                          PostRepository postRepository,
                          UserRepository userRepository,
                          UserService userService, NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.replyCommentRepository = replyCommentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @Transactional
    public CommentDTO createComment(Long postId, Long userId, String content, Comment.Emotion emotion, Long fileId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(content);
        comment.setEmotion(emotion);
        comment.setFileId(fileId);
        comment.setCreatedAt(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);

        Integer currentTotal = post.getTotalComments();
        post.setTotalComments(currentTotal == null ? 1 : currentTotal + 1);
        postRepository.save(post);

        // Tạo thông báo cho chủ bài viết
        try {

            if (post != null && user != null && !post.getUser().getId().equals(userId)) {
                notificationService.createCommentNotification(
                    post.getUser().getId(), // postOwnerId
                    null, // commentOwnerId
                    userId, // senderId
                    user.getUsername(), // senderUsername
                    "POST",
                    postId
                );
            }
        } catch (Exception e) {
            // Log error but don't fail the comment creation
            System.err.println("Error creating notification: " + e.getMessage());
        }

        return convertToDTO(savedComment);
    }

    public List<CommentDTO> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostId(postId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CommentDTO updateComment(Long commentId, User currentUser, Authentication authentication, String content, Comment.Emotion emotion) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("comment:update_all"));
        boolean isOwner = comment.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("Bạn không có quyền sửa bình luận này");
        }

        comment.setContent(content);
        comment.setEmotion(emotion);
        comment.setUpdatedAt(LocalDateTime.now());

        Comment updatedComment = commentRepository.save(comment);
        return convertToDTO(updatedComment);
    }

    public void deleteComment(Long commentId, User currentUser, Authentication authentication) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("comment:delete_all"));
        boolean isOwner = comment.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("Bạn không có quyền xóa bình luận này");
        }
        Post post = comment.getPost();
        if (post != null) {
            Integer currentTotal = post.getTotalComments();
            // Đảm bảo không bị âm
            post.setTotalComments(currentTotal == null || currentTotal <= 0 ? 0 : currentTotal - 1);
            postRepository.save(post);
        }
        commentRepository.delete(comment);
    }

    @Transactional
    public ReplyCommentDTO createReplyComment(Long commentId, Long userId, String content, ReplyComment.Emotion emotion, Long fileId) {

        // Bước 1: Lấy các đối tượng cha từ DB và gán vào biến
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Bước 2: Tạo Reply và set đối tượng
        ReplyComment reply = new ReplyComment();
        reply.setComment(comment); // Dùng biến 'comment' đã lấy ở trên
        reply.setUser(user);       // Dùng biến 'user' đã lấy ở trên
        reply.setContent(content);
        reply.setEmotion(emotion);
        reply.setFileId(fileId);
        reply.setCreatedAt(LocalDateTime.now());

        ReplyComment savedReplyComment = replyCommentRepository.save(reply); // Sửa 'replyComment' thành 'reply'

        // Bước 3: Tạo thông báo (Đã sửa logic)
        try {
            // Không cần fetch lại 'comment' và 'user', chúng ta đã có sẵn

            // Sửa logic check: dùng 'comment.getUser().getId()' thay vì 'comment.getUserId()'
            if (!comment.getUser().getId().equals(userId)) {
                notificationService.createCommentNotification(
                        null, // postOwnerId
                        comment.getUser().getId(), // commentOwnerId (SỬA Ở ĐÂY)
                        userId, // senderId
                        user.getUsername(), // senderUsername
                        "COMMENT",
                        commentId
                );
            }
        } catch (Exception e) {
            // Log error but don't fail the reply creation
            System.err.println("Error creating notification: " + e.getMessage());
        }

        return convertReplyToDTO(savedReplyComment); // Giữ nguyên tên hàm của bạn
    }

    public List<ReplyCommentDTO> getReplyCommentsByCommentId(Long commentId) {
        return replyCommentRepository.findByCommentId(commentId)
                .stream()
                .map(this::convertReplyToDTO)
                .collect(Collectors.toList());
    }

    public ReplyCommentDTO updateReplyComment(User currentUser, Authentication authentication, Long replyCommentId, String content, ReplyComment.Emotion emotion) {
        ReplyComment reply = replyCommentRepository.findById(replyCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply comment not found with id: " + replyCommentId));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("comment:update_all"));
        boolean isOwner = reply.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("Bạn không có quyền sửa trả lời này");
        }

        reply.setContent(content);
        reply.setEmotion(emotion);
        reply.setUpdatedAt(LocalDateTime.now());

        ReplyComment updatedReply = replyCommentRepository.save(reply);
        return convertReplyToDTO(updatedReply);
    }

    public void deleteReplyComment(User currentUser, Authentication authentication, Long replyCommentId) {
        ReplyComment replyComment = replyCommentRepository.findById(replyCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply comment not found with id: " + replyCommentId));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("comment:delete_all"));
        boolean isOwner = replyComment.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("Bạn không có quyền xóa trả lời này");
        }

        replyCommentRepository.delete(replyComment);
    }

    private User getUserFromAuthentication(Authentication authentication) {
        // Lấy user entity từ logic của bạn (ví dụ: qua UserService)
        return userService.findByUsernameOrEmail(authentication.getName());
    }

    private CommentDTO convertToDTO(Comment comment) {
        User user = comment.getUser();
        String username = (user != null) ? user.getUsername() : "Unknown";
        String userAvatar = (user != null) ? user.getProfileImage() : " ";
        String emotionName = (comment.getEmotion() != null) ? comment.getEmotion().name() : null;

        // Sửa lỗi tên hàm (Lỗi 3, 5): phải là 'convertReplyToDTO'
        List<ReplyCommentDTO> replies = comment.getReplies()
                .stream()
                .map(this::convertReplyToDTO)
                .collect(Collectors.toList());

        // SẮP XẾP LẠI TOÀN BỘ THAM SỐ CHO ĐÚNG
        return new CommentDTO(
                comment.getId(),            // 1. Long id
                comment.getUser().getId(),  // 2. Long userId
                username,                   // 3. String username
                comment.getContent(),       // 4. String content
                userAvatar,                 // 5. String userAvatar
                emotionName,                // 6. String emotion
                comment.getFileId(),        // 7. Long fileId
                comment.getCreatedAt(),     // 8. LocalDateTime createdAt
                replies                     // 9. List<ReplyCommentDTO> replyComments
        );
    }

    private ReplyCommentDTO convertReplyToDTO(ReplyComment reply) {
        String username = reply.getUser() != null ? reply.getUser().getUsername() : "Unknown";
        String emotionName = reply.getEmotion() != null ? reply.getEmotion().name() : null;
        return new ReplyCommentDTO(
                reply.getId(),
                reply.getComment().getId(),
                reply.getUser().getId(),
                username,
                reply.getContent(),
                reply.getEmotion(),
                reply.getFileId(),
                reply.getCreatedAt(),
                reply.getUser() != null ? reply.getUser().getProfileImage() : " "
        );
    }

    public boolean isOwner(Authentication authentication, Long commentId) {
        User currentUser = getUserFromAuthentication(authentication);
        return commentRepository.findById(commentId)
                .map(comment -> comment.getUser().getId().equals(currentUser.getId()))
                .orElse(false); // <-- Sửa: Nếu không tìm thấy, trả về false (không phải là owner)
    }

    public boolean isReplyOwner(Authentication authentication, Long replyCommentId) {
        User currentUser = getUserFromAuthentication(authentication);
        return replyCommentRepository.findById(replyCommentId)
                .map(reply -> reply.getUser().getId().equals(currentUser.getId()))
                .orElse(false); // <-- Sửa: Nếu không tìm thấy, trả về false
    }

    public long countCommentsByPostId(Long postId) {
        return commentRepository.countByPostId(postId);
    }
}
