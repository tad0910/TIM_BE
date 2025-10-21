package com.tim.appTim.service;

import com.tim.appTim.dto.CommentDTO;
import com.tim.appTim.dto.ReplyCommentDTO;
import com.tim.appTim.entity.Comment;
import com.tim.appTim.entity.ReplyComment;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.CommentRepository;
import com.tim.appTim.repository.PostRepository;
import com.tim.appTim.repository.ReplyCommentRepository;
import com.tim.appTim.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.NoSuchElementException;

@Service("commentService")
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final ReplyCommentRepository replyCommentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public CommentService(CommentRepository commentRepository,
                          ReplyCommentRepository replyCommentRepository,
                          UserService userService, PostRepository postRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.replyCommentRepository = replyCommentRepository;
        this.userService = userService;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
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
        return convertToDTO(savedComment);
    }

    public List<CommentDTO> getCommentsByPostId(Long postId) {
        List<Comment> comments = commentRepository.findByPostId(postId);
        return comments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CommentDTO updateComment(Long commentId, Long userId, String content, Comment.Emotion emotion) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("You don't have permission to update this comment");
        }

        comment.setContent(content);
        comment.setEmotion(emotion);
        comment.setCreatedAt(LocalDateTime.now());

        Comment updatedComment = commentRepository.save(comment);
        return convertToDTO(updatedComment);
    }

    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("You don't have permission to delete this comment");
        }

        List<ReplyComment> replyComments = replyCommentRepository.findByCommentId(commentId);
        replyCommentRepository.deleteAll(replyComments);
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
        return convertReplyToDTO(savedReplyComment);
    }

    public List<ReplyCommentDTO> getReplyCommentsByCommentId(Long commentId) {
        List<ReplyComment> replyComments = replyCommentRepository.findByCommentId(commentId);
        return replyComments.stream()
                .map(this::convertReplyToDTO)
                .collect(Collectors.toList());
    }

    public ReplyCommentDTO updateReplyComment(Long userId, Long replyCommentId, String content, ReplyComment.Emotion emotion) {
        ReplyComment replyComment = replyCommentRepository.findById(replyCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply comment not found with id: " + replyCommentId));

        replyComment.setContent(content);
        replyComment.setEmotion(emotion);
        replyComment.setCreatedAt(LocalDateTime.now());

        ReplyComment updatedReplyComment = replyCommentRepository.save(replyComment);
        return convertReplyToDTO(updatedReplyComment);
    }

    public void deleteReplyComment(Long userId, Long replyCommentId) {
        ReplyComment replyComment = replyCommentRepository.findById(replyCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply comment not found with id: " + replyCommentId));

        replyCommentRepository.delete(replyComment);
    }

    private CommentDTO convertToDTO(Comment comment) {
        String username = comment.getUser() != null ? comment.getUser().getUsername() : "Unknown";
        List<ReplyComment> replyComments = replyCommentRepository.findByCommentId(comment.getId());
        List<ReplyCommentDTO> replyCommentDTOs = replyComments.stream()
                .map(this::convertReplyToDTO)
                .collect(Collectors.toList());

        return new CommentDTO(
                comment.getId(),
                comment.getUserId(),
                username,
                comment.getContent(),
                comment.getEmotion() != null ? comment.getEmotion().name() : null,
                comment.getFileId(),
                comment.getCreatedAt(),
                replyCommentDTOs
        );
    }

    private ReplyCommentDTO convertReplyToDTO(ReplyComment replyComment) {
        String username = replyComment.getUser() != null ? replyComment.getUser().getUsername() : "Unknown";
        return new ReplyCommentDTO(
                replyComment.getId(),
                replyComment.getUserId(),
                username,
                replyComment.getContent(),
                replyComment.getEmotion(),
                replyComment.getFileId(),
                replyComment.getCreatedAt()
        );
    }

        public boolean isOwner(Authentication authentication, Long commentId) {
            if (authentication == null || !authentication.isAuthenticated()) return false;

            User currentUser = userService.findByUsernameOrEmail(authentication.getName());
            if (currentUser == null) return false;

            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy Comment: " + commentId));

            return comment.getUser().getId().equals(currentUser.getId());
        }

        public boolean isReplyOwner(Authentication authentication, Long replyCommentId) {
            if (authentication == null || !authentication.isAuthenticated()) return false;

            User currentUser = userService.findByUsernameOrEmail(authentication.getName());
            if (currentUser == null) return false;

            ReplyComment reply = replyCommentRepository.findById(replyCommentId)
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy Reply Comment: " + replyCommentId));

            return reply.getUser().getId().equals(currentUser.getId());
        }
}
