package com.tim.appTim.dto.devto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class DevToArticleDTO {

    private String title;
    private String url;
    private String description;

    @JsonProperty("published_at")
    private Instant publishedAt;

    @JsonProperty("user")
    private DevToUserDTO user;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public DevToUserDTO getUser() { return user; }
    public void setUser(DevToUserDTO user) { this.user = user; }
}