package com.tim.appTim.service;

import com.tim.appTim.dto.CreateModuleSessionRequest;
import com.tim.appTim.dto.ModuleSessionDTO;
import com.tim.appTim.dto.UpdateModuleSessionRequest;
import com.tim.appTim.entity.ModuleSession;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.exception.ConflictException;
import com.tim.appTim.repository.ModuleRepository;
import com.tim.appTim.repository.ModuleSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.tim.appTim.dto.ModuleDTO;
import com.tim.appTim.entity.User;
import com.tim.appTim.repository.UserRepository;

@Service
public class ModuleSessionService {
    private final ModuleSessionRepository moduleSessionRepository;
    private final ModuleRepository moduleRepository;
    private final UserRepository userRepository;

    public ModuleSessionService(ModuleSessionRepository moduleSessionRepository, 
                               ModuleRepository moduleRepository,
                               UserRepository userRepository) {
        this.moduleSessionRepository = moduleSessionRepository;
        this.moduleRepository = moduleRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ModuleSessionDTO> getSessionsByModule(Integer moduleId) {
        if (!moduleRepository.existsById(moduleId)) {
            throw new ResourceNotFoundException("Không tìm thấy module với id = " + moduleId);
        }

        return moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(moduleId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ModuleSessionDTO getSessionById(Long sessionId) {
        ModuleSession session = moduleSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học với id = " + sessionId));
        return toDTO(session);
    }

    @Transactional
    public ModuleSessionDTO createSession(Integer moduleId, CreateModuleSessionRequest request) {
        if (!moduleRepository.existsById(moduleId)) {
            throw new ResourceNotFoundException("Không tìm thấy module với id = " + moduleId);
        }

        if (request.getSessionNumber() == null) {
            throw new BadRequestException("Thứ tự buổi học không được để trống");
        }

        boolean exists = moduleSessionRepository.findByModuleId(moduleId).stream()
                .anyMatch(s -> s.getSessionNumber().equals(request.getSessionNumber()));
        if (exists) {
            throw new BadRequestException("Buổi học " + request.getSessionNumber() + " đã tồn tại trong module này");
        }

        if (request.getScheduledAt() != null && request.getEndDate() != null) {
            List<ModuleSession> existingSessions = moduleSessionRepository.findByModuleId(moduleId);
            for (ModuleSession existingSession : existingSessions) {
                if (existingSession.getScheduledAt() != null && existingSession.getEndDate() != null) {
                    if (isTimeOverlapping(
                            request.getScheduledAt(), request.getEndDate(),
                            existingSession.getScheduledAt(), existingSession.getEndDate())) {
                        throw new BadRequestException(
                                String.format("Thời gian của buổi học này trùng với buổi học số %d (từ %s đến %s)",
                                        existingSession.getSessionNumber(),
                                        existingSession.getScheduledAt(),
                                        existingSession.getEndDate()));
                    }
                }
            }
        }

        ModuleSession session = new ModuleSession();
        session.setModuleId(moduleId);
        session.setSessionNumber(request.getSessionNumber());
        session.setTitle(request.getTitle());
        session.setContent(request.getContent());
        session.setScheduledAt(request.getScheduledAt());
        session.setEndDate(request.getEndDate());

        if (request.getInstructorId() != null) {
            User instructor = userRepository.findById(request.getInstructorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo viên với id = " + request.getInstructorId()));
            session.setInstructorId(request.getInstructorId());
        } else {

            moduleRepository.findById(moduleId).ifPresent(module -> {
                if (module.getInstructorId() != null) {
                    session.setInstructorId(module.getInstructorId());
                }
            });
        }

        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            try {
                session.setStatus(ModuleSession.SessionStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Status không hợp lệ. Chỉ chấp nhận: planned, ongoing, completed");
            }
        } else {
            session.setStatus(ModuleSession.SessionStatus.planned);
        }

        ModuleSession savedSession = moduleSessionRepository.save(session);
        return toDTO(savedSession);
    }

    @Transactional
    public ModuleSessionDTO updateSession(Long sessionId, UpdateModuleSessionRequest request) {
        ModuleSession session = moduleSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học với id = " + sessionId));

        if (request.getSessionNumber() != null) {
            boolean exists = moduleSessionRepository.findByModuleId(session.getModuleId()).stream()
                    .anyMatch(s -> !s.getId().equals(sessionId) && s.getSessionNumber().equals(request.getSessionNumber()));
            if (exists) {
                throw new ConflictException("Buổi học " + request.getSessionNumber() + " đã tồn tại trong module này");
            }
            session.setSessionNumber(request.getSessionNumber());
        }

        LocalDateTime newScheduledAt = request.getScheduledAt() != null ? request.getScheduledAt() : session.getScheduledAt();
        LocalDateTime newEndDate = request.getEndDate() != null ? request.getEndDate() : session.getEndDate();

        if (newScheduledAt != null && newEndDate != null) {
            List<ModuleSession> existingSessions = moduleSessionRepository.findByModuleId(session.getModuleId());
            for (ModuleSession existingSession : existingSessions) {
                if (existingSession.getId().equals(sessionId)) {
                    continue;
                }
                if (existingSession.getScheduledAt() != null && existingSession.getEndDate() != null) {
                    if (isTimeOverlapping(
                            newScheduledAt, newEndDate,
                            existingSession.getScheduledAt(), existingSession.getEndDate())) {
                        throw new BadRequestException(
                                String.format("Thời gian của buổi học này trùng với buổi học số %d (từ %s đến %s)",
                                        existingSession.getSessionNumber(),
                                        existingSession.getScheduledAt(),
                                        existingSession.getEndDate()));
                    }
                }
            }
        }

        if (request.getTitle() != null) {
            session.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            session.setContent(request.getContent());
        }
        if (request.getScheduledAt() != null) {
            session.setScheduledAt(request.getScheduledAt());
        }
        if (request.getEndDate() != null) {
            session.setEndDate(request.getEndDate());
        }
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            try {
                session.setStatus(ModuleSession.SessionStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Status không hợp lệ. Chỉ chấp nhận: planned, ongoing, completed");
            }
        }

        if (request.getInstructorId() != null) {
            User instructor = userRepository.findById(request.getInstructorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo viên với id = " + request.getInstructorId()));
            session.setInstructorId(request.getInstructorId());
        }

        ModuleSession updatedSession = moduleSessionRepository.save(session);
        return toDTO(updatedSession);
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        if (!moduleSessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Không tìm thấy buổi học với id = " + sessionId);
        }
        moduleSessionRepository.deleteById(sessionId);
    }

    @Transactional
    public ModuleSessionDTO assignInstructor(Long sessionId, Long instructorId) {
        ModuleSession session = moduleSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học với id = " + sessionId));

        if (instructorId != null) {
            User instructor = userRepository.findById(instructorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo viên với id = " + instructorId));
            session.setInstructorId(instructorId);
        } else {
            session.setInstructorId(null);
        }

        ModuleSession updatedSession = moduleSessionRepository.save(session);
        return toDTO(updatedSession);
    }

    @Transactional
    public ModuleDTO addSessionsToModule(Integer moduleId, List<Long> sessionIds) {
        com.tim.appTim.entity.Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy module với id = " + moduleId));
        List<ModuleSession> sessionsToAdd = moduleSessionRepository.findAllById(sessionIds);
        if (module.getModuleSessions() == null) {
            module.setModuleSessions(new java.util.ArrayList<>());
        }
        java.util.Set<Long> existingIds = module.getModuleSessions().stream().map(ModuleSession::getId).collect(java.util.stream.Collectors.toSet());
        for (ModuleSession s : sessionsToAdd) {
            if (!existingIds.contains(s.getId())) {
                s.setModuleId(moduleId);
                module.getModuleSessions().add(s);
                moduleSessionRepository.save(s);
            }
        }

        ModuleDTO dto = new ModuleDTO();
        dto.setId(module.getId());
        dto.setName(module.getName());
        dto.setDescription(module.getDescription());
        dto.setSessions(module.getModuleSessions().stream().map(this::toDTO).collect(java.util.stream.Collectors.toList()));
        return dto;
    }

    private ModuleSessionDTO toDTO(ModuleSession session) {
        ModuleSessionDTO dto = new ModuleSessionDTO();
        dto.setId(session.getId());
        dto.setModuleId(session.getModuleId());
        dto.setSessionNumber(session.getSessionNumber());
        dto.setTitle(session.getTitle());
        dto.setContent(session.getContent());
        dto.setScheduledAt(session.getScheduledAt());
        dto.setEndDate(session.getEndDate());
        dto.setStatus(session.getStatus() != null ? session.getStatus().name() : null);
        dto.setInstructorId(session.getInstructorId());

        if (session.getInstructorId() != null) {
            userRepository.findById(session.getInstructorId()).ifPresent(instructor -> {
                String fullName = instructor.getFirstName() + " " + instructor.getLastName();
                dto.setInstructorName(fullName.trim().isEmpty() ? instructor.getUsername() : fullName);
            });
        }

        return dto;
    }

    private boolean isTimeOverlapping(LocalDateTime start1, LocalDateTime end1, 
                                      LocalDateTime start2, LocalDateTime end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }

        if (!end1.isAfter(start1) || !end2.isAfter(start2)) {
            return false;
        }

        return start1.isBefore(end2) && end1.isAfter(start2);
    }
}
