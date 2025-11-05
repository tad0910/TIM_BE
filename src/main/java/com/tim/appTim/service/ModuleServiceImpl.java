package com.tim.appTim.service;

import com.tim.appTim.dto.ModuleDTO;
import com.tim.appTim.dto.ModuleSessionDTO;
import com.tim.appTim.entity.Module;
import com.tim.appTim.entity.ModuleSession;
import com.tim.appTim.repository.ModuleRepository;
import com.tim.appTim.repository.ModuleSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import com.tim.appTim.entity.User;
import com.tim.appTim.repository.UserRepository;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.exception.BadRequestException;

@Service
public class ModuleServiceImpl implements ModuleService {

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private ModuleSessionRepository moduleSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ModuleDTO> getAllModules() {
        return moduleRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ModuleDTO getModuleById(Integer id) {
        Module module = moduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Module not found with ID: " + id));

        return toDTO(module);
    }

    @Override
    @Transactional
    public ModuleDTO createModule(ModuleDTO dto) {
        Module module = new Module();
        module.setName(dto.getName());
        module.setDescription(dto.getDescription());

        if (dto.getInstructorId() != null) {
            User instructor = userRepository.findById(dto.getInstructorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo viên với id = " + dto.getInstructorId()));
            module.setInstructorId(dto.getInstructorId());
        }

        module = moduleRepository.save(module);

        if (dto.getSessions() != null && !dto.getSessions().isEmpty()) {
            List<ModuleSession> sessions = new ArrayList<>();
            for (int i = 0; i < dto.getSessions().size(); i++) {
                ModuleSessionDTO sessionDTO = dto.getSessions().get(i);
                ModuleSession session = new ModuleSession();
                session.setModuleId(module.getId());
                session.setSessionNumber(sessionDTO.getSessionNumber());
                session.setTitle(sessionDTO.getTitle());
                session.setContent(sessionDTO.getContent());
                session.setScheduledAt(sessionDTO.getScheduledAt());
                session.setEndDate(sessionDTO.getEndDate());

                if (sessionDTO.getScheduledAt() != null && sessionDTO.getEndDate() != null) {
                    if (sessionDTO.getEndDate().isBefore(sessionDTO.getScheduledAt()) || sessionDTO.getEndDate().isEqual(sessionDTO.getScheduledAt())) {
                        throw new BadRequestException("Thời gian kết thúc của session phải sau thời gian bắt đầu");
                    }
                }

                if (sessionDTO.getScheduledAt() != null && sessionDTO.getEndDate() != null) {
                    for (int j = 0; j < i; j++) {
                        ModuleSessionDTO otherSessionDTO = dto.getSessions().get(j);
                        if (otherSessionDTO.getScheduledAt() != null && otherSessionDTO.getEndDate() != null) {
                            if (isTimeOverlapping(
                                    sessionDTO.getScheduledAt(), sessionDTO.getEndDate(),
                                    otherSessionDTO.getScheduledAt(), otherSessionDTO.getEndDate())) {
                                throw new BadRequestException(
                                        String.format("Buổi học số %d trùng thời gian với buổi học số %d",
                                                sessionDTO.getSessionNumber(),
                                                otherSessionDTO.getSessionNumber()));
                            }
                        }
                    }
                }
                
                if (sessionDTO.getInstructorId() != null) {
                    User sessionInstructor = userRepository.findById(sessionDTO.getInstructorId())
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo viên với id = " + sessionDTO.getInstructorId()));
                    session.setInstructorId(sessionDTO.getInstructorId());
                } else if (module.getInstructorId() != null) {

                    session.setInstructorId(module.getInstructorId());
                }
                
                if (sessionDTO.getStatus() != null && !sessionDTO.getStatus().isEmpty()) {
                    try {
                        session.setStatus(ModuleSession.SessionStatus.valueOf(sessionDTO.getStatus()));
                    } catch (IllegalArgumentException e) {
                        session.setStatus(ModuleSession.SessionStatus.planned);
                    }
                } else {
                    session.setStatus(ModuleSession.SessionStatus.planned);
                }
                sessions.add(session);
            }
            moduleSessionRepository.saveAll(sessions);
        }

        return toDTO(module);
    }

    @Override
    @Transactional
    public ModuleDTO updateModule(Integer id, ModuleDTO dto) {
        Module module = moduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with ID: " + id));

        module.setName(dto.getName());
        module.setDescription(dto.getDescription());

        if (dto.getInstructorId() != null) {
            User instructor = userRepository.findById(dto.getInstructorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo viên với id = " + dto.getInstructorId()));
            module.setInstructorId(dto.getInstructorId());
        }

        module = moduleRepository.save(module);

        return toDTO(module);
    }

    @Override
    @Transactional
    public void deleteModule(Integer id) {
        if (!moduleRepository.existsById(id)) {
            throw new RuntimeException("Module not found with ID: " + id);
        }
        moduleRepository.deleteById(id);
    }

    @Override
    @Transactional
    public ModuleDTO assignInstructor(Integer moduleId, Long instructorId) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with ID: " + moduleId));

        if (instructorId != null) {
            User instructor = userRepository.findById(instructorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo viên với id = " + instructorId));
            module.setInstructorId(instructorId);
        } else {
            module.setInstructorId(null);
        }

        module = moduleRepository.save(module);
        return toDTO(module);
    }

    private ModuleDTO toDTO(Module module) {
        ModuleDTO dto = new ModuleDTO();
        dto.setId(module.getId());
        dto.setName(module.getName());
        dto.setDescription(module.getDescription());
        dto.setInstructorId(module.getInstructorId());

        if (module.getInstructorId() != null) {
            userRepository.findById(module.getInstructorId()).ifPresent(instructor -> {
                String fullName = instructor.getFirstName() + " " + instructor.getLastName();
                dto.setInstructorName(fullName.trim().isEmpty() ? instructor.getUsername() : fullName);
            });
        }

        List<ModuleSessionDTO> sessions = moduleSessionRepository
                .findByModuleIdOrderBySessionNumberAsc(module.getId())
                .stream()
                .map(this::sessionToDTO)
                .collect(Collectors.toList());
        dto.setSessions(sessions);

        return dto;
    }

    private ModuleSessionDTO sessionToDTO(ModuleSession session) {
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
