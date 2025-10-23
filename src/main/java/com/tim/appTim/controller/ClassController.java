package com.tim.appTim.controller;

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
        ClassDTO createdClass = classService.createClass(classDTO, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdClass);
    }

    @PutMapping("/{classId}")
    @PreAuthorize("hasAuthority('class:update_all') or @classService.isClassTeacher(authentication, #classId)")
    public ResponseEntity<ClassDTO> updateClass(
            @PathVariable Long classId,
            @RequestBody ClassDTO classDTO,
            Authentication authentication
    ) {
        ClassDTO updatedClass = classService.updateClass(classId, classDTO);
        return ResponseEntity.ok(updatedClass);
    }

    @DeleteMapping("/{classId}")
    @PreAuthorize("hasAuthority('class:delete_all') or @classService.isClassTeacher(authentication, #classId)")
    public ResponseEntity<Void> deleteClass(
            @PathVariable Long classId,
            Authentication authentication
    ) {
        classService.deleteClass(classId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{classId}/members")
    @PreAuthorize("hasAuthority('class:update_all') or @classService.isClassTeacher(authentication, #classId)")
    public ResponseEntity<Map<String, Object>> addMemberToClass(
            @PathVariable Long classId,
            @RequestBody AddMemberDTO request,
            Authentication authentication
    ) {
        ClassMember classMember = classService.addMember(classId, request);
        return ResponseEntity.ok(Map.of(
                "message", "Thêm thành viên vào lớp học thành công",
                "member", classMember
        ));
    }

    @PutMapping("/{classId}/members/{userId}")
    @PreAuthorize("hasAuthority('class:update_all') or @classService.isClassTeacher(authentication, #classId)")
    public ResponseEntity<Map<String, Object>> updateMemberRole(
            @PathVariable Long classId,
            @PathVariable Long userId,
            @RequestBody UpdateMemberRequest request,
            Authentication authentication
    ) {
        ClassMember classMember = classService.updateMemberRole(classId, userId, request.getRole());
        return ResponseEntity.ok(Map.of(
                "message", "Cập nhật vai trò thành viên thành công",
                "member", classMember
        ));
    }

    @DeleteMapping("/{classId}/members/{userIdToRemove}")
    @PreAuthorize("hasAuthority('class:update_all') or " +
            "@classService.isClassTeacher(authentication, #classId) or " +
            "@userService.isSelf(authentication, #userIdToRemove)")
    public ResponseEntity<Map<String, String>> removeMemberFromClass(
            @PathVariable Long classId,
            @PathVariable Long userIdToRemove,
            Authentication authentication
    ) {
        classService.removeMember(classId, userIdToRemove);
        return ResponseEntity.ok(Map.of("message", "Xóa thành viên khỏi lớp học thành công"));
    }
}
