package com.tim.appTim.controller;

import com.tim.appTim.entity.NotificationTemplate;
import com.tim.appTim.service.NotificationTemplateService;
import com.tim.appTim.service.FileUploadService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@RestController
@RequestMapping("/notification-templates")
public class NotificationTemplateController {

    private final NotificationTemplateService templateService;
    
    @Autowired
    private FileUploadService fileUploadService;

    public NotificationTemplateController(NotificationTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('notification:create_manual')")
    public ResponseEntity<List<NotificationTemplate>> getAll(
            @RequestParam(value = "name", required = false) String name) {
        if (name == null || name.isBlank()) {
            return ResponseEntity.ok(templateService.getAll());
        }
        return templateService.getAll().stream()
                .filter(t -> name.equalsIgnoreCase(t.getName()))
                .findFirst()
                .map(t -> ResponseEntity.ok(java.util.List.of(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('notification:create_manual')")
    public ResponseEntity<NotificationTemplate> getById(@PathVariable Long id) {
        return ResponseEntity.ok(templateService.getById(id));
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('notification:create_manual')")
    public ResponseEntity<NotificationTemplate> create(
            @RequestParam("name") String name,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "iconFile", required = false) MultipartFile iconFile,
            @RequestParam(value = "iconUrl", required = false) String iconUrl) {
        
        String finalIconUrl = iconUrl;
        if (iconFile != null && !iconFile.isEmpty()) {
            try {
                finalIconUrl = fileUploadService.uploadFile(iconFile);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(null);
            }
        }
        
        return ResponseEntity.ok(templateService.create(name, title, content, finalIconUrl));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('notification:create_manual')")
    public ResponseEntity<NotificationTemplate> update(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "iconFile", required = false) MultipartFile iconFile,
            @RequestParam(value = "iconUrl", required = false) String iconUrl) {
        
        String finalIconUrl = iconUrl;
        if (iconFile != null && !iconFile.isEmpty()) {
            try {
                finalIconUrl = fileUploadService.uploadFile(iconFile);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(null);
            }
        }
        
        return ResponseEntity.ok(templateService.update(id, name, title, content, finalIconUrl));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('notification:create_manual')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

