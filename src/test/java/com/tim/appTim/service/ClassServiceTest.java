package com.tim.appTim.service;

import com.tim.appTim.dto.AddMemberDTO;
import com.tim.appTim.dto.AddMemberRequest;
import com.tim.appTim.dto.ClassDTO;
import com.tim.appTim.dto.ProgramsDTO;
import com.tim.appTim.entity.Class;
import com.tim.appTim.entity.ClassMember;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.exception.ConflictException;
import com.tim.appTim.exception.InternalServerErrorException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.ClassMemberRepository;
import com.tim.appTim.repository.ClassRepository;
import com.tim.appTim.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private ClassRepository classRepository;
    @Mock
    private ClassMemberRepository classMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProgramsService programsService;
    @Mock
    private ClassModuleService classModuleService;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private ClassService classService;

    private User user;
    private Class classEntity;
    private ClassMember classMember;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        classEntity = new Class();
        classEntity.setId(100L);
        classEntity.setClassName("Test Class");
        classEntity.setDescription("Description");
        classEntity.setProgramId(1);

        classMember = new ClassMember();
        classMember.setId(10L);
        classMember.setClassId(100L);
        classMember.setUserId(1L);
        classMember.setRole(ClassMember.Role.sinh_vien);
        classMember.setJoinDate(LocalDateTime.now());
    }

    // --- getAllClasses ---

    @Test
    // Covers: Empty page
    void getAllClasses_Empty() {
        when(classRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());
        Page<ClassDTO> result = classService.getAllClasses(PageRequest.of(0, 10));
        assertThat(result).isEmpty();
    }

    @Test
    // Covers: Success with members and program
    void getAllClasses_Success() {
        Page<Class> page = new PageImpl<>(Collections.singletonList(classEntity));
        when(classRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(classMemberRepository.findAllById(anyList())).thenReturn(Collections.singletonList(classMember));

        Page<ClassDTO> result = classService.getAllClasses(PageRequest.of(0, 10));
        assertThat(result).isNotEmpty();
        assertThat(result.getContent().get(0).getMembers()).hasSize(1);
    }

    @Test
    // Covers: Exception in processing
    void getAllClasses_ExceptionInProcessing() {
        Page<Class> page = new PageImpl<>(Collections.singletonList(classEntity));
        when(classRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(classMemberRepository.findAllById(anyList())).thenThrow(new RuntimeException("Processing Error"));

        assertThatThrownBy(() -> classService.getAllClasses(PageRequest.of(0, 10)))
                .isInstanceOf(InternalServerErrorException.class);
    }

    // --- getClassById ---

    @Test
    // Covers: Success
    void getClassById_Success() {
        when(classRepository.findById(100L)).thenReturn(Optional.of(classEntity));
        Optional<Class> result = classService.getClassById(100L);
        assertThat(result).isPresent();
    }

    // --- getClassMembersByClassId ---

    @Test
    // Covers: Success
    void getClassMembersByClassId_Success() {
        when(classMemberRepository.findByClassId(100L)).thenReturn(Collections.singletonList(classMember));
        List<ClassMember> result = classService.getClassMembersByClassId(100L);
        assertThat(result).hasSize(1);
    }

    // --- isClassMember ---

    @Test
    // Covers: Not authenticated
    void isClassMember_NotAuth() {
        assertThat(classService.isClassMember(null, 100L)).isFalse();
    }

    @Test
    // Covers: User not found
    void isClassMember_UserNotFound() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("user");
        when(userService.findByUsernameOrEmail("user")).thenReturn(null);
        assertThat(classService.isClassMember(authentication, 100L)).isFalse();
    }

    @Test
    // Covers: Success
    void isClassMember_Success() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("user");
        when(userService.findByUsernameOrEmail("user")).thenReturn(user);
        when(classMemberRepository.findByUserIdAndClassId(1L, 100L)).thenReturn(Optional.of(classMember));
        assertThat(classService.isClassMember(authentication, 100L)).isTrue();
    }

    // --- isClassTeacher ---

    @Test
    // Covers: Not authenticated
    void isClassTeacher_NotAuth() {
        assertThat(classService.isClassTeacher(null, 100L)).isFalse();
    }

    @Test
    // Covers: User not found
    void isClassTeacher_UserNotFound() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("user");
        when(userService.findByUsernameOrEmail("user")).thenReturn(null);
        assertThat(classService.isClassTeacher(authentication, 100L)).isFalse();
    }

    @Test
    // Covers: Not a member
    void isClassTeacher_NotMember() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("user");
        when(userService.findByUsernameOrEmail("user")).thenReturn(user);
        when(classMemberRepository.findByUserIdAndClassId(1L, 100L)).thenReturn(Optional.empty());
        assertThat(classService.isClassTeacher(authentication, 100L)).isFalse();
    }

    @Test
    // Covers: Is Teacher
    void isClassTeacher_Success() {
        classMember.setRole(ClassMember.Role.giao_vien);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("user");
        when(userService.findByUsernameOrEmail("user")).thenReturn(user);
        when(classMemberRepository.findByUserIdAndClassId(1L, 100L)).thenReturn(Optional.of(classMember));
        assertThat(classService.isClassTeacher(authentication, 100L)).isTrue();
    }

    // --- createClass ---

    @Test
    // Covers: User not found
    void createClass_UserNotFound() {
        when(authentication.getName()).thenReturn("user");
        when(userService.findByUsernameOrEmail("user")).thenReturn(null);
        ClassDTO dto = new ClassDTO(null, null, null, null, null, null);
        assertThatThrownBy(() -> classService.createClass(dto, authentication, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Không tìm thấy thông tin người dùng");
    }

    @Test
    // Covers: Class name empty
    void createClass_NameEmpty() {
        when(authentication.getName()).thenReturn("user");
        when(userService.findByUsernameOrEmail("user")).thenReturn(user);
        ClassDTO dto = new ClassDTO(null, null, null, null, null, null);
        assertThatThrownBy(() -> classService.createClass(dto, authentication, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Tên lớp học (className) là bắt buộc");
    }

    @Test
    // Covers: Name conflict
    void createClass_NameConflict() {
        when(authentication.getName()).thenReturn("user");
        when(userService.findByUsernameOrEmail("user")).thenReturn(user);
        ClassDTO dto = new ClassDTO(null, "Existing", null, null, null, null);

        Class existing = new Class();
        existing.setId(200L);
        existing.setClassName("Existing");
        when(classRepository.findAll()).thenReturn(Collections.singletonList(existing));

        assertThatThrownBy(() -> classService.createClass(dto, authentication, 100L))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    // Covers: Success
    void createClass_Success() {
        when(authentication.getName()).thenReturn("user");
        when(userService.findByUsernameOrEmail("user")).thenReturn(user);
        ClassDTO dto = new ClassDTO(null, "New Class", null, null, 1, null);

        when(classRepository.findAll()).thenReturn(Collections.emptyList());
        when(classRepository.save(any(Class.class))).thenAnswer(i -> {
            Class c = i.getArgument(0);
            c.setId(100L);
            return c;
        });
        when(programsService.getProgramById(1)).thenReturn(new ProgramsDTO());

        ClassDTO result = classService.createClass(dto, authentication, null);
        assertThat(result.getId()).isEqualTo(100L);
    }

    // --- updateClass ---

    @Test
    // Covers: Class not found
    void updateClass_NotFound() {
        when(classRepository.findById(100L)).thenReturn(Optional.empty());
        ClassDTO dto = new ClassDTO(null, null, null, null, null, null);
        assertThatThrownBy(() -> classService.updateClass(100L, dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Name empty
    void updateClass_NameEmpty() {
        when(classRepository.findById(100L)).thenReturn(Optional.of(classEntity));
        ClassDTO dto = new ClassDTO(null, null, null, null, null, null);
        assertThatThrownBy(() -> classService.updateClass(100L, dto))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    // Covers: Name conflict
    void updateClass_NameConflict() {
        when(classRepository.findById(100L)).thenReturn(Optional.of(classEntity));
        ClassDTO dto = new ClassDTO(null, "Existing", null, null, null, null);

        Class existing = new Class();
        existing.setId(200L);
        existing.setClassName("Existing");
        when(classRepository.findAll()).thenReturn(Collections.singletonList(existing));

        assertThatThrownBy(() -> classService.updateClass(100L, dto))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    // Covers: Success
    void updateClass_Success() {
        when(classRepository.findById(100L)).thenReturn(Optional.of(classEntity));
        when(classRepository.findAll()).thenReturn(Collections.emptyList());
        when(classRepository.save(any(Class.class))).thenReturn(classEntity);
        when(classMemberRepository.findByClassId(100L)).thenReturn(Collections.emptyList());

        ClassDTO dto = new ClassDTO(null, "Updated Name", "Updated Desc", null, 2, null);

        ClassDTO result = classService.updateClass(100L, dto);
        assertThat(result.getClassName()).isEqualTo("Updated Name");
    }

    // --- updateClassProgram ---

    @Test
    // Covers: Class not found
    void updateClassProgram_NotFound() {
        when(classRepository.findById(100L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> classService.updateClassProgram(100L, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Success
    void updateClassProgram_Success() {
        when(classRepository.findById(100L)).thenReturn(Optional.of(classEntity));
        when(classRepository.save(any(Class.class))).thenReturn(classEntity);
        when(classMemberRepository.findByClassId(100L)).thenReturn(Collections.emptyList());
        when(programsService.getProgramById(1)).thenReturn(new ProgramsDTO());

        ClassDTO result = classService.updateClassProgram(100L, 1);
        assertThat(result.getProgramId()).isEqualTo(1);
    }

    // --- deleteClass ---

    @Test
    // Covers: Class not found
    void deleteClass_NotFound() {
        when(classRepository.findById(100L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> classService.deleteClass(100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Success
    void deleteClass_Success() {
        when(classRepository.findById(100L)).thenReturn(Optional.of(classEntity));
        classService.deleteClass(100L);
        verify(classRepository).delete(classEntity);
    }

    // --- addMember ---

    @Test
    // Covers: Class not found
    void addMember_ClassNotFound() {
        when(classRepository.findById(100L)).thenReturn(Optional.empty());
        AddMemberDTO dto = new AddMemberDTO(null, null);
        assertThatThrownBy(() -> classService.addMember(100L, dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: User not found
    void addMember_UserNotFound() {
        when(classRepository.findById(100L)).thenReturn(Optional.of(classEntity));
        when(userService.findById(1L)).thenReturn(null);
        AddMemberDTO dto = new AddMemberDTO(1L, null);
        assertThatThrownBy(() -> classService.addMember(100L, dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Already exists
    void addMember_AlreadyExists() {
        when(classRepository.findById(100L)).thenReturn(Optional.of(classEntity));
        when(userService.findById(1L)).thenReturn(user);
        when(classMemberRepository.existsByClassIdAndUserId(100L, 1L)).thenReturn(true);
        AddMemberDTO dto = new AddMemberDTO(1L, null);
        assertThatThrownBy(() -> classService.addMember(100L, dto))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    // Covers: Success
    void addMember_Success() {
        when(classRepository.findById(100L)).thenReturn(Optional.of(classEntity));
        when(userService.findById(1L)).thenReturn(user);
        when(classMemberRepository.existsByClassIdAndUserId(100L, 1L)).thenReturn(false);
        when(classMemberRepository.save(any(ClassMember.class))).thenReturn(classMember);

        AddMemberDTO dto = new AddMemberDTO(1L, "sinh_vien");

        ClassMember result = classService.addMember(100L, dto);
        assertThat(result).isNotNull();
    }

    // --- addMembersBatch ---

    @Test
    // Covers: Class not found
    void addMembersBatch_ClassNotFound() {
        when(classRepository.findById(100L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> classService.addMembersBatch(100L, Collections.emptyList()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Invalid Request (Null fields)
    void addMembersBatch_InvalidRequest() {
        when(classRepository.findById(100L)).thenReturn(Optional.of(classEntity));
        AddMemberRequest req = new AddMemberRequest();
        assertThatThrownBy(() -> classService.addMembersBatch(100L, Collections.singletonList(req)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    // Covers: User not found
    void addMembersBatch_UserNotFound() {
        when(classRepository.findById(100L)).thenReturn(Optional.of(classEntity));
        AddMemberRequest req = new AddMemberRequest();
        req.setUserId(1L);
        req.setRole(ClassMember.Role.sinh_vien);
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> classService.addMembersBatch(100L, Collections.singletonList(req)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Success (Skip existing, Add new)
    void addMembersBatch_Success() {
        when(classRepository.findById(100L)).thenReturn(Optional.of(classEntity));

        AddMemberRequest req1 = new AddMemberRequest();
        req1.setUserId(1L);
        req1.setRole(ClassMember.Role.sinh_vien);

        AddMemberRequest req2 = new AddMemberRequest();
        req2.setUserId(2L);
        req2.setRole(ClassMember.Role.sinh_vien);

        when(userRepository.existsById(anyLong())).thenReturn(true);
        when(classMemberRepository.existsByClassIdAndUserId(100L, 1L)).thenReturn(true); // Skip
        when(classMemberRepository.existsByClassIdAndUserId(100L, 2L)).thenReturn(false); // Add
        when(classMemberRepository.save(any(ClassMember.class))).thenReturn(classMember);

        List<ClassMember> result = classService.addMembersBatch(100L, Arrays.asList(req1, req2));
        assertThat(result).hasSize(1);
    }

    // --- removeMember ---

    @Test
    // Covers: Class not found
    void removeMember_ClassNotFound() {
        when(classRepository.findById(100L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> classService.removeMember(100L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Member not found
    void removeMember_MemberNotFound() {
        when(classRepository.findById(100L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassIdAndUserId(100L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> classService.removeMember(100L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Success
    void removeMember_Success() {
        when(classRepository.findById(100L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassIdAndUserId(100L, 1L)).thenReturn(Optional.of(classMember));
        classService.removeMember(100L, 1L);
        verify(classMemberRepository).delete(classMember);
    }

    // --- addMemberToClass ---

    @Test
    // Covers: Already exists
    void addMemberToClass_Exists() {
        when(classMemberRepository.findByClassIdAndUserId(100L, 1L)).thenReturn(Optional.of(classMember));
        assertThatThrownBy(() -> classService.addMemberToClass(100L, 1L, ClassMember.Role.sinh_vien))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    // Covers: Success
    void addMemberToClass_Success() {
        when(classMemberRepository.findByClassIdAndUserId(100L, 1L)).thenReturn(Optional.empty());
        when(classMemberRepository.save(any(ClassMember.class))).thenReturn(classMember);
        ClassMember result = classService.addMemberToClass(100L, 1L, ClassMember.Role.sinh_vien);
        assertThat(result).isNotNull();
    }

    // --- updateMemberRole ---

    @Test
    // Covers: Member not found
    void updateMemberRole_NotFound() {
        when(classMemberRepository.findByClassIdAndUserId(100L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> classService.updateMemberRole(100L, 1L, ClassMember.Role.giao_vien))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Success
    void updateMemberRole_Success() {
        when(classMemberRepository.findByClassIdAndUserId(100L, 1L)).thenReturn(Optional.of(classMember));
        when(classMemberRepository.save(any(ClassMember.class))).thenReturn(classMember);
        ClassMember result = classService.updateMemberRole(100L, 1L, ClassMember.Role.giao_vien);
        assertThat(result.getRole()).isEqualTo(ClassMember.Role.giao_vien);
    }

    // --- removeMemberFromClass ---

    @Test
    // Covers: Member not found
    void removeMemberFromClass_NotFound() {
        when(classMemberRepository.findByClassIdAndUserId(100L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> classService.removeMemberFromClass(100L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Success
    void removeMemberFromClass_Success() {
        when(classMemberRepository.findByClassIdAndUserId(100L, 1L)).thenReturn(Optional.of(classMember));
        classService.removeMemberFromClass(100L, 1L);
        verify(classMemberRepository).delete(classMember);
    }

    // --- getUserClasses ---

    @Test
    // Covers: Success
    void getUserClasses_Success() {
        when(classMemberRepository.findByUserId(1L)).thenReturn(Collections.singletonList(classMember));
        List<ClassMember> result = classService.getUserClasses(1L);
        assertThat(result).hasSize(1);
    }

    // --- getClassDTOById ---

    @Test
    // Covers: Class not found
    void getClassDTOById_NotFound() {
        when(classRepository.findById(100L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> classService.getClassDTOById(100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Success with user details
    void getClassDTOById_Success() {
        when(classRepository.findById(100L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(100L)).thenReturn(Collections.singletonList(classMember));
        when(userService.findById(1L)).thenReturn(user);
        when(programsService.getProgramById(1)).thenReturn(new ProgramsDTO());

        ClassDTO result = classService.getClassDTOById(100L);
        assertThat(result.getMembers()).hasSize(1);
        assertThat(result.getMembers().get(0).getUsername()).isEqualTo("testuser");
    }
}
