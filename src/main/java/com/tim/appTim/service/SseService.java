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

    // Map để lưu trữ kết nối SseEmitter theo User ID (Định danh người nhận)
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Khởi tạo và lưu trữ kết nối SseEmitter cho một người dùng.
     * @param userId ID của người dùng đang kết nối.
     * @return SseEmitter đã tạo.
     */
    public SseEmitter addEmitter(Long userId) {
        // Thiết lập timeout (ví dụ: 1 giờ). Client sẽ tự động reconnect.
        SseEmitter emitter = new SseEmitter(3600000L);

        // Ghi đè emitter cũ nếu có
        emitters.put(userId, emitter);

        // Xử lý khi kết nối bị ngắt do Timeout
        emitter.onTimeout(() -> {
            logger.warn("SSE Emitter timed out for User ID: {}", userId);
            emitters.remove(userId);
        });

        // Xử lý khi kết nối hoàn tất (ví dụ: Client đóng)
        emitter.onCompletion(() -> {
            logger.info("SSE Emitter connection completed for User ID: {}", userId);
            emitters.remove(userId);
        });

        // Gửi một tin nhắn khởi tạo để đảm bảo kết nối được thiết lập
        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected"));
        } catch (IOException e) {
            logger.error("Error sending init event to User ID: {}", userId, e);
            emitters.remove(userId);
        }

        logger.info("New SSE Emitter registered for User ID: {}", userId);
        return emitter;
    }

    /**
     * Gửi thông báo đến người dùng cụ thể.
     * @param receiverId ID của người nhận.
     * @param notificationDTO DTO thông báo.
     */
    public void sendNotification(Long receiverId, NotificationDTO notificationDTO) {
        SseEmitter emitter = emitters.get(receiverId);

        if (emitter != null) {
            try {
                // Gửi sự kiện dưới dạng JSON
                emitter.send(SseEmitter.event()
                        .name("NOTIFICATION") // Tên sự kiện (Client sẽ lắng nghe tên này)
                        .data(notificationDTO)); // Dữ liệu là NotificationDTO

                logger.info("SSE notification sent to User ID: {}", receiverId);

            } catch (IOException e) {
                // Nếu gửi thất bại (ví dụ: client đã ngắt kết nối), xóa emitter
                logger.error("Failed to send SSE notification to User ID: {}. Removing emitter.", receiverId, e);
                emitters.remove(receiverId);
            }
        }
    }
}