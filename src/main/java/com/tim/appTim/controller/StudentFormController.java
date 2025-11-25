package com.tim.appTim.controller;

import com.tim.appTim.dto.ApprovalRequestDTO;
import com.tim.appTim.dto.StudentFormCreateDTO;
import com.tim.appTim.dto.StudentFormResponseDTO;
import com.tim.appTim.entity.FormTemplate;
import com.tim.appTim.entity.User;
import com.tim.appTim.service.StudentFormService;
import com.tim.appTim.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/forms")
public class StudentFormController {

    private final StudentFormService formService;
    private final UserService userService;

    public StudentFormController(StudentFormService formService, UserService userService) {
        this.formService = formService;
        this.userService = userService;
    }

    private User getUserFromAuthentication(Authentication authentication) {
        return userService.findByUsernameOrEmail(authentication.getName());
    }

    @GetMapping("/templates")
    @PreAuthorize("hasAuthority('form:read_all')")
    public ResponseEntity<List<FormTemplate>> getTemplates() {
        return ResponseEntity.ok(formService.getAllActiveTemplates());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('form:create')")
    public ResponseEntity<StudentFormResponseDTO> createForm(
            @RequestBody StudentFormCreateDTO createDTO,
            Authentication authentication
    ) {
        User currentUser = getUserFromAuthentication(authentication);
        StudentFormResponseDTO newForm = formService.createForm(createDTO, currentUser);
        return ResponseEntity.ok(newForm);
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('form:approve')")
    public ResponseEntity<StudentFormResponseDTO> approveForm(
            @PathVariable Long id,
            @RequestBody ApprovalRequestDTO request,
            Authentication authentication
    ) {
        User currentUser = getUserFromAuthentication(authentication);
        StudentFormResponseDTO updatedForm = formService.approveForm(id, currentUser, request);
        return ResponseEntity.ok(updatedForm);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('form:delete')")
    public ResponseEntity<String> deleteForm(@PathVariable Long id) {
        formService.deleteForm(id);
        return ResponseEntity.ok("Đã xóa đơn thành công");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('form:read_all')")
    public ResponseEntity<StudentFormResponseDTO> getFormDetail(@PathVariable Long id) {
        StudentFormResponseDTO form = formService.getFormDetail(id);
        return ResponseEntity.ok(form);
    }

   @GetMapping
    @PreAuthorize("hasAuthority('form:read_all')")
    public ResponseEntity<List<StudentFormResponseDTO>> getAllForms(Authentication authentication) {
        User currentUser = getUserFromAuthentication(authentication);
        List<StudentFormResponseDTO> forms = formService.getAllForms(currentUser);
        return ResponseEntity.ok(forms);
    }
    
}