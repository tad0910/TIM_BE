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

        // 1. Kết nối và tải tài liệu HTML
        // Thêm userAgent để giả lập là một trình duyệt, tránh bị chặn
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get();

        // 2. Lấy các thẻ Open Graph (OG)
        String title = getMetaTagContent(doc, "og:title");
        String description = getMetaTagContent(doc, "og:description");
        String imageUrl = getMetaTagContent(doc, "og:image");
        String domain = getMetaTagContent(doc, "og:site_name");

        // 3. Fallback (Dự phòng nếu không có thẻ OG)
        if (title.isEmpty()) {
            title = doc.title(); // Lấy thẻ <title>
        }
        if (description.isEmpty()) {
            description = getMetaTagContent(doc, "description"); // Lấy thẻ <meta name="description">
        }
        if (domain.isEmpty()) {
            domain = new URI(url).getHost(); // Lấy domain từ URL
        }

        return new LinkPreviewDTO(title, description, imageUrl, domain);
    }

    // Hàm tiện ích để lấy nội dung thẻ meta
    private String getMetaTagContent(Document doc, String property) {
        // Thử tìm theo 'property' (ví dụ: og:title)
        String content = doc.select("meta[property=" + property + "]").attr("content");
        if (content.isEmpty()) {
            // Nếu không có, thử tìm theo 'name' (ví dụ: description)
            content = doc.select("meta[name=" + property + "]").attr("content");
        }
        return content;
    }
}