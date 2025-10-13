package com.tim.appTim.service;

import com.tim.appTim.dto.CommentDTO;
import com.tim.appTim.dto.ReplyCommentDTO;
import com.tim.appTim.entity.Comment;
import com.tim.appTim.entity.ReplyComment;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.CommentRepository;
import com.tim.appTim.repository.PostRepository;
import com.tim.appTim.repository.ReplyCommentRepository;
import com.tim.appTim.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ReplyCommentRepository replyCommentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    // Tạo comment mới
    public CommentDTO createComment(Long postId, Long userId, String content, Comment.Emotion emotion, Long fileId) {
        // Kiểm tra post tồn tại
        postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
        
        // Kiểm tra user tồn tại
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

    // Lấy tất cả comments của một post
    public List<CommentDTO> getCommentsByPostId(Long postId) {
        List<Comment> comments = commentRepository.findByPostId(postId);
        return comments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Cập nhật comment
    public CommentDTO updateComment(Long commentId, Long userId, String content, Comment.Emotion emotion) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        // Kiểm tra quyền sở hữu
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("You don't have permission to update this comment");
        }

        comment.setContent(content);
        comment.setEmotion(emotion);
        comment.setCreatedAt(LocalDateTime.now());

        Comment updatedComment = commentRepository.save(comment);
        return convertToDTO(updatedComment);
    }

    // Xóa comment
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        // Kiểm tra quyền sở hữu
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("You don't have permission to delete this comment");
        }

        // Xóa tất cả reply comments trước
        List<ReplyComment> replyComments = replyCommentRepository.findByCommentId(commentId);
        replyCommentRepository.deleteAll(replyComments);

        // Xóa comment
        commentRepository.delete(comment);
    }

    // Tạo reply comment
    public ReplyCommentDTO createReplyComment(Long commentId, Long userId, String content, ReplyComment.Emotion emotion, Long fileId) {
        // Kiểm tra comment tồn tại
        commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        // Kiểm tra user tồn tại
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

    // Lấy tất cả reply comments của một comment
    public List<ReplyCommentDTO> getReplyCommentsByCommentId(Long commentId) {
        List<ReplyComment> replyComments = replyCommentRepository.findByCommentId(commentId);
        return replyComments.stream()
                .map(this::convertReplyToDTO)
                .collect(Collectors.toList());
    }

    // Cập nhật reply comment
    public ReplyCommentDTO updateReplyComment(Long replyCommentId, String content, ReplyComment.Emotion emotion) {
        ReplyComment replyComment = replyCommentRepository.findById(replyCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply comment not found with id: " + replyCommentId));

        replyComment.setContent(content);
        replyComment.setEmotion(emotion);
        replyComment.setCreatedAt(LocalDateTime.now());

        ReplyComment updatedReplyComment = replyCommentRepository.save(replyComment);
        return convertReplyToDTO(updatedReplyComment);
    }

    // Xóa reply comment
    public void deleteReplyComment(Long replyCommentId) {
        ReplyComment replyComment = replyCommentRepository.findById(replyCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply comment not found with id: " + replyCommentId));

        replyCommentRepository.delete(replyComment);
    }

    // Convert Comment entity to DTO
    private CommentDTO convertToDTO(Comment comment) {
        // Lấy username từ user
        String username = comment.getUser() != null ? comment.getUser().getUsername() : "Unknown";
        
        // Lấy reply comments
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

    // Convert ReplyComment entity to DTO
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
}
