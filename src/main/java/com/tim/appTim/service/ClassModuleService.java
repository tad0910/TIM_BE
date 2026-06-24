package com.tim.appTim.service;

import com.tim.appTim.dto.common.ClassModuleDTO;
import com.tim.appTim.dto.common.ClassModuleTeacherDTO;
import com.tim.appTim.entity.Class;
import com.tim.appTim.entity.ClassModule;
import com.tim.appTim.entity.ClassModuleTeacher;
import com.tim.appTim.entity.ProgramModule;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.exception.ConflictException;
import com.tim.appTim.repository.ClassModuleRepository;
import com.tim.appTim.repository.ClassModuleTeacherRepository;
import com.tim.appTim.repository.ClassRepository;
import com.tim.appTim.repository.ModuleRepository;
import com.tim.appTim.repository.ProgramModuleRepository;
import com.tim.appTim.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClassModuleService {

    private final ClassModuleRepository classModuleRepository;
    private final ClassRepository classRepository;
    private final ModuleRepository moduleRepository;
    private final ClassModuleTeacherRepository classModuleTeacherRepository;
    private final UserRepository userRepository;
    private final ProgramModuleRepository programModuleRepository;

    public ClassModuleService(ClassModuleRepository classModuleRepository,
                              ClassRepository classRepository,
                              ModuleRepository moduleRepository,
                              ClassModuleTeacherRepository classModuleTeacherRepository,
                              UserRepository userRepository,
                              ProgramModuleRepository programModuleRepository) {
        this.classModuleRepository = classModuleRepository;
        this.classRepository = classRepository;
        this.moduleRepository = moduleRepository;
        this.classModuleTeacherRepository = classModuleTeacherRepository;
        this.userRepository = userRepository;
        this.programModuleRepository = programModuleRepository;
    }

    @Transactional
    public List<ClassModuleDTO> createClassModulesFromProgram(Long classId) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId));

        if (classEntity.getProgramId() == null) {
            throw new BadRequestException("Lớp học chưa được gán chương trình đào tạo");
        }

        List<ProgramModule> programModules = programModuleRepository.findByProgramId(classEntity.getProgramId());

        if (programModules.isEmpty()) {
            throw new BadRequestException("Chương trình đào tạo chưa có module nào");
        }

        List<ClassModule> createdModules = programModules.stream()
                .map(pm -> {
                    if (!classModuleRepository.existsByClassIdAndModuleId(classId, pm.getModule().getId())) {
                        ClassModule classModule = new ClassModule();
                        classModule.setClassId(classId);
                        classModule.setModuleId(pm.getModule().getId());
                        classModule.setScheduleType(ClassModule.ScheduleType.fixed); 
                        return classModuleRepository.save(classModule);
                    }
                    return null;
                })
                .filter(cm -> cm != null)
                .collect(Collectors.toList());

        return createdModules.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClassModuleDTO createClassModule(ClassModuleDTO dto) {
        if (dto.getClassId() == null || dto.getModuleId() == null) {
            throw new BadRequestException("classId và moduleId là bắt buộc");
        }

        if (!classRepository.existsById(dto.getClassId())) {
            throw new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + dto.getClassId());
        }

        if (!moduleRepository.existsById(dto.getModuleId())) {
            throw new ResourceNotFoundException("Không tìm thấy module với ID: " + dto.getModuleId());
        }

        if (classModuleRepository.existsByClassIdAndModuleId(dto.getClassId(), dto.getModuleId())) {
            throw new ConflictException("Module đã được gán vào lớp học này");
        }

        ClassModule classModule = new ClassModule();
        classModule.setClassId(dto.getClassId());
        classModule.setModuleId(dto.getModuleId());
        classModule.setScheduleType(dto.getScheduleType() != null ? dto.getScheduleType() : ClassModule.ScheduleType.fixed);

        ClassModule saved = classModuleRepository.save(classModule);
        return convertToDTO(saved);
    }

    /**
     * Lấy danh sách ClassModule của một lớp
     */
    @Transactional(readOnly = true)
    public List<ClassModuleDTO> getClassModulesByClassId(Long classId) {
        if (!classRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId);
        }

        return classModuleRepository.findByClassId(classId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy ClassModule theo ID
     */
    @Transactional(readOnly = true)
    public ClassModuleDTO getClassModuleById(Long id) {
        ClassModule classModule = classModuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ClassModule với ID: " + id));
        return convertToDTO(classModule);
    }

    /**
     * Cập nhật ClassModule
     */
    @Transactional
    public ClassModuleDTO updateClassModule(Long id, ClassModuleDTO dto) {
        ClassModule classModule = classModuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ClassModule với ID: " + id));

        if (dto.getScheduleType() != null) {
            classModule.setScheduleType(dto.getScheduleType());
        }

        ClassModule updated = classModuleRepository.save(classModule);
        return convertToDTO(updated);
    }

    /**
     * Xóa ClassModule
     */
    @Transactional
    public void deleteClassModule(Long id) {
        if (!classModuleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy ClassModule với ID: " + id);
        }
        classModuleRepository.deleteById(id);
    }

    /**
     * Gán giáo viên vào ClassModule
     */
    @Transactional
    public ClassModuleTeacherDTO assignTeacherToClassModule(Long classModuleId, ClassModuleTeacherDTO dto) {
        if (!classModuleRepository.existsById(classModuleId)) {
            throw new ResourceNotFoundException("Không tìm thấy ClassModule với ID: " + classModuleId);
        }

        if (dto.getUserId() == null) {
            throw new BadRequestException("userId là bắt buộc");
        }

        if (!userRepository.existsById(dto.getUserId())) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + dto.getUserId());
        }

        if (classModuleTeacherRepository.existsByClassModuleIdAndUserId(classModuleId, dto.getUserId())) {
            throw new ConflictException("Giáo viên đã được gán vào ClassModule này");
        }

        ClassModuleTeacher teacher = new ClassModuleTeacher();
        teacher.setClassModuleId(classModuleId);
        teacher.setUserId(dto.getUserId());
        teacher.setRole(dto.getRole() != null ? dto.getRole() : ClassModuleTeacher.TeacherRole.TEACHER);

        ClassModuleTeacher saved = classModuleTeacherRepository.save(teacher);
        return convertTeacherToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ClassModuleTeacherDTO> getClassModuleTeachers(Long classModuleId) {
        if (!classModuleRepository.existsById(classModuleId)) {
            throw new ResourceNotFoundException("Không tìm thấy ClassModule với ID: " + classModuleId);
        }

        return classModuleTeacherRepository.findByClassModuleId(classModuleId).stream()
                .map(this::convertTeacherToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeTeacherFromClassModule(Long classModuleId, Long userId) {
        ClassModuleTeacher teacher = classModuleTeacherRepository
                .findByClassModuleIdAndUserId(classModuleId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo viên trong ClassModule này"));

        classModuleTeacherRepository.delete(teacher);
    }

    @Transactional
    public ClassModuleTeacherDTO updateTeacherRole(Long classModuleId, Long userId, ClassModuleTeacher.TeacherRole newRole) {
        ClassModuleTeacher teacher = classModuleTeacherRepository
                .findByClassModuleIdAndUserId(classModuleId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo viên trong ClassModule này"));

        teacher.setRole(newRole);
        ClassModuleTeacher updated = classModuleTeacherRepository.save(teacher);
        return convertTeacherToDTO(updated);
    }

    @Transactional
    public void syncClassModules(Long classId, List<Integer> desiredModuleIds) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId));

        LinkedHashSet<Integer> desiredIds = desiredModuleIds == null
                ? new LinkedHashSet<>()
                : desiredModuleIds.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

        List<ClassModule> existingModules = classModuleRepository.findByClassId(classId);

        for (ClassModule classModule : existingModules) {
            Integer moduleId = classModule.getModuleId();
            if (desiredIds.isEmpty() || moduleId == null || !desiredIds.contains(moduleId)) {
                classModuleRepository.delete(classModule);
            }
        }

        for (Integer moduleId : desiredIds) {
            if (!classModuleRepository.existsByClassIdAndModuleId(classId, moduleId)) {
                ClassModule classModule = new ClassModule();
                classModule.setClassId(classId);
                classModule.setModuleId(moduleId);
                classModule.setScheduleType(ClassModule.ScheduleType.fixed);
                classModuleRepository.save(classModule);
            }
        }
    }

    private ClassModuleDTO convertToDTO(ClassModule classModule) {
        ClassModuleDTO dto = new ClassModuleDTO();
        dto.setId(classModule.getId());
        dto.setClassId(classModule.getClassId());
        dto.setModuleId(classModule.getModuleId());
        dto.setScheduleType(classModule.getScheduleType());
        dto.setCreatedAt(classModule.getCreatedAt());

        if (classModule.getClassEntity() != null) {
            dto.setClassName(classModule.getClassEntity().getClassName());
        } else if (classModule.getClassId() != null) {
            classRepository.findById(classModule.getClassId())
                    .ifPresent(c -> dto.setClassName(c.getClassName()));
        }

        if (classModule.getModule() != null) {
            dto.setModuleName(classModule.getModule().getName());
        } else if (classModule.getModuleId() != null) {
            moduleRepository.findById(classModule.getModuleId())
                    .ifPresent(m -> dto.setModuleName(m.getName()));
        }

        if (classModule.getTeachers() != null && !classModule.getTeachers().isEmpty()) {
            List<ClassModuleTeacherDTO> teacherDTOs = classModule.getTeachers().stream()
                    .map(this::convertTeacherToDTO)
                    .collect(Collectors.toList());
            dto.setTeachers(teacherDTOs);
        }

        return dto;
    }

    private ClassModuleTeacherDTO convertTeacherToDTO(ClassModuleTeacher teacher) {
        ClassModuleTeacherDTO dto = new ClassModuleTeacherDTO();
        dto.setId(teacher.getId());
        dto.setClassModuleId(teacher.getClassModuleId());
        dto.setUserId(teacher.getUserId());
        dto.setRole(teacher.getRole());
        dto.setAssignedAt(teacher.getAssignedAt());

        if (teacher.getUser() != null) {
            dto.setUserName(teacher.getUser().getUsername());
            dto.setUserEmail(teacher.getUser().getEmail());
        } else if (teacher.getUserId() != null) {
            userRepository.findById(teacher.getUserId())
                    .ifPresent(user -> {
                        dto.setUserName(user.getUsername());
                        dto.setUserEmail(user.getEmail());
                    });
        }

        return dto;
    }
}


