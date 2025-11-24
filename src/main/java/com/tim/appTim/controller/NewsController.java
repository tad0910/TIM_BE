package com.tim.appTim.controller; // Đảm bảo đúng package

import com.tim.appTim.dto.BlogDTO;
import com.tim.appTim.service.NewsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("news") // URL gốc cho tin tức
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    /**
     * API cho "Blog Mới" (Task 58)
     * Lấy từ: blog.codegym.vn/feed/
     */
    @GetMapping("/latest")
    public ResponseEntity<List<BlogDTO>> getLatestBlogs() {
        List<BlogDTO> blogs = newsService.getLatestBlogs();
        return ResponseEntity.ok(blogs);
    }

    /**
     * API cho "Blog Hay" (Task 60)
     * Lấy từ: blog.codegym.vn/category/blog-hay/feed/
     */
    @GetMapping("/featured")
    public ResponseEntity<List<BlogDTO>> getFeaturedBlogs() {
        List<BlogDTO> blogs = newsService.getFeaturedBlogs();
        return ResponseEntity.ok(blogs);
    }

    /**
     * API MỚI CHO "Tin Tức" (Task 55)
     * Lấy từ: dev.to/api/articles?tag=java
     */
    @GetMapping("/tech")
    public ResponseEntity<List<BlogDTO>> getTechNews() {
        List<BlogDTO> blogs = newsService.getTechNews();
        return ResponseEntity.ok(blogs);
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}