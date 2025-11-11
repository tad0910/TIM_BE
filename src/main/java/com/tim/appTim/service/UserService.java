package com.tim.appTim.service;
    
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set; 
import java.util.stream.Collectors;

import com.tim.appTim.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication; 
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt; 
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import com.tim.appTim.repository.ProgramsRepository;
import com.tim.appTim.repository.ProgramModuleRepository;
import com.tim.appTim.repository.ClassRepository;
import com.tim.appTim.entity.Programs;
import com.tim.appTim.entity.ProgramModule;
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
    private final PostService postService;
    

    public UserService(UserRepository userRepository, PostRepository postRepository, CommentRepository commentRepository,
                       ReplyCommentRepository replyCommentRepository, ReactionRepository reactionRepository,
                       UserImageRepository userImageRepository, ClassMemberRepository classMemberRepository,
                       ProgramsRepository programsRepository, ProgramModuleRepository programModuleRepository,
                       ClassRepository classRepository, @Lazy BCryptPasswordEncoder passwordEncoder, FileRepository fileRepository,
                       RoleRepository roleRepository, PostService postService
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
        this.postService = postService;
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

        user.setRoles(new HashSet<>(Collections.singleton(defaultRole)));
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail));
            return new UserDetailsImpl(user);
    }


    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public User create(User user) {
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

        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
            if (!user.getPhoneNumber().matches("^[0-9]*$")) {
                throw new UnprocessableException("Số điện thoại chỉ được chứa số");
            }
            if (user.getPhoneNumber().length() > 11) {
                throw new UnprocessableException("Số điện thoại không được quá 11 số");
            }

            if (userRepository.existsByPhoneNumber(user.getPhoneNumber())) {
                throw new ConflictException("Số điện thoại đã được sử dụng");
            }
        }

        Role selectedRole;
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            if (user.getRoles().size() > 1) {
                throw new UnprocessableException("Chỉ được chọn một role duy nhất");
            }
            selectedRole = user.getRoles().iterator().next();
        } else {
            selectedRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new ResourceNotFoundException("Lỗi: Role 'ROLE_USER' không tồn tại trong DB."));
        }
        user.setRoles(new HashSet<>(Collections.singleton(selectedRole)));
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
        return userRepository.save(user);
    }

    public User update(Long id, UserUpdateDTO userDTO) {
        User existingUser = findById(id);

        if (userDTO.getUsername() == null || userDTO.getUsername().isEmpty()) {
            throw new UnprocessableException("Username is required");
        }
        if (!existingUser.getUsername().equals(userDTO.getUsername())) {
            if (userRepository.existsByUsername(userDTO.getUsername())) {
                throw new ConflictException("Username already exists");
            }
            existingUser.setUsername(userDTO.getUsername());
        }

        if (userDTO.getPhoneNumber() != null && !userDTO.getPhoneNumber().isEmpty()) {
            if (!userDTO.getPhoneNumber().matches("^[0-9]*$")) {
                throw new UnprocessableException("Số điện thoại chỉ được chứa số");
            }
            if (userDTO.getPhoneNumber().length() > 11) {
                throw new UnprocessableException("Số điện thoại không được quá 11 số");
            }

            String currentPhoneNumber = existingUser.getPhoneNumber();
            if (currentPhoneNumber == null || !currentPhoneNumber.equals(userDTO.getPhoneNumber())) {
                if (userRepository.existsByPhoneNumber(userDTO.getPhoneNumber())) {

                    Optional<User> userWithPhone = userRepository.findByPhoneNumber(userDTO.getPhoneNumber());
                    if (userWithPhone.isPresent() && !userWithPhone.get().getId().equals(id)) {
                        throw new ConflictException("Số điện thoại đã được sử dụng bởi người dùng khác");
                    }
                }
            }
        }

        existingUser.setFirstName(userDTO.getFirstName());
        existingUser.setLastName(userDTO.getLastName());
        existingUser.setPhoneNumber(userDTO.getPhoneNumber());

        if (userDTO.getRole() != null && !userDTO.getRole().isEmpty()) {
            Role role = roleRepository.findByName(userDTO.getRole())
                    .orElseThrow(() -> new ResourceNotFoundException("Role không tồn tại: " + userDTO.getRole()));
            existingUser.setRoles(new HashSet<>(Collections.singleton(role)));
        } else {

            if (existingUser.getRoles() == null || existingUser.getRoles().isEmpty()) {
                Role defaultRole = roleRepository.findByName("ROLE_USER")
                        .orElseThrow(() -> new ResourceNotFoundException("Lỗi: Role 'ROLE_USER' không tồn tại trong DB."));
                existingUser.setRoles(new HashSet<>(Collections.singleton(defaultRole)));
            }
        }

        try {
            return userRepository.save(existingUser);
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = e.getClass().getSimpleName() + ": " + (e.getCause() != null ? e.getCause().getMessage() : "Unknown error");
            }
            e.printStackTrace(); 
            throw new InternalServerErrorException("Không thể cập nhật thông tin người dùng: " + errorMessage);
        }
    }

    public User internalSave(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User hoặc User ID không được null khi lưu nội bộ");
        }
        return userRepository.save(user);
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


    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElse(null);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public ProfileResponse getUserProfileByEmail(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (!pageable.getSort().isSorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by("createdAt").descending()
            );
        }

        Page<PostDTO> postsPage = postService.getPostsByUserId(user.getId(), pageable);

        List<UserImageDTO> images = userImageRepository.findByUserId(user.getId()).stream()
                .map(image -> new UserImageDTO(image.getId(), image.getImageUrl(), image.getDescription(), image.getCreatedAt()))
                .collect(Collectors.toList());

        List<ProgramsDTO> programs = getUserPrograms(user.getId());

        return new ProfileResponse(user, postsPage, images, programs);
    }



    public ProfileResponse getUserProfile(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        if (!pageable.getSort().isSorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by("createdAt").descending()
            );
        }
        Page<PostDTO> postsPage = postService.getPostsByUserId(userId, pageable);
        List<UserImageDTO> images = userImageRepository.findByUserId(userId).stream()
                .map(image -> new UserImageDTO(image.getId(), image.getImageUrl(), image.getDescription(), image.getCreatedAt()))
                .collect(Collectors.toList());

        List<ProgramsDTO> programs = getUserPrograms(userId);
        return new ProfileResponse(user, postsPage, images, programs);
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

        try {
            User user = userRepository.findById(id)
                    .orElse(null);

            if (user == null) {
                return false;
            }

            return user.getUsername().equals(currentUsername);
        } catch (Exception e) {
            return false;
        }
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

    @Transactional
    public void restoreUser(Long id) {
        userRepository.findDeletedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng đã xóa với ID: " + id));
        userRepository.restoreById(id);
    }

    public Page<User> findAllUsersIncludingDeleted(Pageable pageable) {
        return userRepository.findAllIncludingDeleted(pageable);
    }
}