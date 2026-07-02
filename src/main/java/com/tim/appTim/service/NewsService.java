package com.tim.appTim.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.tim.appTim.dto.common.BlogDTO;
import com.tim.appTim.dto.devto.DevToArticleDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NewsService {

    private static final Logger logger = LoggerFactory.getLogger(NewsService.class);

    private static final String LATEST_BLOGS_URL = "https://vnexpress.net/rss/so-hoa.rss";

    private static final String FEATURED_BLOGS_URL = "https://thanhnien.vn/rss/cong-nghe.rss";

    private static final String DEV_TO_API_URL = "https://dev.to/api/articles?tag=programming";

    private final RestTemplate restTemplate;

    public NewsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Cacheable("latestBlogs")
    public List<BlogDTO> getLatestBlogs() {
        logger.info("Đang gọi RSS feed từ VNExpress (Số hóa)...");
        return fetchFeedUsingRestTemplate(LATEST_BLOGS_URL);
    }

    @Cacheable("featuredBlogs")
    public List<BlogDTO> getFeaturedBlogs() {
        logger.info("Đang gọi RSS feed từ Thanh Niên (Công nghệ)...");
        return fetchFeedUsingRestTemplate(FEATURED_BLOGS_URL);
    }

    @Cacheable("techNews")
    public List<BlogDTO> getTechNews() {
        logger.info("Đang gọi JSON API từ Dev.to (Programming)...");
        try {
            ResponseEntity<DevToArticleDTO[]> response =
                    restTemplate.getForEntity(DEV_TO_API_URL, DevToArticleDTO[].class);

            if (response.getBody() == null) {
                return List.of();
            }

            return Arrays.stream(response.getBody())
                    .map(this::convertDevToDTO)
                    .toList();

        } catch (Exception e) {
            logger.error("Lỗi khi gọi Dev.to API: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private List<BlogDTO> fetchFeedUsingRestTemplate(String feedUrl) {
        try {
            String rssData = restTemplate.getForObject(feedUrl, String.class);

            if (rssData == null) {
                logger.warn("Nhận được dữ liệu null từ RSS feed: {}", feedUrl);
                return List.of();
            }

            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new StringReader(rssData));

            return feed.getEntries().stream()
                    .map(this::convertEntryToDTO)
                    .toList();

        } catch (Exception e) {
            logger.error("Lỗi khi đọc RSS feed (dùng RestTemplate) từ [{}]: {}", feedUrl, e.getMessage(), e);
            return List.of();
        }
    }

    private BlogDTO convertEntryToDTO(SyndEntry entry) {
        BlogDTO dto = new BlogDTO();
        
        String title = entry.getTitle();
        if (title != null) {
            dto.setTitle(org.jsoup.parser.Parser.unescapeEntities(title, true));
        }

        dto.setLink(entry.getLink());

        if (entry.getPublishedDate() != null) {
            dto.setPublishedDate(entry.getPublishedDate().toInstant().toString());
        }

        if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
            String description = entry.getDescription().getValue().replaceAll("<[^>]*>", "");
            dto.setDescription(org.jsoup.parser.Parser.unescapeEntities(description, true));
        } else {
            dto.setDescription("");
        }

        return dto;
    }

    private BlogDTO convertDevToDTO(DevToArticleDTO devToArticle) {
        BlogDTO dto = new BlogDTO();
        dto.setTitle(devToArticle.getTitle());
        dto.setLink(devToArticle.getUrl());
        dto.setPublishedDate(devToArticle.getPublishedAt());
        String author = (devToArticle.getUser() != null) ? devToArticle.getUser().getName() : "Unknown";
        dto.setDescription(devToArticle.getDescription() + " (by " + author + ")");
        return dto;
    }
}
