package com.tim.appTim.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set; 
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication; 
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt; 
import org.springframework.stereotype.Service;

import com.tim.appTim.dto.CommentDTO;
import com.tim.appTim.dto.ProgramsDTO;
import com.tim.appTim.repository.ProgramsRepository;
import com.tim.appTim.repository.ProgramModuleRepository;
import com.tim.appTim.repository.ClassRepository;
import com.tim.appTim.entity.Programs;
import com.tim.appTim.entity.ProgramModule;
import com.tim.appTim.dto.FileDTO;
import com.tim.appTim.dto.LinkPreviewDTO;
import com.tim.appTim.dto.PostDTO;
import com.tim.appTim.dto.ProfileResponse;
import com.tim.appTim.dto.ReactionDTO;
import com.tim.appTim.dto.ReplyCommentDTO;
import com.tim.appTim.dto.UserImageDTO;
import com.tim.appTim.entity.ClassMember;
import com.tim.appTim.entity.Role;
import com.tim.appTim.entity.User;
import com.tim.appTim.entity.UserImage;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.exception.ConflictException;
import com.tim.appTim.exception.InternalServerErrorException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.exception.UnauthorizedException;
import com.tim.appTim.exception.UnprocessableException;
import com.tim.appTim.repository.ClassMemberRepository;
import com.tim.appTim.repository.CommentRepository;
import com.tim.appTim.repository.PostRepository;
import com.tim.appTim.repository.ReactionRepository;
import com.tim.appTim.repository.ReplyCommentRepository;
import com.tim.appTim.repository.RoleRepository; 
import com.tim.appTim.repository.UserImageRepository;
import com.tim.appTim.repository.UserRepository;
import com.tim.appTim.repository.FileRepository;


