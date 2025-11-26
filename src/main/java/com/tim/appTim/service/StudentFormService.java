package com.tim.appTim.service;

import com.tim.appTim.dto.StudentFormCreateDTO;
import com.tim.appTim.dto.StudentFormResponseDTO;
import com.tim.appTim.dto.ApprovalRequestDTO;
import com.tim.appTim.entity.Class;
import com.tim.appTim.entity.ClassMember;
import com.tim.appTim.entity.FormTemplate;
import com.tim.appTim.entity.StudentForm;
import com.tim.appTim.entity.StudentForm.ApprovalStatus;
import com.tim.appTim.entity.StudentForm.FormStatus;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.exception.ForbiddenException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.ClassMemberRepository;
import com.tim.appTim.repository.ClassRepository;
import com.tim.appTim.repository.FormTemplateRepository;
import com.tim.appTim.repository.StudentFormRepository;
import com.tim.appTim.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentFormService {

    private final StudentFormRepository formRepo;
    private final FormTemplateRepository templateRepo;
    private final UserRepository userRepo;
    private final ClassRepository classRepo;
    private final ClassMemberRepository classMemberRepo;

    public StudentFormService(StudentFormRepository formRepo, FormTemplateRepository templateRepo,
         UserRepository userRepo, ClassRepository classRepo, ClassMemberRepository classMemberRepo) {
        this.formRepo = formRepo;
        this.templateRepo = templateRepo;
        this.userRepo = userRepo;
        this.classRepo = classRepo;
        this.classMemberRepo = classMemberRepo;
    }

    public List<FormTemplate> getAllActiveTemplates() {
        return templateRepo.findByIsActiveTrue();
    }
    
    @Transactional 
    public StudentFormResponseDTO approveForm(Long formId, User currentUser, ApprovalRequestDTO request) {
        
        StudentForm form = formRepo.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found"));
       
        String targetRole = determineTargetSection(currentUser, request.getTargetRole());

        switch (targetRole) { 
            case "ROLE_GIAO_VIEN":
                form.setCoachApproval(request.getDecision());
                form.setCoachNote(request.getNote());
                form.setCoachUser(currentUser); 
                break;

            case "ROLE_GIAO_VU":
                form.setAcademicApproval(request.getDecision());
                form.setAcademicNote(request.getNote());
                form.setAcademicUser(currentUser);
                break;

            case "ROLE_KE_TOAN":
                form.setAccountantApproval(request.getDecision());
                form.setAccountantNote(request.getNote());
                form.setAccountantUser(currentUser);
                break;

            case "ROLE_ADMIN":
                form.setAdminApproval(request.getDecision());
                form.setAdminNote(request.getNote());
                form.setAdminUser(currentUser);
                break;

            default:
                throw new ForbiddenException("Bạn không có quyền duyệt đơn này!");
        }

        updateOverallStatus(form);

        return mapToDTO(formRepo.save(form));
    }


    private String determineTargetSection(User user, String requestedTarget) {
        String userRole = user.getRoles().stream()
                .findFirst()
                .map(role -> role.getName())
                .orElseThrow(() -> new ForbiddenException("User chưa được gán quyền hạn (Role)"));

        if ("ROLE_ADMIN".equals(userRole)) {

            if (requestedTarget == null || requestedTarget.isBlank()) {
                return "ROLE_ADMIN";
            }

            String upperTarget = requestedTarget.toUpperCase();
            List<String> validTargets = List.of("ROLE_GIAO_VIEN", "ROLE_GIAO_VU", "ROLE_KE_TOAN", "ROLE_ADMIN");
            
            if (validTargets.contains(upperTarget)) {
                return upperTarget; 
            }
            
            throw new BadRequestException("Target Role không hợp lệ. Admin chỉ được chọn: " + validTargets);
        }

        if (userRole.equals("ROLE_GIAO_VIEN")) return "ROLE_GIAO_VIEN";
        if (userRole.equals("ROLE_GIAO_VU"))   return "ROLE_GIAO_VU";
        if (userRole.equals("ROLE_KE_TOAN"))   return "ROLE_KE_TOAN";

        throw new ForbiddenException("Tài khoản của bạn (" + userRole + ") không có quyền duyệt đơn này.");
    }

    

    @Transactional
    public StudentFormResponseDTO createForm(StudentFormCreateDTO dto, User creator) {
        FormTemplate template = templateRepo.findById(dto.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Mẫu đơn không tồn tại"));

        User student = userRepo.findById(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Học sinh không tồn tại"));

        if (dto.getFullName() != null && !dto.getFullName().isBlank()) {

            String dbLastName = student.getLastName() == null ? "" : student.getLastName();
            String dbFirstName = student.getFirstName() == null ? "" : student.getFirstName();
            String dbFullName = (dbLastName + " " + dbFirstName).trim();
            String inputFullName = dto.getFullName().trim();

            if (!dbFullName.equalsIgnoreCase(inputFullName)) {
                throw new BadRequestException(
                    "Dữ liệu không hợp lệ: Tên '" + inputFullName + 
                    "' không khớp với mã học viên ID " + dto.getStudentId()
                );
            }
        }

        Class currentClass = classRepo.findById(dto.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học không tồn tại"));
        
        StudentForm form = new StudentForm();
        form.setTemplate(template);
        form.setStudent(student);
        form.setClassRoom(currentClass);
        form.setCreatedBy(creator); 
        form.setReason(dto.getReason());
        form.setStartDate(dto.getStartDate());
        form.setEndDate(dto.getEndDate());
        form.setFeeAmount(dto.getFeeAmount());
        form.setStatus(FormStatus.PENDING); 
        
        StudentForm savedForm = formRepo.save(form);
        return mapToDTO(savedForm);
    }

    private void updateOverallStatus(StudentForm form) {
        boolean isRejected = 
            form.getCoachApproval() == ApprovalStatus.REJECTED ||
            form.getAcademicApproval() == ApprovalStatus.REJECTED ||
            form.getAccountantApproval() == ApprovalStatus.REJECTED ||
            form.getAdminApproval() == ApprovalStatus.REJECTED;

        if (isRejected) {
            form.setStatus(StudentForm.FormStatus.REJECTED);
            return;
        }

        boolean isAllApproved = 
            form.getCoachApproval() == ApprovalStatus.APPROVED &&
            form.getAcademicApproval() == ApprovalStatus.APPROVED &&
            form.getAccountantApproval() == ApprovalStatus.APPROVED &&
            form.getAdminApproval() == ApprovalStatus.APPROVED;

        if (isAllApproved) {
            form.setStatus(StudentForm.FormStatus.APPROVED);
        } else {
            form.setStatus(StudentForm.FormStatus.PROCESSING);
        }
    }


    @Transactional
    public void deleteForm(Long formId) {
        if (!formRepo.existsById(formId)) {
            throw new ResourceNotFoundException("Đơn không tồn tại");
        }
        formRepo.deleteById(formId);
    }

    @Transactional(readOnly = true)
    public StudentFormResponseDTO getFormDetail(Long id) {
        StudentForm form = formRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found"));
        return mapToDTO(form);
    }

    @Transactional(readOnly = true)
    public List<StudentFormResponseDTO> getAllForms(User currentUser) {
        List<StudentForm> forms;
        String userRole = currentUser.getRoles().stream()
                .findFirst()
                .map(r -> r.getName())
                .orElse("");

        if ("ROLE_GIAO_VIEN".equals(userRole)) {
            List<Long> classIds = classMemberRepo.findClassIdsByUserIdAndRole(
                    currentUser.getId(), 
                    ClassMember.Role.giao_vien 
            );

            if (classIds.isEmpty()) {
                return Collections.emptyList();
            }

            forms = formRepo.findByClassRoomIdIn(classIds);
        } else {

            forms = formRepo.findAll();
        }

        return forms.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public StudentFormResponseDTO mapToDTO(StudentForm form) {
        StudentFormResponseDTO dto = new StudentFormResponseDTO();

        dto.setId(form.getId());
        dto.setReason(form.getReason());
        dto.setStartDate(form.getStartDate());
        dto.setEndDate(form.getEndDate());
        dto.setFeeAmount(form.getFeeAmount());
        dto.setStatus(form.getStatus());
        dto.setCreatedAt(form.getCreatedAt());

        if (form.getTemplate() != null) {
            dto.setTemplateName(form.getTemplate().getName());
        }

        if (form.getStudent() != null) {
            dto.setStudentId(form.getStudent().getId()); 
            dto.setStudentName(form.getStudent().getLastName() + " " + form.getStudent().getFirstName());
            dto.setPhoneNumber(form.getStudent().getPhoneNumber());
            dto.setEmail(form.getStudent().getEmail());
        }

        if (form.getClassRoom() != null) {
            dto.setClassName(form.getClassRoom().getClassName());
            if (form.getClassRoom().getProgram() != null) {
                dto.setProgramName(form.getClassRoom().getProgram().getName());
            }
        }

        dto.setCoachApproval(form.getCoachApproval());
        dto.setCoachNote(form.getCoachNote());
        if (form.getCoachUser() != null) {
            dto.setCoachName(form.getCoachUser().getLastName() + " " + form.getCoachUser().getFirstName());
        }

        dto.setAcademicApproval(form.getAcademicApproval());
        dto.setAcademicNote(form.getAcademicNote());
        if (form.getAcademicUser() != null) {
            dto.setAcademicName(form.getAcademicUser().getLastName() + " " + form.getAcademicUser().getFirstName());
        }

        dto.setAccountantApproval(form.getAccountantApproval());
        dto.setAccountantNote(form.getAccountantNote());
        if (form.getAccountantUser() != null) {
            dto.setAccountantName(form.getAccountantUser().getLastName() + " " + form.getAccountantUser().getFirstName());
        }

        dto.setAdminApproval(form.getAdminApproval());
        dto.setAdminNote(form.getAdminNote());
        if (form.getAdminUser() != null) {
            dto.setAdminName(form.getAdminUser().getLastName() + " " + form.getAdminUser().getFirstName());
        }
        
        return dto;
    }
}