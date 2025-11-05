package com.tim.appTim.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tim.appTim.dto.ClassDTO;
import com.tim.appTim.dto.ProgramsDTO;
import com.tim.appTim.entity.Class;
import com.tim.appTim.entity.ClassMember;
import com.tim.appTim.entity.User;
import com.tim.appTim.repository.ClassMemberRepository;
import com.tim.appTim.repository.ClassRepository;
import com.tim.appTim.service.ProgramsService;
import com.tim.appTim.service.UserService;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.exception.UnprocessableException;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.exception.ConflictException;
import com.tim.appTim.exception.InternalServerErrorException;

import java.util.Map;
import org.springframework.security.core.Authentication;
import com.tim.appTim.dto.AddMemberDTO;

@Service("classService")
public class ClassService {

    private final UserService userService;
    private final ClassRepository classRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ProgramsService programsService;

    public ClassService(UserService userService,
                        ClassRepository classRepository,
                        ClassMemberRepository classMemberRepository,
                        ProgramsService programsService) {
        this.userService = userService;
        this.classRepository = classRepository;
        this.classMemberRepository = classMemberRepository;
        this.programsService = programsService;
    }

    @Transactional(readOnly = true)
    public List<ClassDTO> getAllClasses() {
        try {
            List<Class> classes = classRepository.findAll();

            if (classes == null || classes.isEmpty()) {
                return new java.util.ArrayList<>();
            }

            List<Long> classIds = classes.stream()
                    .filter(c -> c != null && c.getId() != null)
                    .map(Class::getId)
                    .collect(Collectors.toList());
            List<ClassMember> allMembers = new java.util.ArrayList<>();
            for (Long classId : classIds) {
                try {
                    List<ClassMember> members = classMemberRepository.findByClassId(classId);
                    if (members != null) {
                        allMembers.addAll(members);
                    }
                } catch (Exception e) {
                    System.err.println("Error loading members for class " + classId + ": " + e.getMessage());
                }
            }

            Map<Long, List<ClassMember>> membersByClassId = allMembers.stream()
                    .filter(m -> m != null && m.getClassId() != null)
                    .collect(Collectors.groupingBy(ClassMember::getClassId));
            
            return classes.stream()
                    .filter(classEntity -> classEntity != null && classEntity.getId() != null)
                    .map(classEntity -> {
                        try {
                            List<ClassMember> members = membersByClassId.getOrDefault(
                                    classEntity.getId(), 
                                    new java.util.ArrayList<>()
                            );
                            
                            List<ClassDTO.MemberDTO> memberDTOs = members.stream()
                                    .filter(m -> m != null && m.getRole() != null)
                                    .map(member -> new ClassDTO.MemberDTO(
                                            member.getUserId(),
                                            member.getRole().name(),
                                            member.getJoinDate()
                                    ))
                                    .collect(Collectors.toList());
                            ProgramsDTO programDTO = null;

                            ClassDTO classDTO = new ClassDTO(
                                    classEntity.getId(),
                                    classEntity.getClassName() != null ? classEntity.getClassName() : "",
                                    classEntity.getDescription() != null ? classEntity.getDescription() : "",
                                    memberDTOs,
                                    classEntity.getProgramId(),
                                    programDTO
                            );
                            
                            return classDTO;
                        } catch (Exception e) {
                            System.err.println("Error processing class " + classEntity.getId() + ": " + e.getMessage());
                            return new ClassDTO(
                                    classEntity.getId(),
                                    classEntity.getClassName() != null ? classEntity.getClassName() : "",
                                    "",
                                    new java.util.ArrayList<>(),
                                    classEntity.getProgramId(),
                                    null
                            );
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error in getAllClasses: " + e.getMessage());
            e.printStackTrace();
            throw new InternalServerErrorException("Lỗi khi lấy danh sách lớp học: " + e.getMessage());
        }
    }

    public Optional<Class> getClassById(Long id) {
        return classRepository.findById(id);
    }

    public List<ClassMember> getClassMembersByClassId(Long classId) {
        return classMemberRepository.findByClassId(classId);
    }
    public boolean isClassMember(Authentication authentication, Long classId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        User currentUser = userService.findByUsernameOrEmail(authentication.getName());
        if (currentUser == null) {
            return false;
        }

        return classMemberRepository.findByUserIdAndClassId(currentUser.getId(), classId).isPresent();
    }

    public boolean isClassTeacher(Authentication authentication, Long classId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        User currentUser = userService.findByUsernameOrEmail(authentication.getName());
        if (currentUser == null) {
            return false;
        }

        Optional<ClassMember> memberOpt = classMemberRepository.findByUserIdAndClassId(currentUser.getId(), classId);

        if (memberOpt.isEmpty()) {
            return false;
        }

        ClassMember member = memberOpt.get();
        return member.getRole() == ClassMember.Role.giao_vien;
    }

    public ClassDTO createClass(ClassDTO classDTO, Authentication authentication) {
        User currentUser = userService.findByUsernameOrEmail(authentication.getName());
        if (currentUser == null) {
            throw new BadRequestException("Không tìm thấy thông tin người dùng hiện tại");
        }

        if (classDTO.getClassName() == null || classDTO.getClassName().trim().isEmpty()) {
            throw new BadRequestException("Tên lớp học (className) là bắt buộc");
        }

        Class newClass = new Class();
        newClass.setClassName(classDTO.getClassName());
        newClass.setDescription(classDTO.getDescription());
        newClass.setProgramId(classDTO.getProgramId());

        Class savedClass = classRepository.save(newClass);

        ProgramsDTO programDTO = null;
        if (savedClass.getProgramId() != null) {
                try {
                    programDTO = programsService.getProgramById(savedClass.getProgramId());
                } catch (ResourceNotFoundException e) {
            }
        }

        return new ClassDTO(
                savedClass.getId(),
                savedClass.getClassName(),
                savedClass.getDescription(),
                List.of(), 
                classDTO.getProgramId(),
                programDTO
        );
    }


    public ClassDTO updateClass(Long id, ClassDTO classDTO) {
        Class existingClass = classRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + id));

        if (classDTO.getClassName() == null || classDTO.getClassName().trim().isEmpty()) {
            throw new BadRequestException("Tên lớp học (className) là bắt buộc");
        }

        boolean nameConflict = classRepository.findAll().stream()
            .anyMatch(c -> !c.getId().equals(id) && 
                        c.getClassName().equalsIgnoreCase(classDTO.getClassName()));
        if (nameConflict) {
            throw new ConflictException("Tên lớp học '" + classDTO.getClassName() + "' đã tồn tại");
        }
        
        if (classDTO.getClassName() != null) {
            existingClass.setClassName(classDTO.getClassName());
        }
        if (classDTO.getDescription() != null) {
            existingClass.setDescription(classDTO.getDescription());
        }
        if (classDTO.getProgramId() != null) {
            existingClass.setProgramId(classDTO.getProgramId());
        }

        Class saved = classRepository.save(existingClass);

        List<ClassMember> members = classMemberRepository.findByClassId(saved.getId());
        List<ClassDTO.MemberDTO> memberDTOs = members.stream()
                .map(m -> new ClassDTO.MemberDTO(
                        m.getUserId(),
                        m.getRole().name(),
                        m.getJoinDate()
                ))
                .toList();

        

        return new ClassDTO(
                saved.getId(),  
                saved.getClassName(),
                saved.getDescription(),
                memberDTOs,
                saved.getProgramId(),
                null
        );
    }

    @Transactional
    public ClassDTO updateClassProgram(Long classId, Integer programId) {
        Class existingClass = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId));
        existingClass.setProgramId(programId);
        Class saved = classRepository.save(existingClass);
        ProgramsDTO programDTO = null;
        if (programId != null) {
            try {
                programDTO = programsService.getProgramById(programId);
            } catch (ResourceNotFoundException ignored) {}
        }
        List<ClassMember> members = classMemberRepository.findByClassId(classId);
        List<ClassDTO.MemberDTO> memberDTOs = members.stream()
                .map(m -> new ClassDTO.MemberDTO(
                        m.getUserId(),
                        m.getRole().name(),
                        m.getJoinDate()
                ))
                .toList();
        return new ClassDTO(
                saved.getId(),
                saved.getClassName(),
                saved.getDescription(),
                memberDTOs,
                programId,
                programDTO
        );
    }


    public void deleteClass(Long id) {

        Class existingClass = classRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + id));
            
        classMemberRepository.deleteById(id);
        classRepository.delete(existingClass);
    }


    public ClassMember addMember(Long classId, AddMemberDTO addMemberDTO) {
        Class existingClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với id: " + classId));

        User user = userService.findById(addMemberDTO.getUserId());
        if (user == null) {
            throw new ResourceNotFoundException("Người dùng không tồn tại với id: " + addMemberDTO.getUserId());
        }
        boolean exists = classMemberRepository.existsByClassIdAndUserId(classId, addMemberDTO.getUserId());
        if (exists) {
            throw new BadRequestException("Người dùng đã là thành viên của lớp này");
        }

        ClassMember member = new ClassMember();
        member.setClassId(classId);
        member.setUserId(addMemberDTO.getUserId());
        member.setRole(ClassMember.Role.valueOf(addMemberDTO.getRole()));
        member.setJoinDate(LocalDateTime.now());

        return classMemberRepository.save(member);
    }


    public void removeMember(Long classId, Long userIdToRemove) {
        classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp với id: " + classId));

    ClassMember member = classMemberRepository.findByClassIdAndUserId(classId, userIdToRemove)
            .orElseThrow(() -> new ResourceNotFoundException("Người dùng không thuộc lớp này"));

    classMemberRepository.delete(member);
}


    public ClassMember addMemberToClass(Long classId, Long userId, ClassMember.Role role) {
        Optional<ClassMember> existingMember = classMemberRepository.findByClassIdAndUserId(classId, userId);
        if (existingMember.isPresent()) {
            throw new BadRequestException("User đã tham gia lớp học này rồi");
        }

        ClassMember classMember = new ClassMember();
        classMember.setClassId(classId);
        classMember.setUserId(userId);
        classMember.setRole(role);
        classMember.setJoinDate(LocalDateTime.now());
        
        return classMemberRepository.save(classMember);
    }

    public ClassMember updateMemberRole(Long classId, Long userId, ClassMember.Role newRole) {
        ClassMember classMember = classMemberRepository.findByClassIdAndUserId(classId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên trong lớp học"));
        
        classMember.setRole(newRole);
        return classMemberRepository.save(classMember);
    }

    public void removeMemberFromClass(Long classId, Long userId) {
        ClassMember classMember = classMemberRepository.findByClassIdAndUserId(classId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên trong lớp học"));
        
        classMemberRepository.delete(classMember);
    }

    public List<ClassMember> getUserClasses(Long userId) {
        return classMemberRepository.findByUserId(userId);
    }

    public ClassDTO getClassDTOById(Long classId) {
        Class classInfo = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với id = " + classId));

        List<ClassMember> members = classMemberRepository.findByClassId(classId);
        
        List<ClassDTO.MemberDTO> memberDTOs = members.stream()
                .map(member -> {
                    try {
                        User user = userService.findById(member.getUserId());
                        if (user != null) {
                            return new ClassDTO.MemberDTO(
                                    member.getUserId(),
                                    member.getRole().name(),
                                    member.getJoinDate(),
                                    user.getUsername(),
                                    user.getFirstName(),
                                    user.getLastName(),
                                    user.getEmail(),
                                    user.getProfileImage()
                            );
                        } else {

                            return new ClassDTO.MemberDTO(
                                    member.getUserId(),
                                    member.getRole().name(),
                                    member.getJoinDate()
                            );
                        }
                    } catch (Exception e) {
                        return new ClassDTO.MemberDTO(
                                member.getUserId(),
                                member.getRole().name(),
                                member.getJoinDate()
                        );
                    }
                })
                .collect(Collectors.toList());

        ProgramsDTO programDTO = null;
        if (classInfo.getProgramId() != null) {
                try {
                    programDTO = programsService.getProgramById(classInfo.getProgramId());
                } catch (ResourceNotFoundException e) {
            }
        }

        return new ClassDTO(
                classInfo.getId(),
                classInfo.getClassName(),
                classInfo.getDescription(),
                memberDTOs,
                classInfo.getProgramId(),
                programDTO
        );
    }

}