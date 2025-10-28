package com.tim.appTim.dto;

public record LinkPreviewDTO(
        String url,
        String title,
        String description,
        String imageUrl,
        String domain
) {}