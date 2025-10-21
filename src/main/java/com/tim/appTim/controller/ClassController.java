package com.tim.appTim.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tim.appTim.dto.AddMemberDTO;
import com.tim.appTim.dto.ClassDTO;
import com.tim.appTim.dto.UpdateMemberRequest;
import com.tim.appTim.entity.Class;
import com.tim.appTim.entity.ClassMember;
import com.tim.appTim.service.ClassService;
import com.tim.appTim.service.UserService;

@RestController
@RequestMapping("/classes")
public class ClassController {

    private final ClassService classService;
    private final UserService userService;

    public ClassController(ClassService classService, UserService userService) {
        this.classService = classService;
        this.userService = userService;
    }

    @GetMapping("/{classId}")
    @PreAuthorize("hasAuthority('class:read_all') or @classService.isClassMember(authentication, #classId)")
    public ResponseEntity<ClassDTO> getClassInfo(@PathVariable Long classId, Authentication authentication) {
        Class classInfo = classService.getClassById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found with id: " + classId));

        List<ClassMember> members = classService.getClassMembersByClassId(classId);

        ClassDTO classDTO = new ClassDTO(
                classInfo.getClassName(),
                classInfo.getDescription(),
                members.stream()
                        .map(member -> new ClassDTO.MemberDTO(
                                member.getUserId(),
                                member.getRole().name(),
                                member.getJoinDate()
                        ))
                        .collect(Collectors.toList())
        );

        return ResponseEntity.ok(classDTO);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('class:create')")
    public ResponseEntity<ClassDTO> createClass(
            @RequestBody ClassDTO classDTO,
            Authentication authentication
    ) {
        try {
            ClassDTO createdClass = classService.createClass(classDTO, authentication);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdClass);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create class: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); // Return null or error DTO
        }
    }

    @PutMapping("/{classId}")
    @PreAuthorize("hasAuthority('class:update_all') or @classService.isClassTeacher(authentication, #classId)")
    public ResponseEntity<ClassDTO> updateClass(
            @PathVariable Long classId,
            @RequestBody ClassDTO classDTO,
            Authentication authentication
    ) {
        try {
            ClassDTO updatedClass = classService.updateClass(classId, classDTO);
            return ResponseEntity.ok(updatedClass);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/{classId}")
    @PreAuthorize("hasAuthority('class:delete_all') or @classService.isClassTeacher(authentication, #classId)")
    public ResponseEntity<Void> deleteClass(
            @PathVariable Long classId,
            Authentication authentication
    ) {
        try {
            classService.deleteClass(classId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{classId}/members")
    @PreAuthorize("hasAuthority('class:update_all') or @classService.isClassTeacher(authentication, #classId)")
    public ResponseEntity<?> addMemberToClass(
            @PathVariable Long classId,
            @RequestBody AddMemberDTO request,
            Authentication authentication
    ) {
        try {
            ClassMember classMember = classService.addMember(classId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Thêm thành viên vào lớp học thành công");
            response.put("member", classMember);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{classId}/members/{userId}")
    @PreAuthorize("hasAuthority('class:update_all') or @classService.isClassTeacher(authentication, #classId)")
    public ResponseEntity<?> updateMemberRole(
            @PathVariable Long classId,
            @PathVariable Long userId,
            @RequestBody UpdateMemberRequest request,
            Authentication authentication
    ) {
        try {
            ClassMember classMember = classService.updateMemberRole(classId, userId, request.getRole());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cập nhật vai trò thành viên thành công");
            response.put("member", classMember);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{classId}/members/{userIdToRemove}")
    @PreAuthorize("hasAuthority('class:update_all') or " +
            "@classService.isClassTeacher(authentication, #classId) or " +
            "@userService.isSelf(authentication, #userIdToRemove)")
    public ResponseEntity<?> removeMemberFromClass(
                                                    @PathVariable Long classId,
                                                    @PathVariable Long userIdToRemove,
                                                    Authentication authentication
    ) {
        try {
            classService.removeMember(classId, userIdToRemove);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Xóa thành viên khỏi lớp học thành công");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

}