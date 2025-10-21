package com.tim.appTim.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tim.appTim.entity.ClassMember;

@Repository
public interface ClassMemberRepository extends JpaRepository<ClassMember, Long> {
    List<ClassMember> findByClassId(Long classId);
    Optional<ClassMember> findByUserIdAndClassId(Long userId, Long classId);
}