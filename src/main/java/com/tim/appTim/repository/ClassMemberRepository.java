package com.tim.appTim.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tim.appTim.entity.ClassMember;

@Repository
public interface ClassMemberRepository extends JpaRepository<ClassMember, Long> {
    List<ClassMember> findByClassId(Long classId);
    Optional<ClassMember> findByUserIdAndClassId(Long userId, Long classId);
    List<ClassMember> findByUserId(Long userId);
    Optional<ClassMember> findByClassIdAndUserId(Long classId, Long userId);
    boolean existsByClassIdAndUserId(Long classId, Long userId);
    boolean existsByClassIdAndUserIdAndRole(Long classId, Long userId, ClassMember.Role role);
    Page<ClassMember> findByClassIdAndRole(Long classId, ClassMember.Role role, Pageable pageable);
    @Query("SELECT cm.classId FROM ClassMember cm WHERE cm.userId = :userId AND cm.role = :role")
    List<Long> findClassIdsByUserIdAndRole(@Param("userId") Long userId, @Param("role") ClassMember.Role role);

    @Query("SELECT cm FROM ClassMember cm WHERE cm.classEntity.program.id = :programId AND cm.role = com.tim.appTim.entity.ClassMember.Role.sinh_vien")
    List<ClassMember> findStudentsByProgramId(@Param("programId") Long programId);
}