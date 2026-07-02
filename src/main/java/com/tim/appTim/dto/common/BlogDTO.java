package com.tim.appTim.dto.common;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;




public class BlogDTO {

    private String title;
    private String link;
    private String description;
    private String publishedDate;


    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPublishedDate() { return publishedDate; }
    public void setPublishedDate(String publishedDate) { this.publishedDate = publishedDate; }
}