@Service("userService") 
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
    private final ProgramsRepository programsRepository;
    private final ProgramModuleRepository programModuleRepository;
    private final ClassRepository classRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final FileRepository fileRepository;
    private final RoleRepository roleRepository; 
    

    public UserService(UserRepository userRepository, PostRepository postRepository, CommentRepository commentRepository,
                       ReplyCommentRepository replyCommentRepository, ReactionRepository reactionRepository,
                       UserImageRepository userImageRepository, ClassMemberRepository classMemberRepository,
                       ProgramsRepository programsRepository, ProgramModuleRepository programModuleRepository,
                       ClassRepository classRepository, @Lazy BCryptPasswordEncoder passwordEncoder, FileRepository fileRepository,
                       RoleRepository roleRepository 
    ) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.replyCommentRepository = replyCommentRepository;
        this.reactionRepository = reactionRepository;
        this.userImageRepository = userImageRepository;
        this.classMemberRepository = classMemberRepository;
        this.programsRepository = programsRepository;
        this.programModuleRepository = programModuleRepository;
        this.classRepository = classRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileRepository = fileRepository;
        this.roleRepository = roleRepository; 
    }

    public void register(User user) {
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            throw new UnprocessableException("Username is required");
        }
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new ConflictException("Username already exists");
        }

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new UnprocessableException("Email is required");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ConflictException("Email already exists");
        }

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new UnprocessableException("Password is required");
        }

        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException("Lỗi: Role 'ROLE_USER' không tồn tại trong DB."));

        user.setRoles(Set.of(defaultRole));
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


    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
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
        if (existingUser == null) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id);
        }

        if (!existingUser.getEmail().equals(user.getEmail())
                && userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new ConflictException("Email đã tồn tại");
        }

        if (!existingUser.getUsername().equals(user.getUsername())
                && userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new ConflictException("Username đã tồn tại");
        }

        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail());
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setPhoneNumber(user.getPhoneNumber());

        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(user.getPassword());
        }

        try {
            return userRepository.save(existingUser);
        } catch (Exception e) {
            throw new InternalServerErrorException("Không thể cập nhật thông tin người dùng: " + e.getMessage());
        }
    }

    public void delete(Long id) {
        User existingUser = findById(id);
        if (existingUser == null) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id);
        }

        try {
            userRepository.deleteById(id);
        } catch (Exception e) {
            throw new InternalServerErrorException("Không thể xóa người dùng: " + e.getMessage());
        }
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
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        List<PostDTO> posts = postRepository.findByUserId(user.getId()).stream().map(post -> {
            List<CommentDTO> comments = commentRepository.findByPostId(post.getId()).stream().map(comment -> {
                List<ReplyCommentDTO> replyComments = replyCommentRepository.findByCommentId(comment.getId()).stream()
                        .map(reply -> { 
                            String emotionName = reply.getEmotion() != null ? reply.getEmotion().name() : null; 
                            return new ReplyCommentDTO(
                                    reply.getId(),
                                    reply.getComment().getId(),
                                    reply.getUser().getId(),
                                    reply.getUser() != null ? reply.getUser().getUsername() : "Unknown",
                                    reply.getContent(),
                                    emotionName, 
                                    // reply.getFileId(),
                                    reply.getCreatedAt(),
                                    reply.getUser() != null ? reply.getUser().getProfileImage() : " ",
                                    reply.getFiles() != null ? reply.getFiles().stream()
                                            .map(file -> new FileDTO(/*...*/))
                                            .collect(Collectors.toList()) : new java.util.ArrayList<FileDTO>()
                            );
                        })
                        .collect(Collectors.toList());
                return new CommentDTO(
                        comment.getId(),
                        comment.getUser().getId(),
                        comment.getUser().getUsername(),
                        comment.getContent(),
                        comment.getUser().getProfileImage(),
                        comment.getEmotion() != null ? comment.getEmotion().name() : null,
                        // comment.getFileId(), 
                        comment.getCreatedAt(),
                        replyComments,
                        comment.getFiles() != null ? comment.getFiles().stream()
                                .map(file -> new FileDTO(/*...*/))
                                .collect(Collectors.toList()) : new java.util.ArrayList<FileDTO>()
                );
            }).collect(Collectors.toList());

            List<ReactionDTO> reactions = reactionRepository.findByPostAndCommentIsNullAndReplyCommentIsNull(post)
                    .stream()
                    .map(reaction -> new ReactionDTO(
                            reaction.getId(),
                            reaction.getUser().getId(), 
                            reaction.getUser().getUsername(),
                            reaction.getUser().getProfileImage(),
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
                    reactions != null ? reactions.size() : 0,
                    comments != null ? comments.size() : 0,
                    comments,
                    reactions,
                    fileDTOs,
                    post.getUser().getProfileImage(),
                    post.getUser().getUsername(),
                    getUserDisplayName(post.getUser()),
                    post.hasLinkPreview() ? new LinkPreviewDTO(
                            post.getLinkUrl(),
                            post.getLinkTitle(),
                            post.getLinkDescription(),
                            post.getLinkImageUrl(),
                            post.getLinkDomain()
                    ) : null
                                        
            );

        }).collect(Collectors.toList());

        List<UserImageDTO> images = userImageRepository.findByUserId(user.getId()).stream()
                .map(image -> new UserImageDTO(image.getId(), image.getImageUrl(), image.getDescription(), image.getCreatedAt()))
                .collect(Collectors.toList());

        List<ProgramsDTO> programs = getUserPrograms(user.getId());

        return new ProfileResponse(user, posts, images, programs);
    }


    public ProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<PostDTO> posts = postRepository.findByUserId(userId).stream().map(post -> {
            List<CommentDTO> comments = commentRepository.findByPostId(post.getId()).stream().map(comment -> {
                List<ReplyCommentDTO> replyComments = replyCommentRepository.findByCommentId(comment.getId()).stream()
                        .map(reply -> {
                            String emotionName = reply.getEmotion() != null ? reply.getEmotion().name() : null; 
                            return new ReplyCommentDTO(
                                    reply.getId(),
                                    reply.getComment().getId(),
                                    reply.getUser().getId(),
                                    reply.getUser() != null ? reply.getUser().getUsername() : "Unknown",
                                    reply.getContent(),
                                    emotionName, 
                                    // reply.getFileId(), 
                                    reply.getCreatedAt(),
                                    reply.getUser() != null ? reply.getUser().getProfileImage() : " ",
                                    reply.getFiles() != null ? reply.getFiles().stream()
                                            .map(file -> new FileDTO(/*...*/))
                                            .collect(Collectors.toList()) : new java.util.ArrayList<FileDTO>()
                            );
                        })
                        .collect(Collectors.toList());
                return new CommentDTO(
                        comment.getId(),
                        comment.getUser().getId(),
                        comment.getUser().getUsername(),
                        comment.getContent(),
                        comment.getUser().getProfileImage(),
                        comment.getEmotion() != null ? comment.getEmotion().name() : null,
                        // comment.getFileId(), 
                        comment.getCreatedAt(),
                        replyComments,
                        comment.getFiles() != null ? comment.getFiles().stream()
                                .map(file -> new FileDTO(/*...*/))
                                .collect(Collectors.toList()) : new java.util.ArrayList<FileDTO>()
                );
            }).collect(Collectors.toList());

            List<ReactionDTO> reactions = reactionRepository.findByPostAndCommentIsNullAndReplyCommentIsNull(post)
                    .stream()
                    .map(reaction -> new ReactionDTO(
                            reaction.getId(),
                            reaction.getUser().getId(), 
                            reaction.getUser().getUsername(),
                            reaction.getUser().getProfileImage(),
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
                    reactions != null ? reactions.size() : 0,
                    comments != null ? comments.size() : 0,
                    comments,
                    reactions,
                    fileDTOs,
                    post.getUser().getProfileImage(),
                    post.getUser().getUsername(),
                    getUserDisplayName(post.getUser()),
                    post.hasLinkPreview() ? new LinkPreviewDTO(
                            post.getLinkUrl(),
                            post.getLinkTitle(),
                            post.getLinkDescription(),
                            post.getLinkImageUrl(),
                            post.getLinkDomain()
                    ) : null
                    
            );

        }).collect(Collectors.toList());

        List<UserImageDTO> images = userImageRepository.findByUserId(userId).stream()
                .map(image -> new UserImageDTO(image.getId(), image.getImageUrl(), image.getDescription(), image.getCreatedAt()))
                .collect(Collectors.toList());

        List<ProgramsDTO> programs = getUserPrograms(userId);

        return new ProfileResponse(user, posts, images, programs);
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
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
        image.setImageUrl(imageUrl);
        image.setDescription(description);
        image.setCreatedAt( LocalDateTime.now());

        UserImage saved = userImageRepository.save(image);
        return convertToDTO(saved);
    }

    public UserImageDTO updateUserImage(Long userId, Long imageId, String imageUrl, String description) {
        UserImage image = userImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));


        if (!image.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only update your own images");
        }

        if (imageUrl != null) image.setImageUrl(imageUrl);
        if (description != null) image.setDescription(description);
        image.setCreatedAt( LocalDateTime.now());

        UserImage saved = userImageRepository.save(image);
        return convertToDTO(saved);
    }


    public void deleteUserImage(Long userId, Long imageId) {
        UserImage image = userImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        if (!image.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own images");
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
        if (user == null) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId);
        }

        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new BadRequestException("URL ảnh đại diện không hợp lệ");
        }

        try {

            if (user.getProfileImage() != null && !user.getProfileImage().trim().isEmpty()) {
                deleteOldProfileImage(userId, user.getProfileImage());
            }

            user.setProfileImage(imageUrl);
            return userRepository.save(user);

        } catch (ConflictException e) {
            throw e;
        } catch (Exception e) {
            throw new UnprocessableException("Không thể cập nhật ảnh đại diện: " + e.getMessage());
        }
    }

    public User updateCoverImage(Long userId, String imageUrl) {
        User user = findById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId);
        }

        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new BadRequestException("URL ảnh bìa không hợp lệ");
        }

        try {
            if (user.getCoverImage() != null && !user.getCoverImage().trim().isEmpty()) {
                deleteOldCoverImage(userId, user.getCoverImage());
            }

            user.setCoverImage(imageUrl);
            return userRepository.save(user);

        } catch (ConflictException e) {
            throw e;
        } catch (Exception e) {
            throw new UnprocessableException("Không thể cập nhật ảnh bìa: " + e.getMessage());
        }
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
                if (file.exists() && !file.delete()) {
                    throw new InternalServerErrorException("Không thể xóa file ảnh cũ: " + filename);
                }
            }
        } catch (Exception e) {
            throw new InternalServerErrorException("Lỗi khi xóa ảnh đại diện cũ: " + e.getMessage());
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
                if (file.exists() && !file.delete()) {
                    throw new InternalServerErrorException("Không thể xóa file ảnh cũ: " + filename);
                }
            }
        } catch (Exception e) {
            throw new InternalServerErrorException("Lỗi khi xóa ảnh bìa cũ: " + e.getMessage());
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
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        return user.getUsername().equals(currentUsername);
    }
    private String extractFileName(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return "unknown";
        }
        String[] parts = fileUrl.split("/");
        return parts[parts.length - 1];
    }

    private String getUserDisplayName(User user) {
        if (user == null) {
            return "Người dùng";
        }
        
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String username = user.getUsername();
        
        if (firstName != null && !firstName.trim().isEmpty() && 
            lastName != null && !lastName.trim().isEmpty()) {
            return firstName + " " + lastName;
        }
        
        if (firstName != null && !firstName.trim().isEmpty()) {
            return firstName;
        }
        
        if (lastName != null && !lastName.trim().isEmpty()) {
            return lastName;
        }
        
        if (username != null && !username.trim().isEmpty()) {
            return username;
        }
        
        return "Người dùng";
    }


    private List<ProgramsDTO> getUserPrograms(Long userId) {
        List<ClassMember> classMembers = classMemberRepository.findByUserId(userId);

        Set<Long> classIds = classMembers.stream()
                .map(ClassMember::getClassId)
                .collect(Collectors.toSet());

        Set<Integer> programIds = new HashSet<>();
        
        for (Long classId : classIds) {
            classRepository.findById(classId).ifPresent(classEntity -> {
                if (classEntity.getSchedules() != null) {
                    classEntity.getSchedules().forEach(schedule -> {
                        if (schedule.getModuleId() != null) {
                            List<ProgramModule> programModules = programModuleRepository
                                    .findByModuleId(schedule.getModuleId().intValue());
                            programModules.forEach(pm -> {
                                if (pm.getProgram() != null && pm.getProgram().getId() != null) {
                                    programIds.add(pm.getProgram().getId());
                                }
                            });
                        }
                    });
                }
            });
        }
        
        return programIds.stream()
                .map(id -> programsRepository.findById(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::programToDTO)
                .collect(Collectors.toList());
    }

    private ProgramsDTO programToDTO(Programs program) {
        ProgramsDTO dto = new ProgramsDTO();
        dto.setId(program.getId());
        dto.setName(program.getName());
        dto.setDescription(program.getDescription());
        return dto;
    }
}