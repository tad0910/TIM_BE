package com.tim.appTim.repository;

import com.tim.appTim.entity.ClassMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClassMemberRepository extends JpaRepository<ClassMember, Long> {
    List<ClassMember> findByUserId(Long userId);
}
