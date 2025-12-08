package com.tim.appTim.service;

import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.GamificationBehaviorRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper service to resolve behavior ID by name and cache the result.
 * Dùng khi caller muốn trao điểm theo tên nhưng GamificationService nhận id.
 */
@Service
public class BehaviorLookupService {

    private final GamificationBehaviorRepository behaviorRepository;
    private final Map<String, Integer> cache = new ConcurrentHashMap<>();

    public BehaviorLookupService(GamificationBehaviorRepository behaviorRepository) {
        this.behaviorRepository = behaviorRepository;
    }

    public Integer getIdByName(String name) {
        if (cache.containsKey(name)) {
            return cache.get(name);
        }
        Integer id = behaviorRepository.findByName(name)
                .map(b -> b.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hành vi với tên: " + name));
        cache.put(name, id);
        return id;
    }
}


