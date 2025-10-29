package com.tim.appTim.service;

import com.tim.appTim.dto.CommentDTO;
import com.tim.appTim.dto.ReplyCommentDTO;
import com.tim.appTim.entity.Comment;
import com.tim.appTim.entity.Post;
import com.tim.appTim.entity.ReplyComment;
import com.tim.appTim.entity.File;
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
import com.tim.appTim.repository.FileRepository;
import com.tim.appTim.dto.FileDTO;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service("commentService")
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final ReplyCommentRepository replyCommentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ReactionRepository reactionRepository;
    private final FileRepository fileRepository;
    private NotificationService notificationService;

    @Autowired
    public CommentService(CommentRepository commentRepository,
                          ReplyCommentRepository replyCommentRepository,
                          UserService userService,
                          PostRepository postRepository,
                          UserRepository userRepository,
                          NotificationService notificationService,
                          ReactionRepository reactionRepository,
                          FileRepository fileRepository) {
        this.commentRepository = commentRepository;
        this.replyCommentRepository = replyCommentRepository;
        this.userService = userService;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.reactionRepository = reactionRepository;
        this.fileRepository = fileRepository;
    }

    @Transactional
    public CommentDTO createComment(Long postId, Long userId, String content,
                                    Comment.Emotion emotion, List<File> filesFromController) { // Thêm List<File>

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(content);
        comment.setEmotion(emotion);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        if (filesFromController != null && !filesFromController.isEmpty()) {
            for (File file : filesFromController) {
                comment.addFile(file);
            }
        }

        Comment savedComment = commentRepository.save(comment);

        Integer currentTotal = post.getTotalComments();
        post.setTotalComments(currentTotal == null ? 1 : currentTotal + 1);
        postRepository.save(post);

        try {
            if (!post.getUser().getId().equals(userId)) {
                notificationService.createCommentNotification(
                        post.getUser().getId(), null, userId, user.getUsername(), "POST", postId
                );
            }
        } catch (Exception e) { System.err.println("Error creating notification: " + e.getMessage()); }

        return convertToDTO(savedComment);
    }

    public List<CommentDTO> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostId(postId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
    }

    public ReplyComment getReplyCommentById(Long replyCommentId) {
        return replyCommentRepository.findById(replyCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply comment not found with id: " + replyCommentId));
    }

    @Transactional // Thêm @Transactional nếu chưa có
    public CommentDTO updateComment(Long commentId, User currentUser, Authentication authentication, String content, Comment.Emotion emotion, List<File> newFilesFromController) {

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

        if (comment.getFiles() != null) {
            // Xóa các file cũ liên kết với comment này khỏi FileRepository
            fileRepository.deleteAll(comment.getFiles());
            comment.getFiles().clear(); // Xóa khỏi danh sách của Comment Entity
        }

        // 2. Thêm files mới
        if (newFilesFromController != null && !newFilesFromController.isEmpty()) {
            for (File file : newFilesFromController) {
                comment.addFile(file); // Dùng hàm addFile đã có trong Comment entity
            }
        }

        comment.setUpdatedAt(LocalDateTime.now()); // Sửa thành updatedAt

        Comment updatedComment = commentRepository.save(comment);
        return convertToDTO(updatedComment);
    }

    @Transactional // Thêm @Transactional nếu chưa có
    public void deleteComment(Long commentId, User currentUser, Authentication authentication) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("comment:delete_all"));
        boolean isOwner = comment.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("Bạn không có quyền xóa bình luận này");
        }

        // Cập nhật totalComments (giữ nguyên)
        Post post = comment.getPost();
        if (post != null) {
            Integer currentTotal = post.getTotalComments();
            post.setTotalComments(currentTotal == null || currentTotal <= 0 ? 0 : currentTotal - 1);
            postRepository.save(post);
        }

        commentRepository.delete(comment);
    }

    // Sửa lại hàm này, xóa hàm createReplyWithFiles
    @Transactional
    public ReplyCommentDTO createReplyComment(Long commentId, Long userId, String content,
                                              ReplyComment.Emotion emotion, List<File> filesFromController) { // Thêm List<File>

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        ReplyComment reply = new ReplyComment();
        reply.setComment(comment);
        reply.setUser(user);
        reply.setContent(content);
        reply.setEmotion(emotion);
        // reply.setFileId(fileId); // Xóa dòng này nếu File entity đã đủ
        reply.setCreatedAt(LocalDateTime.now());
        reply.setUpdatedAt(LocalDateTime.now()); // Nên thêm cả updatedAt

        // Thêm logic gán file vào reply
        if (filesFromController != null && !filesFromController.isEmpty()) {
            for (File file : filesFromController) {
                reply.addFile(file); // Dùng hàm helper trong ReplyComment entity
            }
        }

        ReplyComment savedReplyComment = replyCommentRepository.save(reply);

        // Tạo thông báo (giữ nguyên logic đã sửa)
        try {
            if (!comment.getUser().getId().equals(userId)) {
                notificationService.createCommentNotification(
                        null, comment.getUser().getId(), userId, user.getUsername(), "COMMENT", commentId
                );
            }
        } catch (Exception e) { System.err.println("Error creating notification: " + e.getMessage()); }

        return convertReplyToDTO(savedReplyComment);
    }

    public List<ReplyCommentDTO> getReplyCommentsByCommentId(Long commentId) {
        return replyCommentRepository.findByCommentId(commentId)
                .stream()
                .map(this::convertReplyToDTO)
                .collect(Collectors.toList());
    }

    public ReplyCommentDTO updateReplyComment(User currentUser, Authentication authentication, Long replyCommentId, String content, ReplyComment.Emotion emotion, List<File> newFilesFromController) {
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

        if (reply.getFiles() != null) {
            fileRepository.deleteAll(reply.getFiles());
            reply.getFiles().clear();
        }

        // 2. Thêm files mới
        if (newFilesFromController != null && !newFilesFromController.isEmpty()) {
            for (File file : newFilesFromController) {
                reply.addFile(file); // Dùng hàm addFile đã có trong ReplyComment entity
            }
        }
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

       List<FileDTO> fileDTOs = new ArrayList<>();
        if (comment.getFiles() != null && !comment.getFiles().isEmpty()) {
            fileDTOs = comment.getFiles().stream()
                    .map(file -> new FileDTO(
                            file.getId(),
                            file.getFileUrl(),
                            file.getFileType().name(),
                            file.getFileName(),
                            file.getFileSize()
                    ))
                    .collect(Collectors.toList());
        }

        // SẮP XẾP LẠI TOÀN BỘ THAM SỐ CHO ĐÚNG
        return new CommentDTO(
                comment.getId(),            // 1. Long id
                comment.getUser().getId(),   // 2. Long userId
                username,                   // 3. String username
                comment.getContent(),       // 4. String content
                userAvatar,                 // 5. String userAvatar
                emotionName,                // 6. String emotion
//                comment.getFileId(),        // 7. Long fileId
                comment.getCreatedAt(),     // 8. LocalDateTime createdAt
                replies,
                fileDTOs// 9. List<ReplyCommentDTO> replyComments
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
                emotionName,
//                reply.getFileId(),
                reply.getCreatedAt(),
                reply.getUser() != null ? reply.getUser().getProfileImage() : " ",
                reply.getFiles() != null ? reply.getFiles().stream()
                        .map(file -> new FileDTO(
                                file.getId(),
                                file.getFileUrl(),
                                file.getFileType().name(),
                                file.getFileName(),
                                file.getFileSize()
                        ))
                        .collect(Collectors.toList()) : new ArrayList<>()
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
