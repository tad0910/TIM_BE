package com.tim.appTim.service;

import com.tim.appTim.dto.ClassModuleScheduleDTO;
import com.tim.appTim.entity.ClassModuleSchedule;
import com.tim.appTim.entity.Module;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.exception.InvalidRequestException;
import com.tim.appTim.repository.ClassModuleScheduleRepository;
import com.tim.appTim.repository.ModuleRepository;
import com.tim.appTim.repository.ClassRepository;
import com.tim.appTim.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClassModuleScheduleService {

    private final ClassModuleScheduleRepository scheduleRepository;
    private final ModuleRepository moduleRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;

    public ClassModuleScheduleService(ClassModuleScheduleRepository scheduleRepository,
                                      ModuleRepository moduleRepository,
                                      ClassRepository classRepository,
                                      UserRepository userRepository) {
        this.scheduleRepository = scheduleRepository;
        this.moduleRepository = moduleRepository;
        this.classRepository = classRepository;
        this.userRepository = userRepository;
    }

    private ClassModuleScheduleDTO convertToDTO(ClassModuleSchedule entity) {
        ClassModuleScheduleDTO dto = new ClassModuleScheduleDTO();

        dto.setId(entity.getId());
        dto.setClassId(entity.getClassId());
        dto.setModuleId(entity.getModuleId());
        dto.setInstructorId(entity.getInstructorId());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setStatus(entity.getStatus());

        if (entity.getModule() != null) {
            dto.setModuleName(entity.getModule().getName());
        } else {
            Optional<Module> moduleOpt = moduleRepository.findById(entity.getModuleId().intValue());
            moduleOpt.ifPresent(module -> dto.setModuleName(module.getName()));
        }

        if (entity.getInstructor() != null) {
            dto.setInstructorName(entity.getInstructor().getUsername());
        } else if (entity.getInstructorId() != null) {
            Optional<User> userOpt = userRepository.findById(entity.getInstructorId());
            userOpt.ifPresent(user -> dto.setInstructorName(user.getUsername()));
        }

        if (entity.getClassEntity() != null) {

        }

        return dto;
    }

    private void checkInstructorConflict(Long instructorId, LocalDate newStartDate, LocalDate newEndDate, Long currentScheduleId) {
        if (instructorId == null) return;

        Long scheduleIdForQuery = currentScheduleId != null ? currentScheduleId : 0L;

        List<ClassModuleSchedule> conflictingSchedules = scheduleRepository.findConflictingSchedules(
                instructorId,
                newStartDate,
                newEndDate,
                scheduleIdForQuery
        );

        if (!conflictingSchedules.isEmpty()) {
            Long conflictingModuleId = conflictingSchedules.get(0).getModuleId();
            String moduleName = moduleRepository.findById(conflictingModuleId.intValue())
                    .map(Module::getName).orElse("Module khác");

            throw new InvalidRequestException("Giảng viên đã bị trùng lịch với " + moduleName +
                    " từ ngày " + conflictingSchedules.get(0).getStartDate());
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidRequestException("Ngày bắt đầu và ngày kết thúc là bắt buộc.");
        }
        if (endDate.isBefore(startDate)) {
            throw new InvalidRequestException("Ngày kết thúc không thể trước ngày bắt đầu.");
        }
    }

    public ClassModuleScheduleDTO createSchedule(ClassModuleScheduleDTO dto) {
        if (!classRepository.existsById(dto.getClassId())) {
            throw new ResourceNotFoundException("Lớp học không tồn tại với ID: " + dto.getClassId());
        }
        if (!moduleRepository.existsById(dto.getModuleId().intValue())) {
            throw new ResourceNotFoundException("Module không tồn tại với ID: " + dto.getModuleId());
        }
        if (dto.getInstructorId() != null && !userRepository.existsById(dto.getInstructorId())) {
            throw new ResourceNotFoundException("Giảng viên không tồn tại với ID: " + dto.getInstructorId());
        }

        validateDateRange(dto.getStartDate(), dto.getEndDate());

        if (scheduleRepository.existsByClassIdAndModuleId(dto.getClassId(), dto.getModuleId())) {
            throw new InvalidRequestException("Module này đã được lập lịch cho lớp học này.");
        }

        checkInstructorConflict(dto.getInstructorId(), dto.getStartDate(), dto.getEndDate(), null);

        ClassModuleSchedule entity = new ClassModuleSchedule();
        entity.setClassId(dto.getClassId());
        entity.setModuleId(dto.getModuleId());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setInstructorId(dto.getInstructorId());
        entity.setStatus(ClassModuleSchedule.ScheduleStatus.planned);

        ClassModuleSchedule savedEntity = scheduleRepository.save(entity);
        return convertToDTO(savedEntity);
    }

    public ClassModuleScheduleDTO updateSchedule(Long scheduleId, ClassModuleScheduleDTO dto) {
        ClassModuleSchedule existingSchedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Lịch học không tồn tại với ID: " + scheduleId));

        validateDateRange(dto.getStartDate(), dto.getEndDate());

        checkInstructorConflict(dto.getInstructorId(), dto.getStartDate(), dto.getEndDate(), scheduleId);

        existingSchedule.setStartDate(dto.getStartDate());
        existingSchedule.setEndDate(dto.getEndDate());

        if (dto.getInstructorId() != null) {
            existingSchedule.setInstructorId(dto.getInstructorId());
        }

        if (dto.getStatus() != null) {
            existingSchedule.setStatus(dto.getStatus());
        }

        ClassModuleSchedule updatedEntity = scheduleRepository.save(existingSchedule);
        return convertToDTO(updatedEntity);
    }

    public List<ClassModuleScheduleDTO> getSchedulesByClass(Long classId) {
        return scheduleRepository.findByClassId(classId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ClassModuleScheduleDTO> getSchedulesByInstructor(Long instructorId) {
        if (!userRepository.existsById(instructorId)) {
            throw new ResourceNotFoundException("Giảng viên không tồn tại với ID: " + instructorId);
        }
        return scheduleRepository.findByInstructorId(instructorId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public void deleteSchedule(Long scheduleId) {
        if (!scheduleRepository.existsById(scheduleId)) {
            throw new ResourceNotFoundException("Lịch học không tồn tại với ID: " + scheduleId);
        }
        scheduleRepository.deleteById(scheduleId);
    }

    public List<ClassModuleScheduleDTO> getSchedulesByClass(Long classId, LocalDate startDate, LocalDate endDate) {
        List<ClassModuleSchedule> entities;

        if (startDate != null && endDate != null) {
            // Lọc theo khoảng ngày (khi FE gửi tham số)
            entities = scheduleRepository.findByClassIdAndStartDateBetween(classId, startDate, endDate);
        } else {
            // Lấy tất cả lịch học của lớp (khi FE không gửi tham số, mặc định)
            entities = scheduleRepository.findByClassId(classId);
        }

        return entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ClassModuleScheduleDTO> getSchedulesByInstructor(Long instructorId, LocalDate startDate, LocalDate endDate) {
        if (!userRepository.existsById(instructorId)) {
            throw new ResourceNotFoundException("Giảng viên không tồn tại với ID: " + instructorId);
        }

        List<ClassModuleSchedule> entities;

        if (startDate != null && endDate != null) {
            entities = scheduleRepository.findByInstructorIdAndStartDateBetween(instructorId, startDate, endDate);
        } else {
            entities = scheduleRepository.findByInstructorId(instructorId);
        }

        return entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}