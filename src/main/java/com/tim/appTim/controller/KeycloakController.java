package com.tim.appTim.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tim.appTim.service.KeycloakSyncService;

@RestController
public class KeycloakController {

    @Autowired
    private KeycloakSyncService keycloakSyncService;

    @GetMapping("/test-sync")
    public String testSync() {
        keycloakSyncService.syncUsers();
        return "Sync completed";
    }
}