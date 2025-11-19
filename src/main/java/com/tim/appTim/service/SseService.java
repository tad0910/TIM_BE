package com.tim.appTim.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.tim.appTim.dto.NotificationDTO;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {

    private static final Logger logger = LoggerFactory.getLogger(SseService.class);

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Khởi tạo và lưu trữ kết nối SseEmitter cho một người dùng.
     *
     * @param userId ID của người dùng đang kết nối.
     * @return SseEmitter đã tạo.
     */
    public SseEmitter addEmitter(Long userId) {
        SseEmitter emitter = new SseEmitter(3600000L);
        emitters.put(userId, emitter);
        emitter.onTimeout(() -> {
            logger.warn("SSE Emitter timed out for User ID: {}", userId);
            emitters.remove(userId);
        });

        emitter.onCompletion(() -> {
            logger.info("SSE Emitter connection completed for User ID: {}", userId);
            emitters.remove(userId);
        });

        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected"));
        } catch (IOException e) {
            logger.error("Error sending init event to User ID: {}", userId, e);
            emitters.remove(userId);
        }

        logger.info("New SSE Emitter registered for User ID: {}", userId);
        return emitter;
    }

    public void sendNotification(Long receiverId, NotificationDTO notificationDTO) {
        SseEmitter emitter = emitters.get(receiverId);

        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("NOTIFICATION")
                        .data(notificationDTO));

                logger.info("SSE notification sent to User ID: {}", receiverId);

            } catch (IOException e) {
                logger.error("Failed to send SSE notification to User ID: {}. Removing emitter.", receiverId, e);
                emitters.remove(receiverId);
            }
        }
    }

    public void broadcastNotification(NotificationDTO notificationDTO) {
        if (emitters.isEmpty()) {
            logger.info("Không có user nào online để gửi broadcast.");
            return;
        }

        logger.info("Đang gửi broadcast blog mới tới {} user online...", emitters.size());

        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("BLOG_NEW")
                        .data(notificationDTO));
            } catch (IOException e) {
                emitters.remove(userId);
            }
        });
    }
}