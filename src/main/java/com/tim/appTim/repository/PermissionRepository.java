package com.tim.appTim.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tim.appTim.entity.Permission;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
}
