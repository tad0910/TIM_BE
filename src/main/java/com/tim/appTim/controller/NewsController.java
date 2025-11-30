package com.tim.appTim.controller; // Đảm bảo đúng package

import com.tim.appTim.dto.BlogDTO;
import com.tim.appTim.repository.UserRepository;
import com.tim.appTim.service.NewsService;
import com.tim.appTim.service.GamificationService;
import com.tim.appTim.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("news") // URL gốc cho tin tức
public class NewsController {

    private final NewsService newsService;
    private final GamificationService gamificationService;
    private final UserService userService;
    
    @Autowired
    private UserRepository userRepository;

    public NewsController(NewsService newsService, 
                         @Lazy GamificationService gamificationService,
                         UserService userService) {
        this.newsService = newsService;
        this.gamificationService = gamificationService;
        this.userService = userService;
    }

    /**
     * API cho "Blog Mới" (Task 58)
     * Lấy từ: blog.codegym.vn/feed/
     */
    @GetMapping("/latest")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BlogDTO>> getLatestBlogs(Authentication authentication) {
        // Tích hợp Gamification: Trao điểm khi lần đầu đọc blog
        // Lưu ý: Trao điểm cho mọi user (học sinh, giáo viên, admin, v.v.), không phân biệt role
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                Long userId = userService.findByUsernameOrEmail(authentication.getName()).getId();
                gamificationService.awardPoints(userId, "READ_BLOG");
            } catch (Exception e) {
                // Log error nhưng không làm gián đoạn flow chính
                System.err.println("Failed to award points for reading blog: " + e.getMessage());
            }
        }
        
        List<BlogDTO> blogs = newsService.getLatestBlogs();
        return ResponseEntity.ok(blogs);
    }

    /**
     * API cho "Blog Hay" (Task 60)
     * Lấy từ: blog.codegym.vn/category/blog-hay/feed/
     */
    @GetMapping("/featured")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BlogDTO>> getFeaturedBlogs(Authentication authentication) {
        // Tích hợp Gamification: Trao điểm khi lần đầu đọc blog
        // Lưu ý: Trao điểm cho mọi user (học sinh, giáo viên, admin, v.v.), không phân biệt role
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                Long userId = userService.findByUsernameOrEmail(authentication.getName()).getId();
                gamificationService.awardPoints(userId, "READ_BLOG");
            } catch (Exception e) {
                System.err.println("Failed to award points for reading blog: " + e.getMessage());
            }
        }
        
        List<BlogDTO> blogs = newsService.getFeaturedBlogs();
        return ResponseEntity.ok(blogs);
    }

    /**
     * API MỚI CHO "Tin Tức" (Task 55)
     * Lấy từ: dev.to/api/articles?tag=java
     */
    @GetMapping("/tech")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BlogDTO>> getTechNews(Authentication authentication) {
        // Tích hợp Gamification: Trao điểm khi lần đầu đọc blog
        // Lưu ý: Trao điểm cho mọi user (học sinh, giáo viên, admin, v.v.), không phân biệt role
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                Long userId = userService.findByUsernameOrEmail(authentication.getName()).getId();
                gamificationService.awardPoints(userId, "READ_BLOG");
            } catch (Exception e) {
                System.err.println("Failed to award points for reading blog: " + e.getMessage());
            }
        }
        
        List<BlogDTO> blogs = newsService.getTechNews();
        return ResponseEntity.ok(blogs);
    }

    @GetMapping("/ping")
    public String ping() {
        long count = userRepository.count();
        return "pong - DB active (Users: " + count + ")";
    }
}