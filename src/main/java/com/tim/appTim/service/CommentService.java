package com.tim.appTim.service;

import com.tim.appTim.dto.CommentDTO;
import com.tim.appTim.dto.ReplyCommentDTO;
import com.tim.appTim.entity.Comment;
import com.tim.appTim.entity.ReplyComment;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.exception.ForbiddenException;
import com.tim.appTim.repository.CommentRepository;
import com.tim.appTim.repository.PostRepository;
import com.tim.appTim.repository.ReplyCommentRepository;
import com.tim.appTim.repository.UserRepository;
import com.tim.appTim.repository.ReactionRepository;
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
    private final ReactionRepository reactionRepository;


    private NotificationService notificationService;

    public CommentService(CommentRepository commentRepository,
                          ReplyCommentRepository replyCommentRepository,
                          UserService userService,
                          PostRepository postRepository,
                          UserRepository userRepository,
                          NotificationService notificationService, ReactionRepository reactionRepository) {
        this.commentRepository = commentRepository;
        this.replyCommentRepository = replyCommentRepository;
        this.userService = userService;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.reactionRepository = reactionRepository;
    }

    public CommentDTO createComment(Long postId, Long userId, String content, Comment.Emotion emotion, Long fileId) {
        postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setEmotion(emotion);
        comment.setFileId(fileId);
        comment.setCreatedAt(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);
        
        // Tạo thông báo cho chủ bài viết
        try {
            var post = postRepository.findById(postId).orElse(null);
            var user = userRepository.findById(userId).orElse(null);
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
        return commentRepository.findByPostId(postId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CommentDTO updateComment(Long commentId, Long userId, String content, Comment.Emotion emotion) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        if (!comment.getUserId().equals(userId)) {
            throw new ForbiddenException("Bạn không có quyền sửa bình luận này");
        }

        comment.setContent(content);
        comment.setEmotion(emotion);
        comment.setCreatedAt(LocalDateTime.now());

        return convertToDTO(commentRepository.save(comment));
    }

    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        if (!comment.getUserId().equals(userId)) {
            throw new ForbiddenException("Bạn không có quyền xóa bình luận này");
        }

        List<ReplyComment> replyComments = replyCommentRepository.findByCommentId(commentId);
        for (ReplyComment reply : replyComments) {
            reactionRepository.deleteByReplyCommentId(reply.getId());
        }
        replyCommentRepository.deleteAll(replyComments);
        reactionRepository.deleteByCommentId(commentId);
        commentRepository.delete(comment);
    }

    public ReplyCommentDTO createReplyComment(Long commentId, Long userId, String content, ReplyComment.Emotion emotion, Long fileId) {
        commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        ReplyComment replyComment = new ReplyComment();
        replyComment.setCommentId(commentId);
        replyComment.setUserId(userId);
        replyComment.setContent(content);
        replyComment.setEmotion(emotion);
        replyComment.setFileId(fileId);
        replyComment.setCreatedAt(LocalDateTime.now());

        ReplyComment savedReplyComment = replyCommentRepository.save(replyComment);
        
        // Tạo thông báo cho chủ comment
        try {
            var comment = commentRepository.findById(commentId).orElse(null);
            var user = userRepository.findById(userId).orElse(null);
            if (comment != null && user != null && !comment.getUserId().equals(userId)) {
                notificationService.createCommentNotification(
                    null, // postOwnerId
                    comment.getUserId(), // commentOwnerId
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
        
        return convertReplyToDTO(savedReplyComment);
    }

    public List<ReplyCommentDTO> getReplyCommentsByCommentId(Long commentId) {
        return replyCommentRepository.findByCommentId(commentId)
                .stream()
                .map(this::convertReplyToDTO)
                .collect(Collectors.toList());
    }

    public ReplyCommentDTO updateReplyComment(Long userId, Long replyCommentId, String content, ReplyComment.Emotion emotion) {
        ReplyComment replyComment = replyCommentRepository.findById(replyCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply comment not found with id: " + replyCommentId));

        if (!replyComment.getUserId().equals(userId)) {
            throw new ForbiddenException("Bạn không có quyền sửa phản hồi này");
        }

        replyComment.setContent(content);
        replyComment.setEmotion(emotion);
        replyComment.setCreatedAt(LocalDateTime.now());

        return convertReplyToDTO(replyCommentRepository.save(replyComment));
    }

    public void deleteReplyComment(Long userId, Long replyCommentId) {
        ReplyComment replyComment = replyCommentRepository.findById(replyCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply comment not found with id: " + replyCommentId));

        if (!replyComment.getUserId().equals(userId)) {
            throw new ForbiddenException("Bạn không có quyền xóa phản hồi này");
        }

        replyCommentRepository.delete(replyComment);
    }

    private CommentDTO convertToDTO(Comment comment) {
        User user = comment.getUser();
        String username = comment.getUser() != null ? comment.getUser().getUsername() : "Unknown";
        String userAvatar = user != null ? user.getProfileImage() : " ";
        List<ReplyCommentDTO> replies = replyCommentRepository.findByCommentId(comment.getId())
                .stream().map(this::convertReplyToDTO).collect(Collectors.toList());

        return new CommentDTO( // <-- Thứ tự ĐÚNG
                comment.getId(),              // 1. id
                comment.getUserId(),            // 2. userId
                username,                     // 3. username
                comment.getContent(),         // 4. content <<< ĐÚNG
                userAvatar,                   // 5. userAvatar <<< ĐÚNG
                comment.getEmotion() != null ? comment.getEmotion().name() : null, // 6. emotion
                comment.getFileId(),            // 7. fileId
                comment.getCreatedAt(),         // 8. createdAt
                replies                       // 9. replies
        );
    }

    private ReplyCommentDTO convertReplyToDTO(ReplyComment reply) {
        String username = reply.getUser() != null ? reply.getUser().getUsername() : "Unknown";
        return new ReplyCommentDTO(
                reply.getId(),
                reply.getUserId(),
                username,
                reply.getContent(),
                reply.getEmotion(),
                reply.getFileId(),
                reply.getCreatedAt(),
                reply.getUser() != null ? reply.getUser().getProfileImage() : " "
        );
    }

    public boolean isOwner(Authentication authentication, Long commentId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        User currentUser = userService.findByUsernameOrEmail(authentication.getName());
        if (currentUser == null) return false;

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Comment: " + commentId));
        return comment.getUser().getId().equals(currentUser.getId());
    }

    public boolean isReplyOwner(Authentication authentication, Long replyCommentId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        User currentUser = userService.findByUsernameOrEmail(authentication.getName());
        if (currentUser == null) return false;

        ReplyComment reply = replyCommentRepository.findById(replyCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Reply Comment: " + replyCommentId));
        return reply.getUser().getId().equals(currentUser.getId());
    }

    public long countCommentsByPostId(Long postId) {
        return commentRepository.countByPostId(postId);
    }
}
