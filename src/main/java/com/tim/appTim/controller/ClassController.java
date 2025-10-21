package com.tim.appTim.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.tim.appTim.dto.ClassDTO;
import com.tim.appTim.entity.Class;
import com.tim.appTim.entity.ClassMember;
import com.tim.appTim.service.ClassService;
import com.tim.appTim.dto.AddMemberDTO;

@RestController
@RequestMapping("/classes")
public class ClassController {

    @Autowired
    private ClassService classService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('class:read_all') or @classService.isClassMember(authentication, #id)")
    public ClassDTO getClassInfo(@PathVariable Long id, Authentication authentication) {
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
    @PostMapping
    @PreAuthorize("hasAuthority('class:create')")
    public ResponseEntity<ClassDTO> createClass(
            @RequestBody ClassDTO classDTO,
            Authentication authentication
    ) {
        return null;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('class:update_all') or @classService.isClassTeacher(authentication, #id)")
    public ResponseEntity<ClassDTO> updateClass(
            @PathVariable Long id,
            @RequestBody ClassDTO classDTO,
            Authentication authentication
    ) {
        return null;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('class:delete_all') or @classService.isClassTeacher(authentication, #id)")
    public ResponseEntity<Void> deleteClass(
            @PathVariable Long id,
            Authentication authentication
    ) {

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAuthority('class:update_all') or @classService.isClassTeacher(authentication, #id)")
    public ResponseEntity<?> addMember(
            @PathVariable Long id,
            @RequestBody AddMemberDTO addMemberDTO,
            Authentication authentication
    ) {

        return null;
    }

    @DeleteMapping("/{id}/members/{userIdToRemove}")
    @PreAuthorize("hasAuthority('class:update_all') or " +
            "@classService.isClassTeacher(authentication, #id) or " +
            "@userService.isSelf(authentication, #userIdToRemove)")
    public ResponseEntity<?> removeMember(
            @PathVariable Long id,
            @PathVariable Long userIdToRemove,
            Authentication authentication
    ) {

        return ResponseEntity.noContent().build();
    }
}
