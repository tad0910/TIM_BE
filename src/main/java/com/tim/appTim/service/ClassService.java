package com.tim.appTim.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tim.appTim.entity.Class;
import com.tim.appTim.entity.ClassMember;
import com.tim.appTim.repository.ClassMemberRepository;
import com.tim.appTim.repository.ClassRepository;

@Service
public class ClassService {

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private ClassMemberRepository classMemberRepository;

    public Optional<Class> getClassById(Long id) {
        return classRepository.findById(id);
    }

    public List<ClassMember> getClassMembersByClassId(Long classId) {
        return classMemberRepository.findByClassId(classId);
    }

    public ClassMember addMemberToClass(Long classId, Long userId, ClassMember.Role role) {
        // Kiểm tra xem user đã tham gia class chưa
        Optional<ClassMember> existingMember = classMemberRepository.findByClassIdAndUserId(classId, userId);
        if (existingMember.isPresent()) {
            throw new RuntimeException("User đã tham gia lớp học này rồi");
        }

        ClassMember classMember = new ClassMember();
        classMember.setClassId(classId);
        classMember.setUserId(userId);
        classMember.setRole(role);
        classMember.setJoinDate(LocalDateTime.now());
        
        return classMemberRepository.save(classMember);
    }

    public ClassMember updateMemberRole(Long classId, Long userId, ClassMember.Role newRole) {
        ClassMember classMember = classMemberRepository.findByClassIdAndUserId(classId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên trong lớp học"));
        
        classMember.setRole(newRole);
        return classMemberRepository.save(classMember);
    }

    public void removeMemberFromClass(Long classId, Long userId) {
        ClassMember classMember = classMemberRepository.findByClassIdAndUserId(classId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên trong lớp học"));
        
        classMemberRepository.delete(classMember);
    }

    public List<ClassMember> getUserClasses(Long userId) {
        return classMemberRepository.findByUserId(userId);
    }
}