package com.tim.appTim.service;

import com.tim.appTim.dto.PostDTO;
import com.tim.appTim.dto.ProfileResponse;
import com.tim.appTim.dto.UserImageDTO;
import com.tim.appTim.dto.UserUpdateDTO;
import com.tim.appTim.entity.Role;
import com.tim.appTim.entity.User;
import com.tim.appTim.entity.UserImage;
import com.tim.appTim.exception.*;
import com.tim.appTim.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    @Mock
    private PostService postService;
    @Mock
    private UserImageRepository userImageRepository;
    @Mock
    private ClassMemberRepository classMemberRepository;
    @Mock
    private ClassRepository classRepository;
    @Mock
    private ProgramModuleRepository programModuleRepository;
    @Mock
    private ProgramsRepository programsRepository;

    @InjectMocks
    private UserService userService;

    private User user;
    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("ROLE_USER");

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password123");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRoles(new HashSet<>(Collections.singleton(userRole)));
        user.setCreatedAt(LocalDateTime.now());
    }

    // --- register ---

    @Test
    // Covers branch: Username null/empty
    void register_Fail_UsernameNull() {
        user.setUsername(null);
        assertThatThrownBy(() -> userService.register(user))
                .isInstanceOf(UnprocessableException.class)
                .hasMessageContaining("Username is required");
    }

    @Test
    // Covers branch: Username exists
    void register_Fail_UsernameExists() {
        when(userRepository.existsByUsername(user.getUsername())).thenReturn(true);
        assertThatThrownBy(() -> userService.register(user))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    // Covers branch: Email null/empty
    void register_Fail_EmailNull() {
        when(userRepository.existsByUsername(user.getUsername())).thenReturn(false);
        user.setEmail(null);
        assertThatThrownBy(() -> userService.register(user))
                .isInstanceOf(UnprocessableException.class)
                .hasMessageContaining("Email is required");
    }

    @Test
    // Covers branch: Email exists
    void register_Fail_EmailExists() {
        when(userRepository.existsByUsername(user.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(true);
        assertThatThrownBy(() -> userService.register(user))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    // Covers branch: Password null/empty
    void register_Fail_PasswordNull() {
        when(userRepository.existsByUsername(user.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(false);
        user.setPassword(null);
        assertThatThrownBy(() -> userService.register(user))
                .isInstanceOf(UnprocessableException.class)
                .hasMessageContaining("Password is required");
    }

    @Test
    // Covers branch: Role not found
    void register_Fail_RoleNotFound() {
        when(userRepository.existsByUsername(user.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.register(user))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Role 'ROLE_USER' không tồn tại");
    }

    @Test
    // Covers branch: Success
    void register_Success() {
        when(userRepository.existsByUsername(user.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.register(user);

        verify(userRepository).save(user);
        assertThat(user.getPassword()).isEqualTo("encoded");
    }

    // --- loadUserByUsername ---

    @Test
    // Covers branch: Found by username
    void loadUserByUsername_Success_Username() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        UserDetails userDetails = userService.loadUserByUsername("testuser");
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
    }

    @Test
    // Covers branch: Found by email
    void loadUserByUsername_Success_Email() {
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        UserDetails userDetails = userService.loadUserByUsername("test@example.com");
        assertThat(userDetails).isNotNull();
    }

    @Test
    // Covers branch: Not found
    void loadUserByUsername_Fail_NotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.loadUserByUsername("unknown"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }

    // --- findById ---

    @Test
    // Covers branch: Found
    void findById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        User found = userService.findById(1L);
        assertThat(found).isEqualTo(user);
    }

    @Test
    // Covers branch: Not found
    void findById_Fail() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- create ---

    @Test
    // Covers branch: Username exists (including deleted)
    void create_Fail_UsernameExists() {
        user.setUsername("existing");
        when(userRepository.findIdByUsernameIncludingDeleted("existing")).thenReturn(1L);
        assertThatThrownBy(() -> userService.create(user))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    // Covers branch: Email exists (including deleted)
    void create_Fail_EmailExists() {
        when(userRepository.findIdByUsernameIncludingDeleted(anyString())).thenReturn(null);
        user.setEmail("existing@email.com");
        when(userRepository.findIdByEmailIncludingDeleted("existing@email.com")).thenReturn(1L);
        assertThatThrownBy(() -> userService.create(user))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    // Covers branch: Phone number invalid format
    void create_Fail_PhoneInvalid() {
        when(userRepository.findIdByUsernameIncludingDeleted(anyString())).thenReturn(null);
        when(userRepository.findIdByEmailIncludingDeleted(anyString())).thenReturn(null);
        user.setPhoneNumber("abc");
        assertThatThrownBy(() -> userService.create(user))
                .isInstanceOf(UnprocessableException.class)
                .hasMessageContaining("Số điện thoại chỉ được chứa số");
    }

    @Test
    // Covers branch: Phone number too long
    void create_Fail_PhoneTooLong() {
        when(userRepository.findIdByUsernameIncludingDeleted(anyString())).thenReturn(null);
        when(userRepository.findIdByEmailIncludingDeleted(anyString())).thenReturn(null);
        user.setPhoneNumber("123456789012");
        assertThatThrownBy(() -> userService.create(user))
                .isInstanceOf(UnprocessableException.class)
                .hasMessageContaining("Số điện thoại không được quá 11 số");
    }

    @Test
    // Covers branch: Phone number exists
    void create_Fail_PhoneExists() {
        when(userRepository.findIdByUsernameIncludingDeleted(anyString())).thenReturn(null);
        when(userRepository.findIdByEmailIncludingDeleted(anyString())).thenReturn(null);
        user.setPhoneNumber("1234567890");
        when(userRepository.findIdByPhoneNumberIncludingDeleted("1234567890")).thenReturn(1L);
        assertThatThrownBy(() -> userService.create(user))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Số điện thoại đã được sử dụng");
    }

    @Test
    // Covers branch: Too many roles
    void create_Fail_TooManyRoles() {
        when(userRepository.findIdByUsernameIncludingDeleted(anyString())).thenReturn(null);
        when(userRepository.findIdByEmailIncludingDeleted(anyString())).thenReturn(null);
        user.setPhoneNumber(null);
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        roles.add(new Role());
        user.setRoles(roles);
        assertThatThrownBy(() -> userService.create(user))
                .isInstanceOf(UnprocessableException.class)
                .hasMessageContaining("Chỉ được chọn một role duy nhất");
    }

    @Test
    // Covers branch: Success with default role
    void create_Success_DefaultRole() {
        when(userRepository.findIdByUsernameIncludingDeleted(anyString())).thenReturn(null);
        when(userRepository.findIdByEmailIncludingDeleted(anyString())).thenReturn(null);
        user.setRoles(null);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User created = userService.create(user);
        assertThat(created.getRoles()).contains(userRole);
    }

    // --- update ---

    @Test
    // Covers branch: Update username conflict
    void update_Fail_UsernameConflict() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setUsername("newuser");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        assertThatThrownBy(() -> userService.update(1L, dto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    // Covers branch: Update phone number too short
    void update_Fail_PhoneTooShort() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setUsername("testuser");
        dto.setPhoneNumber("123");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.update(1L, dto))
                .isInstanceOf(UnprocessableException.class)
                .hasMessageContaining("Số điện thoại phải có ít nhất 10 số");
    }

    @Test
    // Covers branch: Update phone number conflict (different user)
    void update_Fail_PhoneConflict() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setUsername("testuser");
        dto.setPhoneNumber("0987654321");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneNumber("0987654321")).thenReturn(true);
        User otherUser = new User();
        otherUser.setId(2L);
        when(userRepository.findByPhoneNumber("0987654321")).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> userService.update(1L, dto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Số điện thoại đã được sử dụng");
    }

    @Test
    // Covers branch: Update role not found
    void update_Fail_RoleNotFound() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setUsername("testuser");
        dto.setRole("ROLE_ADMIN");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(1L, dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers branch: Success update all fields
    void update_Success() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setUsername("newuser");
        dto.setFirstName("New");
        dto.setLastName("Name");
        dto.setPhoneNumber("0987654321");
        dto.setRole("ROLE_USER");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User updated = userService.update(1L, dto);
        assertThat(updated.getUsername()).isEqualTo("newuser");
        assertThat(updated.getFirstName()).isEqualTo("New");
        assertThat(updated.getPhoneNumber()).isEqualTo("0987654321");
    }

    @Test
    // Covers branch: Internal Server Error on Save
    void update_Fail_SaveException() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setUsername("testuser");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("DB Error"));

        assertThatThrownBy(() -> userService.update(1L, dto))
                .isInstanceOf(InternalServerErrorException.class);
    }

    // --- internalSave ---

    @Test
    // Covers branch: User null
    void internalSave_Fail_Null() {
        assertThatThrownBy(() -> userService.internalSave(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    // Covers branch: Success
    void internalSave_Success() {
        when(userRepository.save(user)).thenReturn(user);
        User saved = userService.internalSave(user);
        assertThat(saved).isEqualTo(user);
    }

    // --- delete ---

    @Test
    // Covers branch: User not found
    void delete_Fail_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers branch: Delete exception
    void delete_Fail_Exception() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("DB Error")).when(userRepository).deleteById(1L);
        assertThatThrownBy(() -> userService.delete(1L))
                .isInstanceOf(InternalServerErrorException.class);
    }

    @Test
    // Covers branch: Success
    void delete_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        userService.delete(1L);
        verify(userRepository).deleteById(1L);
    }

    // --- getUserProfileByEmail ---

    @Test
    // Covers branch: User not found
    void getUserProfileByEmail_Fail_NotFound() {
        when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getUserProfileByEmail("unknown", Pageable.unpaged()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers branch: Success
    void getUserProfileByEmail_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(postService.getPostsByUserId(eq(1L), any(Pageable.class))).thenReturn(Page.empty());
        when(userImageRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(classMemberRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        ProfileResponse response = userService.getUserProfileByEmail("test@example.com", PageRequest.of(0, 10));
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo(user.getUsername());
        assertThat(response.getEmail()).isEqualTo(user.getEmail());
    }

    // --- getUserProfile ---

    @Test
    // Covers branch: Success
    void getUserProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(postService.getPostsByUserId(eq(1L), any(Pageable.class))).thenReturn(Page.empty());
        when(userImageRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(classMemberRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        ProfileResponse response = userService.getUserProfile(1L, PageRequest.of(0, 10));
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo(user.getUsername());
        assertThat(response.getEmail()).isEqualTo(user.getEmail());
    }

    // --- User Image Methods ---

    @Test
    // Covers branch: Create image success
    void createUserImage_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userImageRepository.save(any(UserImage.class))).thenAnswer(i -> {
            UserImage img = i.getArgument(0);
            img.setId(10L);
            return img;
        });

        UserImageDTO dto = userService.createUserImage(1L, "url", "desc");
        assertThat(dto.getImageUrl()).isEqualTo("url");
    }

    @Test
    // Covers branch: Update image not found
    void updateUserImage_Fail_NotFound() {
        when(userImageRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.updateUserImage(1L, 10L, "url", "desc"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers branch: Update image unauthorized
    void updateUserImage_Fail_Unauthorized() {
        UserImage img = new UserImage();
        img.setId(10L);
        User other = new User();
        other.setId(2L);
        img.setUser(other);
        when(userImageRepository.findById(10L)).thenReturn(Optional.of(img));

        assertThatThrownBy(() -> userService.updateUserImage(1L, 10L, "url", "desc"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    // Covers branch: Update image success
    void updateUserImage_Success() {
        UserImage img = new UserImage();
        img.setId(10L);
        img.setUser(user);
        when(userImageRepository.findById(10L)).thenReturn(Optional.of(img));
        when(userImageRepository.save(any(UserImage.class))).thenAnswer(i -> i.getArgument(0));

        UserImageDTO dto = userService.updateUserImage(1L, 10L, "newUrl", "newDesc");
        assertThat(dto.getImageUrl()).isEqualTo("newUrl");
    }

    @Test
    // Covers branch: Delete image unauthorized
    void deleteUserImage_Fail_Unauthorized() {
        UserImage img = new UserImage();
        img.setId(10L);
        User other = new User();
        other.setId(2L);
        img.setUser(other);
        when(userImageRepository.findById(10L)).thenReturn(Optional.of(img));

        assertThatThrownBy(() -> userService.deleteUserImage(1L, 10L))
                .isInstanceOf(UnauthorizedException.class);
    }

    // --- updateProfileImage ---

    @Test
    // Covers branch: Invalid URL
    void updateProfileImage_Fail_InvalidUrl() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> userService.updateProfileImage(1L, ""))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    // Covers branch: Success with delete old
    void updateProfileImage_Success() {
        user.setProfileImage("oldUrl");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserImage oldImg = new UserImage();
        oldImg.setImageUrl("oldUrl");
        when(userImageRepository.findByUserId(1L)).thenReturn(Collections.singletonList(oldImg));

        User updated = userService.updateProfileImage(1L, "newUrl");
        assertThat(updated.getProfileImage()).isEqualTo("newUrl");
        verify(userImageRepository).delete(oldImg);
    }

    // --- isSelf ---

    @Test
    // Covers branch: Not authenticated
    void isSelf_Fail_NotAuth() {
        assertThat(userService.isSelf(null, 1L)).isFalse();
    }

    @Test
    // Covers branch: UserDetails principal success
    void isSelf_Success_UserDetails() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("testuser");
        when(auth.getPrincipal()).thenReturn(userDetails);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(userService.isSelf(auth, 1L)).isTrue();
    }

    @Test
    // Covers branch: Jwt principal success
    void isSelf_Success_Jwt() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("preferred_username")).thenReturn("testuser");
        when(auth.getPrincipal()).thenReturn(jwt);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(userService.isSelf(auth, 1L)).isTrue();
    }

    // --- restoreUser ---

    @Test
    // Covers branch: Not found (deleted)
    void restoreUser_Fail_NotFound() {
        when(userRepository.findDeletedById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.restoreUser(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers branch: Success
    void restoreUser_Success() {
        when(userRepository.findDeletedById(1L)).thenReturn(Optional.of(user));
        userService.restoreUser(1L);
        verify(userRepository).restoreById(1L);
    }
}
