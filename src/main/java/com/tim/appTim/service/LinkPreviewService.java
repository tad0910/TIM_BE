package com.tim.appTim.service;

import com.tim.appTim.dto.LinkPreviewDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Service
public class LinkPreviewService {

    public LinkPreviewDTO getLinkPreview(String url) throws IOException, URISyntaxException {

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get();

        String title = getMetaTagContent(doc, "og:title");
        String description = getMetaTagContent(doc, "og:description");
        String imageUrl = getMetaTagContent(doc, "og:image");
        String domain = getMetaTagContent(doc, "og:site_name");

        if (title.isEmpty()) {
            title = doc.title(); 
        }
        if (description.isEmpty()) {
            description = getMetaTagContent(doc, "description"); 
        }
        if (domain.isEmpty()) {
            domain = new URI(url).getHost(); 
        }

        return new LinkPreviewDTO(url, title, description, imageUrl, domain);
    }

    private String getMetaTagContent(Document doc, String property) {

        String content = doc.select("meta[property=" + property + "]").attr("content");
        if (content.isEmpty()) {
            content = doc.select("meta[name=" + property + "]").attr("content");
        }
        return content;
    }
}