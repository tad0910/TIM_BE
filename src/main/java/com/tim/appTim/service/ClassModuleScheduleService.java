package com.tim.appTim.service;

import com.tim.appTim.dto.ClassModuleScheduleDTO;
import com.tim.appTim.entity.ClassModuleSchedule;
import com.tim.appTim.entity.Module;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.exception.InvalidRequestException;
import com.tim.appTim.repository.ClassModuleScheduleRepository;
import com.tim.appTim.repository.ModuleRepository;
import com.tim.appTim.repository.ModuleSessionRepository;
import com.tim.appTim.repository.ClassRepository;
import com.tim.appTim.repository.UserRepository;
import com.tim.appTim.repository.ClassModuleRepository;
import com.tim.appTim.repository.ClassModuleScheduleTeacherRepository;
import com.tim.appTim.entity.ModuleSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClassModuleScheduleService {

    private final ClassModuleScheduleRepository scheduleRepository;
    private final ModuleRepository moduleRepository;
    private final ModuleSessionRepository moduleSessionRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final ClassModuleRepository classModuleRepository;
    private final ClassModuleScheduleTeacherRepository scheduleTeacherRepository;

    public ClassModuleScheduleService(ClassModuleScheduleRepository scheduleRepository,
                                      ModuleRepository moduleRepository,
                                      ModuleSessionRepository moduleSessionRepository,
                                      ClassRepository classRepository,
                                      UserRepository userRepository,
                                      ClassModuleRepository classModuleRepository,
                                      ClassModuleScheduleTeacherRepository scheduleTeacherRepository) {
        this.scheduleRepository = scheduleRepository;
        this.moduleRepository = moduleRepository;
        this.moduleSessionRepository = moduleSessionRepository;
        this.classRepository = classRepository;
        this.userRepository = userRepository;
        this.classModuleRepository = classModuleRepository;
        this.scheduleTeacherRepository = scheduleTeacherRepository;
    }

    private ClassModuleScheduleDTO convertToDTO(ClassModuleSchedule entity) {
        ClassModuleScheduleDTO dto = new ClassModuleScheduleDTO();

        dto.setId(entity.getId());
        dto.setClassId(entity.getClassId());
        dto.setModuleId(entity.getModuleId());
        dto.setClassModuleId(entity.getClassModuleId());
        dto.setModuleSessionId(entity.getModuleSessionId());
        dto.setInstructorId(entity.getInstructorId());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setStatus(entity.getStatus());

        if (entity.getClassEntity() != null) {
            dto.setClassName(entity.getClassEntity().getClassName());
        }

        if (entity.getModule() != null) {
            dto.setModuleName(entity.getModule().getName());
        } else if (entity.getModuleId() != null) {
            moduleRepository.findById(entity.getModuleId())
                    .ifPresent(module -> dto.setModuleName(module.getName()));
        }

        if (entity.getInstructor() != null) {
            dto.setInstructorName(entity.getInstructor().getUsername());
        } else if (entity.getInstructorId() != null) {
            Optional<User> userOpt = userRepository.findById(entity.getInstructorId());
            userOpt.ifPresent(user -> dto.setInstructorName(user.getUsername()));
        }

        return dto;
    }

    private void checkInstructorConflict(Long instructorId, LocalDateTime newStartDate, LocalDateTime newEndDate, Long currentScheduleId) {
        if (instructorId == null) return;

        Long scheduleIdForQuery = currentScheduleId != null ? currentScheduleId : 0L;

        List<ClassModuleSchedule> conflictingSchedules = scheduleRepository.findConflictingSchedules(
                instructorId,
                newStartDate,
                newEndDate,
                scheduleIdForQuery
        );

        if (!conflictingSchedules.isEmpty()) {
            Integer conflictingModuleId = conflictingSchedules.get(0).getModuleId();
            String moduleName = moduleRepository.findById(conflictingModuleId)
                    .map(Module::getName).orElse("Module khác");

            throw new InvalidRequestException("Giảng viên đã bị trùng lịch với " + moduleName +
                    " từ ngày " + conflictingSchedules.get(0).getStartDate());
        }
    }

    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
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
        if (!moduleRepository.existsById(dto.getModuleId())) {
            throw new ResourceNotFoundException("Module không tồn tại với ID: " + dto.getModuleId());
        }
        if (dto.getInstructorId() != null && !userRepository.existsById(dto.getInstructorId())) {
            throw new ResourceNotFoundException("Giảng viên không tồn tại với ID: " + dto.getInstructorId());
        }

        validateDateRange(dto.getStartDate(), dto.getEndDate());

        if (dto.getClassModuleId() != null) {
            if (!classModuleRepository.existsById(dto.getClassModuleId())) {
                throw new ResourceNotFoundException("ClassModule không tồn tại với ID: " + dto.getClassModuleId());
            }
        }

        if (dto.getModuleSessionId() != null) {

            boolean existsForSession = scheduleRepository.findByClassId(dto.getClassId()).stream()
                    .anyMatch(s -> s.getModuleSessionId() != null && s.getModuleSessionId().equals(dto.getModuleSessionId()));
            if (existsForSession) {
                throw new InvalidRequestException("ModuleSession này đã được lập lịch cho lớp học này.");
            }

            checkInstructorConflict(dto.getInstructorId(), dto.getStartDate(), dto.getEndDate(), null);

            ClassModuleSchedule entity = new ClassModuleSchedule();
            entity.setClassId(dto.getClassId());
            entity.setModuleId(dto.getModuleId());
            entity.setClassModuleId(dto.getClassModuleId());
            entity.setModuleSessionId(dto.getModuleSessionId());
            entity.setStartDate(dto.getStartDate());
            entity.setEndDate(dto.getEndDate());
            entity.setInstructorId(dto.getInstructorId());
            entity.setStatus(ClassModuleSchedule.ScheduleStatus.planned);

            ClassModuleSchedule savedEntity = scheduleRepository.save(entity);
            return convertToDTO(savedEntity);
        }

        if (dto.getInstructorId() != null) {

            List<ModuleSession> moduleSessions = moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(dto.getModuleId());
            
            if (moduleSessions.isEmpty()) {
                throw new InvalidRequestException("Module này chưa có ModuleSession nào.");
            }

            List<ClassModuleSchedule> existingSchedules = scheduleRepository.findByClassId(dto.getClassId());
            java.util.Set<Long> existingSessionIds = existingSchedules.stream()
                    .filter(s -> s.getModuleId().equals(dto.getModuleId()) && s.getModuleSessionId() != null)
                    .map(ClassModuleSchedule::getModuleSessionId)
                    .collect(java.util.stream.Collectors.toSet());

            boolean moduleScheduledWithoutSession = existingSchedules.stream()
                    .anyMatch(s -> s.getModuleId().equals(dto.getModuleId()) && s.getModuleSessionId() == null);
            
            if (moduleScheduledWithoutSession || !existingSessionIds.isEmpty()) {
                throw new InvalidRequestException("Module này đã được lập lịch cho lớp học này. " +
                        (existingSessionIds.isEmpty() ? "" : "Đã có schedule cho " + existingSessionIds.size() + " ModuleSession."));
            }

            checkInstructorConflict(dto.getInstructorId(), dto.getStartDate(), dto.getEndDate(), null);

            List<ClassModuleSchedule> schedules = new java.util.ArrayList<>();
            for (ModuleSession session : moduleSessions) {
                ClassModuleSchedule entity = new ClassModuleSchedule();
                entity.setClassId(dto.getClassId());
                entity.setModuleId(dto.getModuleId());
                entity.setClassModuleId(dto.getClassModuleId());
                entity.setModuleSessionId(session.getId());
                entity.setStartDate(dto.getStartDate());
                entity.setEndDate(dto.getEndDate());
                entity.setInstructorId(dto.getInstructorId());
                entity.setStatus(ClassModuleSchedule.ScheduleStatus.planned);
                schedules.add(entity);
            }

            List<ClassModuleSchedule> savedSchedules = scheduleRepository.saveAll(schedules);
            return convertToDTO(savedSchedules.get(0));
        }

        if (scheduleRepository.existsByClassIdAndModuleId(dto.getClassId(), dto.getModuleId())) {
            throw new InvalidRequestException("Module này đã được lập lịch cho lớp học này.");
        }

        ClassModuleSchedule entity = new ClassModuleSchedule();
        entity.setClassId(dto.getClassId());
        entity.setModuleId(dto.getModuleId());
        entity.setClassModuleId(dto.getClassModuleId());
        entity.setModuleSessionId(null);
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setInstructorId(null);
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

        if (dto.getClassModuleId() != null) {
            if (!classModuleRepository.existsById(dto.getClassModuleId())) {
                throw new ResourceNotFoundException("ClassModule không tồn tại với ID: " + dto.getClassModuleId());
            }
            existingSchedule.setClassModuleId(dto.getClassModuleId());
        }

        if (dto.getModuleSessionId() != null) {
            existingSchedule.setModuleSessionId(dto.getModuleSessionId());
        }

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

    public List<ClassModuleScheduleDTO> getSchedulesByClass(Long classId, LocalDateTime startDate, LocalDateTime endDate) {
        List<ClassModuleSchedule> entities;

        if (startDate != null && endDate != null) {
            entities = scheduleRepository.findByClassIdAndStartDateBetween(classId, startDate, endDate);
        } else {
            entities = scheduleRepository.findByClassId(classId);
        }

        return entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ClassModuleScheduleDTO> getSchedulesByInstructor(Long instructorId, LocalDateTime startDate, LocalDateTime endDate) {
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

    public List<ClassModuleScheduleDTO> getAllSchedulesByTeacher(Long teacherId, LocalDateTime startDate, LocalDateTime endDate) {
        if (!userRepository.existsById(teacherId)) {
            throw new ResourceNotFoundException("Giảng viên không tồn tại với ID: " + teacherId);
        }

        java.util.Set<Long> scheduleIds = new java.util.HashSet<>();

        List<ClassModuleSchedule> mainInstructorSchedules;
        if (startDate != null && endDate != null) {
            mainInstructorSchedules = scheduleRepository.findByInstructorIdAndStartDateBetween(teacherId, startDate, endDate);
        } else {
            mainInstructorSchedules = scheduleRepository.findByInstructorId(teacherId);
        }
        mainInstructorSchedules.forEach(s -> scheduleIds.add(s.getId()));

        List<com.tim.appTim.entity.ClassModuleScheduleTeacher> teacherAssignments =
            scheduleTeacherRepository.findByUserId(teacherId);
        
        teacherAssignments.forEach(assignment -> {
            Long scheduleId = assignment.getClassModuleScheduleId();
            if (scheduleId != null) {
                if (startDate != null && endDate != null) {
                    scheduleRepository.findById(scheduleId)
                        .filter(s -> {
                            if (s.getStartDate() == null || s.getEndDate() == null) return false;
                            return !s.getStartDate().isAfter(endDate) && !s.getEndDate().isBefore(startDate);
                        })
                        .ifPresent(s -> scheduleIds.add(scheduleId));
                } else {
                    scheduleIds.add(scheduleId);
                }
            }
        });

        List<ClassModuleSchedule> allSchedules = scheduleIds.stream()
                .map(scheduleRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toList());

        return allSchedules.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}