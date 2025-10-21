package com.tim.appTim.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tim.appTim.dto.AddMemberRequest;
import com.tim.appTim.dto.ClassDTO;
import com.tim.appTim.dto.UpdateMemberRequest;
import com.tim.appTim.entity.Class;
import com.tim.appTim.entity.ClassMember;
import com.tim.appTim.service.ClassService;

@RestController
@RequestMapping("/classes")
public class ClassController {

    @Autowired
    private ClassService classService;

    @GetMapping("/{id}")
    public ClassDTO getClassInfo(@PathVariable Long id) {
        Class classInfo = classService.getClassById(id)
                .orElseThrow(() -> new RuntimeException("Class not found with id: " + id));

        List<ClassMember> members = classService.getClassMembersByClassId(id);

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

        return classDTO;
    }

    @PostMapping("/{classId}/members")
    public ResponseEntity<?> addMemberToClass(@PathVariable Long classId, @RequestBody AddMemberRequest request) {
        try {
            ClassMember classMember = classService.addMemberToClass(classId, request.getUserId(), request.getRole());
            
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
    public ResponseEntity<?> updateMemberRole(@PathVariable Long classId, @PathVariable Long userId, @RequestBody UpdateMemberRequest request) {
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

    @DeleteMapping("/{classId}/members/{userId}")
    public ResponseEntity<?> removeMemberFromClass(@PathVariable Long classId, @PathVariable Long userId) {
        try {
            classService.removeMemberFromClass(classId, userId);
            
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
