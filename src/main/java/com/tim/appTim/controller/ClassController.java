package com.tim.appTim.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tim.appTim.dto.ClassDTO;
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
}
