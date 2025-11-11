package com.tim.appTim.service;

import com.tim.appTim.dto.ClassModuleScheduleTeacherDTO;
import com.tim.appTim.entity.ClassModuleScheduleTeacher;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.exception.ConflictException;
import com.tim.appTim.repository.ClassModuleScheduleTeacherRepository;
import com.tim.appTim.repository.ClassModuleScheduleRepository;
import com.tim.appTim.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClassModuleScheduleTeacherService {

    private final ClassModuleScheduleTeacherRepository scheduleTeacherRepository;
    private final ClassModuleScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    public ClassModuleScheduleTeacherService(ClassModuleScheduleTeacherRepository scheduleTeacherRepository,
                                             ClassModuleScheduleRepository scheduleRepository,
                                             UserRepository userRepository) {
        this.scheduleTeacherRepository = scheduleTeacherRepository;
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
    }

    /**
     * Gán giáo viên vào ClassModuleSchedule
     */
    @Transactional
    public ClassModuleScheduleTeacherDTO assignTeacherToSchedule(Long scheduleId, ClassModuleScheduleTeacherDTO dto) {
        if (!scheduleRepository.existsById(scheduleId)) {
            throw new ResourceNotFoundException("Không tìm thấy ClassModuleSchedule với ID: " + scheduleId);
        }

        if (dto.getUserId() == null) {
            throw new BadRequestException("userId là bắt buộc");
        }

        if (!userRepository.existsById(dto.getUserId())) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + dto.getUserId());
        }

        if (scheduleTeacherRepository.existsByClassModuleScheduleIdAndUserId(scheduleId, dto.getUserId())) {
            throw new ConflictException("Giáo viên đã được gán vào buổi học này");
        }

        ClassModuleScheduleTeacher teacher = new ClassModuleScheduleTeacher();
        teacher.setClassModuleScheduleId(scheduleId);
        teacher.setUserId(dto.getUserId());
        teacher.setRole(dto.getRole() != null ? dto.getRole() : ClassModuleScheduleTeacher.ScheduleTeacherRole.LECTURER);

        ClassModuleScheduleTeacher saved = scheduleTeacherRepository.save(teacher);
        return convertToDTO(saved);
    }

    /**
     * Lấy danh sách giáo viên của ClassModuleSchedule
     */
    @Transactional(readOnly = true)
    public List<ClassModuleScheduleTeacherDTO> getScheduleTeachers(Long scheduleId) {
        if (!scheduleRepository.existsById(scheduleId)) {
            throw new ResourceNotFoundException("Không tìm thấy ClassModuleSchedule với ID: " + scheduleId);
        }

        return scheduleTeacherRepository.findByClassModuleScheduleId(scheduleId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Xóa giáo viên khỏi ClassModuleSchedule
     */
    @Transactional
    public void removeTeacherFromSchedule(Long scheduleId, Long userId) {
        ClassModuleScheduleTeacher teacher = scheduleTeacherRepository
                .findByClassModuleScheduleIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo viên trong buổi học này"));

        scheduleTeacherRepository.delete(teacher);
    }

    /**
     * Cập nhật vai trò giáo viên trong ClassModuleSchedule
     */
    @Transactional
    public ClassModuleScheduleTeacherDTO updateTeacherRole(Long scheduleId, Long userId, 
                                                           ClassModuleScheduleTeacher.ScheduleTeacherRole newRole) {
        ClassModuleScheduleTeacher teacher = scheduleTeacherRepository
                .findByClassModuleScheduleIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo viên trong buổi học này"));

        teacher.setRole(newRole);
        ClassModuleScheduleTeacher updated = scheduleTeacherRepository.save(teacher);
        return convertToDTO(updated);
    }

    private ClassModuleScheduleTeacherDTO convertToDTO(ClassModuleScheduleTeacher teacher) {
        ClassModuleScheduleTeacherDTO dto = new ClassModuleScheduleTeacherDTO();
        dto.setId(teacher.getId());
        dto.setClassModuleScheduleId(teacher.getClassModuleScheduleId());
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

