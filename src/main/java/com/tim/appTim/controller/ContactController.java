package com.tim.appTim.controller;

import com.tim.appTim.dto.request.ContactPersonRequestDTO;
import com.tim.appTim.entity.ContactPerson;
import com.tim.appTim.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; 
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    @PreAuthorize("hasAuthority('company:update')")
    public ResponseEntity<ContactPerson> addContact(@RequestBody ContactPersonRequestDTO request) {
        return ResponseEntity.ok(contactService.create(request));
    }
}
