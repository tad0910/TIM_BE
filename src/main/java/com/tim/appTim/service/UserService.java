package com.tim.appTim.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set; // *** THÊM IMPORT NÀY ***
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication; // *** THÊM IMPORT NÀY ***
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt; // *** THÊM IMPORT NÀY ***
import org.springframework.stereotype.Service;

import com.tim.appTim.dto.CommentDTO;
import com.tim.appTim.dto.CourseDTO;
import com.tim.appTim.dto.PostDTO;
import com.tim.appTim.dto.ProfileResponse;
import com.tim.appTim.dto.ReactionDTO;
import com.tim.appTim.dto.ReplyCommentDTO;
import com.tim.appTim.dto.UserImageDTO;
import com.tim.appTim.entity.File;
import com.tim.appTim.entity.Role; // *** THÊM IMPORT NÀY ***
import com.tim.appTim.entity.User;
import com.tim.appTim.entity.UserImage;
import com.tim.appTim.repository.ClassMemberRepository;
import com.tim.appTim.repository.CommentRepository;
import com.tim.appTim.repository.CourseRepository;
import com.tim.appTim.repository.PostRepository;
import com.tim.appTim.repository.ReactionRepository;
import com.tim.appTim.repository.ReplyCommentRepository;
import com.tim.appTim.repository.RoleRepository; // *** THÊM IMPORT NÀY ***
import com.tim.appTim.repository.UserImageRepository;
import com.tim.appTim.repository.UserRepository;
import com.tim.appTim.repository.FileRepository;


@Service("userService") // <-- THAY ĐỔI 1: Đặt tên cho bean
public class UserService implements UserDetailsService {

    @Value("${upload.folder}")
    private String uploadDir;

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReplyCommentRepository replyCommentRepository;
    private final ReactionRepository reactionRepository;
    private final UserImageRepository userImageRepository;
    private final ClassMemberRepository classMemberRepository;
    private final CourseRepository courseRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final FileRepository fileRepository;
    private final RoleRepository roleRepository; // *** THÊM REPO NÀY ***


