package com.tim.appTim.service;

import com.tim.appTim.dto.BlogDTO;
import com.tim.appTim.entity.Notification;
import com.tim.appTim.entity.User;
import com.tim.appTim.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BlogUpdateCheckerService {

    private static final Logger logger = LoggerFactory.getLogger(BlogUpdateCheckerService.class);

    private final NewsService newsService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private String lastNotifiedBlogLink = "";

    public BlogUpdateCheckerService(NewsService newsService,
                                    NotificationService notificationService,
                                    UserRepository userRepository) {
        this.newsService = newsService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    // @Scheduled(fixedRate = 3600000)
    public void checkForNewBlogs() {
        logger.info("Đang chạy tác vụ kiểm tra blog mới...");

        List<BlogDTO> latestBlogs = newsService.getLatestBlogs();
        if (latestBlogs.isEmpty()) {
            logger.info("Không tìm thấy blog nào, kết thúc tác vụ.");
            return;
        }

        BlogDTO newestBlog = latestBlogs.get(0);
        String newestBlogLink = newestBlog.getLink();

        if (!newestBlogLink.equals(lastNotifiedBlogLink) && !lastNotifiedBlogLink.isEmpty()) {

            logger.info("ĐÃ PHÁT HIỆN BLOG MỚI! Link: {}", newestBlogLink);

            broadcastNotification(newestBlog);

        } else if (lastNotifiedBlogLink.isEmpty()) {
            logger.info("Khởi tạo lần đầu, đặt link mới nhất là: {}", newestBlogLink);
        } else {
            logger.info("Không có blog mới.");
        }

        this.lastNotifiedBlogLink = newestBlogLink;
    }

    private void broadcastNotification(BlogDTO blog) {
        String title = "Có blog mới từ Codegym";
        String content = blog.getTitle();

        List<User> allUsers = userRepository.findAll();

        logger.info("Đang gửi thông báo blog mới cho {} người dùng...", allUsers.size());

        for (User user : allUsers) {
            try {
                notificationService.createNotification(
                        user.getId(),
                        null,
                        Notification.NotificationType.BLOG_NEW,
                        "BLOG_POST",
                        0L,
                        title,
                        content
                );
            } catch (Exception e) {
                logger.error("Lỗi khi gửi thông báo blog mới cho user {}: {}", user.getId(), e.getMessage());
            }
        }
    }
}