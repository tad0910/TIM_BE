package com.tim.appTim.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tim.appTim.entity.Permission;
import com.tim.appTim.entity.Role;
import com.tim.appTim.repository.PermissionRepository;
import com.tim.appTim.repository.RoleRepository;

@RestController
@RequestMapping("/roles")
public class RoleController {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleController(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Role>> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        List<Permission> permissions = permissionRepository.findAll();
        return ResponseEntity.ok(permissions);
    }

    @org.springframework.web.bind.annotation.PutMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateRolePermissions(
            @org.springframework.web.bind.annotation.PathVariable Long roleId,
            @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, List<Long>> body) {
        
        Role role = roleRepository.findById(roleId).orElse(null);
        if (role == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                                 .body(java.util.Map.of("message", "Role not found"));
        }

        List<Long> permissionIds = body.get("permissionIds");
        if (permissionIds == null) {
            permissionIds = java.util.Collections.emptyList();
        }

        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        role.setPermissions(new java.util.HashSet<>(permissions));
        roleRepository.save(role);

        return ResponseEntity.ok(java.util.Map.of("message", "Permissions updated successfully"));
    }
}