    public UserService(UserRepository userRepository, PostRepository postRepository, CommentRepository commentRepository,
                       ReplyCommentRepository replyCommentRepository, ReactionRepository reactionRepository,
                       UserImageRepository userImageRepository, ClassMemberRepository classMemberRepository,
                       CourseRepository courseRepository,@Lazy BCryptPasswordEncoder passwordEncoder, FileRepository fileRepository,
                       RoleRepository roleRepository // *** THÊM VÀO CONSTRUCTOR ***
    ) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.replyCommentRepository = replyCommentRepository;
        this.reactionRepository = reactionRepository;
        this.userImageRepository = userImageRepository;
        this.classMemberRepository = classMemberRepository;
        this.courseRepository = courseRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileRepository = fileRepository;
        this.roleRepository = roleRepository; // *** THÊM VÀO CONSTRUCTOR ***
    }

    // *** THAY ĐỔI 2: SỬA LẠI HOÀN TOÀN PHƯƠNG THỨC 'register' ***
    public void register(User user) {
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        // --- BẮT ĐẦU LOGIC MỚI ---
        // Gán vai trò mặc định cho user mới
        // Đảm bảo bạn đã có "ROLE_SINH_VIEN" trong bảng 'roles' của DB
        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Lỗi: Role 'ROLE_USER' không tồn tại trong DB."));

        user.setRoles(Set.of(defaultRole));
        // --- KẾT THÚC LOGIC MỚI ---

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        return userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .map(UserDetailsImpl::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail));
    }

    // ... (Tất cả các phương thức khác của bạn: findById, create, update, delete, ... ) ...
    // ... (getUserProfileByEmail, getUserProfile, ... ) ...
    // ... (Tất cả các phương thức xử lý ảnh ... ) ...
    // ... (Giữ nguyên không thay đổi) ...

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public User create(User user) {
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User update(Long id, User user) {
        User existingUser = findById(id);
        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail());
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setPhoneNumber(user.getPhoneNumber());

        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(user.getPassword());
        }
        return userRepository.save(existingUser);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    public java.util.List<User> findAll() {
        return userRepository.findAll();
    }

    public User findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElse(null);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public ProfileResponse getUserProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        List<PostDTO> posts = postRepository.findByUserId(user.getId()).stream().map(post -> {
            List<CommentDTO> comments = commentRepository.findByPostId(post.getId()).stream().map(comment -> {
                List<ReplyCommentDTO> replyComments = replyCommentRepository.findByCommentId(comment.getId()).stream()
                        .map(reply -> new ReplyCommentDTO(
                                reply.getId(),
                                reply.getUserId(),
                                reply.getUser() != null ? reply.getUser().getUsername() : "Unknown",
                                reply.getContent(),
                                reply.getEmotion(),
                                reply.getFileId(),
                                reply.getCreatedAt()
                        ))
                        .collect(Collectors.toList());
                return new CommentDTO(
                        comment.getId(),
                        comment.getUserId(),
                        comment.getUser().getUsername(),
                        comment.getContent(),
                        comment.getEmotion() != null ? comment.getEmotion().name() : null,
                        comment.getFileId(),
                        comment.getCreatedAt(),
                        replyComments
                );
            }).collect(Collectors.toList());

            List<ReactionDTO> reactions = reactionRepository.findByPostId(post.getId()).stream()
                    .map(reaction -> new ReactionDTO(
                            reaction.getId(),
                            reaction.getUserId(),
                            reaction.getUser().getUsername(),
                            reaction.getEmotionType() != null ? reaction.getEmotionType().name() : null,
                            reaction.getCreatedAt()))
                    .collect(Collectors.toList());

                    // Convert files to FileDTO
            // Convert files to FileDTO
            List<com.tim.appTim.dto.FileDTO> fileDTOs = post.getFiles().stream()
                    .map(file -> new com.tim.appTim.dto.FileDTO(
                            file.getId(),
                            file.getFileUrl(),
                            file.getFileType().name(),
                            file.getFileName() != null ? file.getFileName() : extractFileName(file.getFileUrl()),
                            file.getFileSize() != null ? file.getFileSize() : 0L
                    ))
                    .collect(Collectors.toList());
                    
            return new PostDTO(
                    post.getId(),
                    post.getUser().getId(),
                    post.getContent(),
                    post.getPrivacy() != null ? post.getPrivacy().name() : null,
                    post.getCreatedAt(),
                    post.getUpdatedAt(),
                    comments,
                    reactions,
                    fileDTOs
            );
        }).collect(Collectors.toList());

        List<UserImageDTO> images = userImageRepository.findByUserId(user.getId()).stream()
                .map(image -> new UserImageDTO(image.getId(), image.getImageUrl(), image.getDescription(), image.getCreatedAt()))
                .collect(Collectors.toList());

        List<CourseDTO> courses = classMemberRepository.findByClassId(user.getId()).stream()
                .map(classMember -> courseRepository.findById(classMember.getClassId())
                        .map(course -> new CourseDTO(course.getId(), course.getCourseName(), course.getDescription(),
                                course.getStartDate(), course.getTuitionFee()))
                        .orElse(null))
                .filter(course -> course != null)
                .collect(Collectors.toList());

        return new ProfileResponse(user, posts, images, courses);
    }


    public ProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        List<PostDTO> posts = postRepository.findByUserId(userId).stream().map(post -> {
            List<CommentDTO> comments = commentRepository.findByPostId(post.getId()).stream().map(comment -> {
                List<ReplyCommentDTO> replyComments = replyCommentRepository.findByCommentId(comment.getId()).stream()
                        .map(reply -> new ReplyCommentDTO(
                                reply.getId(),
                                reply.getUserId(),
                                reply.getUser() != null ? reply.getUser().getUsername() : "Unknown",
                                reply.getContent(),
                                reply.getEmotion(),
                                reply.getFileId(),
                                reply.getCreatedAt()
                        ))
                        .collect(Collectors.toList());
                return new CommentDTO(comment.getId(), comment.getUserId(), comment.getUser().getUsername(), // Sửa ở đây
                        comment.getContent(), comment.getEmotion() != null ? comment.getEmotion().name() : null,
                        comment.getFileId(), comment.getCreatedAt(), replyComments);
            }).collect(Collectors.toList());

            List<ReactionDTO> reactions = reactionRepository.findByPostId(post.getId()).stream()
                    .map(reaction -> new ReactionDTO(reaction.getId(), reaction.getUserId(), reaction.getUser().getUsername(), // Sửa ở đây
                            reaction.getEmotionType() != null ? reaction.getEmotionType().name() : null,
                            reaction.getCreatedAt()))
                    .collect(Collectors.toList());

        List<com.tim.appTim.dto.FileDTO> fileDTOs = post.getFiles().stream()
                .map(file -> new com.tim.appTim.dto.FileDTO(
                        file.getId(),
                        file.getFileUrl(),
                        file.getFileType().name(),
                        file.getFileName() != null ? file.getFileName() : extractFileName(file.getFileUrl()),
                        file.getFileSize() != null ? file.getFileSize() : 0L
                ))
                .collect(Collectors.toList());
                return new PostDTO(
                    post.getId(),
                    post.getUser().getId(),
                    post.getContent(),
                    post.getPrivacy() != null ? post.getPrivacy().name() : null,
                    post.getCreatedAt(),
                    post.getUpdatedAt(),
                    comments,
                    reactions,
                    fileDTOs
            );
        }).collect(Collectors.toList());

        List<UserImageDTO> images = userImageRepository.findByUserId(userId).stream()
                .map(image -> new UserImageDTO(image.getId(), image.getImageUrl(), image.getDescription(), image.getCreatedAt()))
                .collect(Collectors.toList());

        List<CourseDTO> courses = classMemberRepository.findByClassId(userId).stream()
                .map(classMember -> courseRepository.findById(classMember.getClassId())
                        .map(course -> new CourseDTO(course.getId(), course.getCourseName(), course.getDescription(),
                                course.getStartDate(), course.getTuitionFee()))
                        .orElse(null))
                .filter(course -> course != null)
                .collect(Collectors.toList());

        return new ProfileResponse(user, posts, images, courses);
    }

    public List<UserImageDTO> getUserImages(Long userId) {
        return userImageRepository.findByUserId(userId)
                .stream()
                .map(image -> new UserImageDTO(
                        image.getId(),
                        image.getImageUrl(),
                        image.getDescription(),
                        image.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    public UserImageDTO createUserImage(Long userId, String imageUrl, String description) {
        UserImage image = new UserImage();
        image.setUser(userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found")));
        image.setImageUrl(imageUrl);
        image.setDescription(description);
        image.setCreatedAt( LocalDateTime.now());

        UserImage saved = userImageRepository.save(image);
        return convertToDTO(saved);
    }

    public UserImageDTO updateUserImage(Long userId, Long imageId, String imageUrl, String description) {
        UserImage image = userImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));


        if (!image.getUser().getId().equals(userId)) {
            throw new SecurityException("You can only update your own images");
        }

        if (imageUrl != null) image.setImageUrl(imageUrl);
        if (description != null) image.setDescription(description);
        image.setCreatedAt( LocalDateTime.now());

        UserImage saved = userImageRepository.save(image);
        return convertToDTO(saved);
    }


    public void deleteUserImage(Long userId, Long imageId) {
        UserImage image = userImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        if (!image.getUser().getId().equals(userId)) {
            throw new SecurityException("You can only delete your own images");
        }

        userImageRepository.delete(image);
    }

    private UserImageDTO convertToDTO(UserImage image) {
        return new UserImageDTO(
                image.getId(),
                image.getImageUrl(),
                image.getDescription(),
                image.getCreatedAt()
        );
    }

    public User updateProfileImage(Long userId, String imageUrl) {
        User user = findById(userId);

        if (user.getProfileImage() != null && !user.getProfileImage().trim().isEmpty()) {
            deleteOldProfileImage(userId, user.getProfileImage());
        }

        user.setProfileImage(imageUrl);
        return userRepository.save(user);
    }

    public User updateCoverImage(Long userId, String imageUrl) {
        User user = findById(userId);

        if (user.getCoverImage() != null && !user.getCoverImage().trim().isEmpty()) {
            deleteOldCoverImage(userId, user.getCoverImage());
        }

        user.setCoverImage(imageUrl);
        return userRepository.save(user);
    }

    private void deleteOldProfileImage(Long userId, String oldImageUrl) {
        try {
            List<UserImage> oldImages = userImageRepository.findByUserId(userId);
            for (UserImage image : oldImages) {
                if (oldImageUrl.equals(image.getImageUrl())) {
                    userImageRepository.delete(image);
                    break;
                }
            }

            if (oldImageUrl != null && oldImageUrl.startsWith("/uploads/")) {
                String filename = oldImageUrl.substring("/uploads/".length());
                java.io.File file = new java.io.File(uploadDir + java.io.File.separator + filename);
                if (file.exists()) {
                    file.delete();
                }
            }
        } catch (Exception e) {
            System.err.println("Error deleting old profile image: " + e.getMessage());
        }
    }

    private void deleteOldCoverImage(Long userId, String oldImageUrl) {
        try {
            List<UserImage> oldImages = userImageRepository.findByUserId(userId);
            for (UserImage image : oldImages) {
                if (oldImageUrl.equals(image.getImageUrl())) {
                    userImageRepository.delete(image);
                    break;
                }
            }

            if (oldImageUrl != null && oldImageUrl.startsWith("/uploads/")) {
                String filename = oldImageUrl.substring("/uploads/".length());
                java.io.File file = new java.io.File(uploadDir + java.io.File.separator + filename);
                if (file.exists()) {
                    file.delete();
                }
            }
        } catch (Exception e) {
            System.err.println("Error deleting old cover image: " + e.getMessage());
        }
    }

    public User save(User user) {
        return userRepository.save(user);
    }
    public boolean isSelf(Authentication authentication, Long id) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String currentUsername = "";
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            currentUsername = ((UserDetails) principal).getUsername();
        } else if (principal instanceof Jwt) {
            currentUsername = ((Jwt) principal).getClaimAsString("preferred_username");
            if (currentUsername == null) {
                currentUsername = ((Jwt) principal).getSubject();
            }
        } else {
            currentUsername = principal.toString();
        }

        if (currentUsername == null) {
            return false;
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        return user.getUsername().equals(currentUsername);
    }
    private String extractFileName(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return "unknown";
        }
        String[] parts = fileUrl.split("/");
        return parts[parts.length - 1];
    }
}