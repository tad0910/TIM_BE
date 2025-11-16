package com.tim.appTim.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tim.appTim.entity.ClassMember;

@Repository
public interface ClassMemberRepository extends JpaRepository<ClassMember, Long> {
    List<ClassMember> findByClassId(Long classId);
    Optional<ClassMember> findByUserIdAndClassId(Long userId, Long classId);
    List<ClassMember> findByUserId(Long userId);
    Optional<ClassMember> findByClassIdAndUserId(Long classId, Long userId);
    boolean existsByClassIdAndUserId(Long classId, Long userId);
    Page<ClassMember> findByClassIdAndVaiTro(Long classId, ClassMember.Role vaiTro, Pageable pageable);
}