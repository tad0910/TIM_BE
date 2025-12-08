package com.tim.appTim.controller; 

import com.tim.appTim.dto.BlogDTO;
import com.tim.appTim.repository.UserRepository;
import com.tim.appTim.service.NewsService;
import com.tim.appTim.service.GamificationService;
import com.tim.appTim.service.UserService;
import com.tim.appTim.service.BehaviorLookupService;
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
@RequestMapping("news") 
public class NewsController {

    private final NewsService newsService;
    private final GamificationService gamificationService;
    private final UserService userService;
    private final BehaviorLookupService behaviorLookupService;
    
    @Autowired
    private UserRepository userRepository;

    public NewsController(NewsService newsService, 
                         @Lazy GamificationService gamificationService,
                         UserService userService,
                         BehaviorLookupService behaviorLookupService) {
        this.newsService = newsService;
        this.gamificationService = gamificationService;
        this.userService = userService;
        this.behaviorLookupService = behaviorLookupService;
    }

    @GetMapping("/latest")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BlogDTO>> getLatestBlogs(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                Long userId = userService.findByUsernameOrEmail(authentication.getName()).getId();
                Integer behaviorId = behaviorLookupService.getIdByName("READ_BLOG");
                gamificationService.awardPoints(userId, behaviorId);
            } catch (Exception e) {
                System.err.println("Failed to award points for reading blog: " + e.getMessage());
            }
        }
        
        List<BlogDTO> blogs = newsService.getLatestBlogs();
        return ResponseEntity.ok(blogs);
    }

    @GetMapping("/featured")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BlogDTO>> getFeaturedBlogs(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                Long userId = userService.findByUsernameOrEmail(authentication.getName()).getId();
                Integer behaviorId = behaviorLookupService.getIdByName("READ_BLOG");
                gamificationService.awardPoints(userId, behaviorId);
            } catch (Exception e) {
                System.err.println("Failed to award points for reading blog: " + e.getMessage());
            }
        }
        
        List<BlogDTO> blogs = newsService.getFeaturedBlogs();
        return ResponseEntity.ok(blogs);
    }

    @GetMapping("/tech")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BlogDTO>> getTechNews(Authentication authentication) {

        if (authentication != null && authentication.isAuthenticated()) {
            try {
                Long userId = userService.findByUsernameOrEmail(authentication.getName()).getId();
                Integer behaviorId = behaviorLookupService.getIdByName("READ_BLOG");
                gamificationService.awardPoints(userId, behaviorId);
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