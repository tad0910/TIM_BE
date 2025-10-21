package com.tim.appTim.service;

import java.util.List;
import java.util.Optional;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tim.appTim.entity.Class;
import com.tim.appTim.entity.ClassMember;
import com.tim.appTim.entity.User;
import com.tim.appTim.repository.ClassMemberRepository;
import com.tim.appTim.repository.ClassRepository;
import com.tim.appTim.service.UserService;
import com.tim.appTim.entity.ClassMember;
import org.springframework.security.core.Authentication;
import com.tim.appTim.dto.ClassDTO;
import com.tim.appTim.dto.AddMemberDTO;

@Service("classService")
public class ClassService {

    private final UserService userService;
    private ClassRepository classRepository;
    private ClassMemberRepository classMemberRepository;

    @Autowired
    public ClassService(UserService userService,
                        ClassRepository classRepository,
                        ClassMemberRepository classMemberRepository) {
        this.userService = userService;
        this.classRepository = classRepository;
        this.classMemberRepository = classMemberRepository;
    }

    public Optional<Class> getClassById(Long id) {
        return classRepository.findById(id);
    }

    public List<ClassMember> getClassMembersByClassId(Long classId) {
        return classMemberRepository.findByClassId(classId);
    }

    public boolean isClassMember(Authentication authentication, Long classId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        User currentUser = userService.findByUsernameOrEmail(authentication.getName());
        if (currentUser == null) {
            return false;
        }

        return classMemberRepository.findByUserIdAndClassId(currentUser.getId(), classId).isPresent();
    }

    public boolean isClassTeacher(Authentication authentication, Long classId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        User currentUser = userService.findByUsernameOrEmail(authentication.getName());
        if (currentUser == null) {
            return false;
        }

        Optional<ClassMember> memberOpt = classMemberRepository.findByUserIdAndClassId(currentUser.getId(), classId);

        if (memberOpt.isEmpty()) {
            return false;
        }

        ClassMember member = memberOpt.get();
        return member.getRole() == ClassMember.Role.giang_vien;
    }
    public ClassDTO createClass(ClassDTO classDTO, Authentication authentication) {
        throw new UnsupportedOperationException("Chưa implement logic createClass");
    }

    public ClassDTO updateClass(Long id, ClassDTO classDTO) {
        throw new UnsupportedOperationException("Chưa implement logic updateClass");
    }

    public void deleteClass(Long id) {
        throw new UnsupportedOperationException("Chưa implement logic deleteClass");
    }

    public ClassMember addMember(Long classId, AddMemberDTO addMemberDTO) {
        throw new UnsupportedOperationException("Chưa implement logic addMember");
    }

    public void removeMember(Long classId, Long userIdToRemove) {
        throw new UnsupportedOperationException("Chưa implement logic removeMember");
    }
}