package com.tim.appTim.service;

import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.GamificationBehaviorRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BehaviorLookupService {

    private final GamificationBehaviorRepository behaviorRepository;
    private final Map<String, Integer> cache = new ConcurrentHashMap<>();

    public BehaviorLookupService(GamificationBehaviorRepository behaviorRepository) {
        this.behaviorRepository = behaviorRepository;
    }

    public Integer getIdByName(String name) {
        String key = normalizeKey(name);
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        Integer id = resolveIdByNameFlexible(name);
        cache.put(key, id);
        return id;
    }

    private Integer resolveIdByNameFlexible(String name) {
        String trimmed = name == null ? null : name.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy hành vi với tên: " + name);
        }

        Integer id = behaviorRepository.findByName(trimmed)
                .map(b -> b.getId())
                .orElse(null);
        if (id != null) {
            return id;
        }

        List<com.tim.appTim.entity.GamificationBehavior> all = behaviorRepository.findAll();
        for (com.tim.appTim.entity.GamificationBehavior b : all) {
            if (b.getName() != null && b.getName().trim().equalsIgnoreCase(trimmed)) {
                return b.getId();
            }
        }

        String legacyName = legacyNameFor(trimmed);
        if (legacyName != null) {
            Integer legacyId = behaviorRepository.findByName(legacyName)
                    .map(b -> b.getId())
                    .orElse(null);
            if (legacyId != null) {
                return legacyId;
            }
            for (com.tim.appTim.entity.GamificationBehavior b : all) {
                if (b.getName() != null && b.getName().trim().equalsIgnoreCase(legacyName)) {
                    return b.getId();
                }
            }
        }

        throw new ResourceNotFoundException("Không tìm thấy hành vi với tên: " + name);
    }

    private String legacyNameFor(String trimmedName) {
        String n = trimmedName.toLowerCase();
        if (n.equals("đạt điểm cao (>80%)")) {
            return "HIGH_POINT_1";
        }
        if (n.equals("đạt điểm xuất sắc (>95%)")) {
            return "HIGH_POINT_2";
        }
        if (n.equals("giáo viên chấm điểm 10")) {
            return "GIVING_SCORES";
        }
        if (n.equals("điểm danh đúng giờ")) {
            return "ATTEND_ON_TIME";
        }
        if (n.equals("đọc tin tức lần đầu")) {
            return "READ_BLOG";
        }
        if (n.equals("đăng bài viết đầu tiên")) {
            return "FIRST_POST";
        }
        if (n.equals("bài viết được yêu thích (>10 likes)")) {
            return "POST'S_LIKE";
        }
        if (n.equals("chia sẻ kiến thức (bài viết có link)")) {
            return "POST_SHARE";
        }
        return null;
    }

    private String normalizeKey(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }
}


