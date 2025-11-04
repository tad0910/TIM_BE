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

import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import com.tim.appTim.entity.User;
import com.tim.appTim.repository.UserRepository;
import com.tim.appTim.exception.ResourceNotFoundException;

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

        // Validate và set instructor
        if (dto.getInstructorId() != null) {
            User instructor = userRepository.findById(dto.getInstructorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo viên với id = " + dto.getInstructorId()));
            module.setInstructorId(dto.getInstructorId());
        }

        module = moduleRepository.save(module);

        // Tạo sessions nếu có trong request
        if (dto.getSessions() != null && !dto.getSessions().isEmpty()) {
            List<ModuleSession> sessions = new ArrayList<>();
            for (ModuleSessionDTO sessionDTO : dto.getSessions()) {
                ModuleSession session = new ModuleSession();
                session.setModuleId(module.getId());
                session.setSessionNumber(sessionDTO.getSessionNumber());
                session.setTitle(sessionDTO.getTitle());
                session.setContent(sessionDTO.getContent());
                session.setScheduledAt(sessionDTO.getScheduledAt());
                session.setEndDate(sessionDTO.getEndDate());
                
                // Set instructor cho session (nếu có, nếu không thì dùng instructor của module)
                if (sessionDTO.getInstructorId() != null) {
                    User sessionInstructor = userRepository.findById(sessionDTO.getInstructorId())
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo viên với id = " + sessionDTO.getInstructorId()));
                    session.setInstructorId(sessionDTO.getInstructorId());
                } else if (module.getInstructorId() != null) {
                    // Nếu session không có instructor, dùng instructor của module
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

        // Validate và update instructor nếu được cung cấp
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

    /**
     * Convert Module entity to ModuleDTO with sessions
     */
    private ModuleDTO toDTO(Module module) {
        ModuleDTO dto = new ModuleDTO();
        dto.setId(module.getId());
        dto.setName(module.getName());
        dto.setDescription(module.getDescription());
        dto.setInstructorId(module.getInstructorId());

        // Load instructor name if exists
        if (module.getInstructorId() != null) {
            userRepository.findById(module.getInstructorId()).ifPresent(instructor -> {
                String fullName = instructor.getFirstName() + " " + instructor.getLastName();
                dto.setInstructorName(fullName.trim().isEmpty() ? instructor.getUsername() : fullName);
            });
        }

        // Load sessions for this module
        List<ModuleSessionDTO> sessions = moduleSessionRepository
                .findByModuleIdOrderBySessionNumberAsc(module.getId())
                .stream()
                .map(this::sessionToDTO)
                .collect(Collectors.toList());
        dto.setSessions(sessions);

        return dto;
    }

    /**
     * Convert ModuleSession entity to ModuleSessionDTO
     */
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

        // Load instructor name if exists
        if (session.getInstructorId() != null) {
            userRepository.findById(session.getInstructorId()).ifPresent(instructor -> {
                String fullName = instructor.getFirstName() + " " + instructor.getLastName();
                dto.setInstructorName(fullName.trim().isEmpty() ? instructor.getUsername() : fullName);
            });
        }

        return dto;
    }
}
