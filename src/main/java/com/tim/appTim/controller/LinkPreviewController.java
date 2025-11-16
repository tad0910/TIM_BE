package com.tim.appTim.controller;

import com.tim.appTim.dto.LinkPreviewDTO;
import com.tim.appTim.service.LinkPreviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/link-preview")
public class LinkPreviewController {

    private final LinkPreviewService linkPreviewService;

    public LinkPreviewController(LinkPreviewService linkPreviewService) {
        this.linkPreviewService = linkPreviewService;
    }

    @GetMapping
    public ResponseEntity<LinkPreviewDTO> getPreview(@RequestParam String url) {
        try {
            LinkPreviewDTO preview = linkPreviewService.getLinkPreview(url);
            return ResponseEntity.ok(preview);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(null);
        } catch (URISyntaxException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }
}