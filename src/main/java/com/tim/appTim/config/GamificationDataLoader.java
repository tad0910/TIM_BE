package com.tim.appTim.config;

import com.tim.appTim.entity.BehaviorPointType;
import com.tim.appTim.entity.GamificationBehavior;
import com.tim.appTim.entity.GamificationBehaviorGroup;
import com.tim.appTim.entity.GamificationPointType;
import com.tim.appTim.repository.BehaviorPointTypeRepository;
import com.tim.appTim.repository.GamificationBehaviorGroupRepository;
import com.tim.appTim.repository.GamificationBehaviorRepository;
import com.tim.appTim.repository.GamificationPointTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Configuration
public class GamificationDataLoader implements CommandLineRunner {

    private final GamificationBehaviorGroupRepository groupRepository;
    private final GamificationBehaviorRepository behaviorRepository;
    private final BehaviorPointTypeRepository behaviorPointTypeRepository;
    private final GamificationPointTypeRepository pointTypeRepository;

    public GamificationDataLoader(
            GamificationBehaviorGroupRepository groupRepository,
            GamificationBehaviorRepository behaviorRepository,
            BehaviorPointTypeRepository behaviorPointTypeRepository,
            GamificationPointTypeRepository pointTypeRepository) {
        this.groupRepository = groupRepository;
        this.behaviorRepository = behaviorRepository;
        this.behaviorPointTypeRepository = behaviorPointTypeRepository;
        this.pointTypeRepository = pointTypeRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        GamificationBehaviorGroup studyGroup = createGroupIfNotFound("Học tập");
        GamificationBehaviorGroup interactionGroup = createGroupIfNotFound("Tương tác");
        GamificationBehaviorGroup activityGroup = createGroupIfNotFound("Hoạt động");

        // Tìm hoặc tạo các point types (có thể tìm theo tên tiếng Việt hoặc tiếng Anh)
        GamificationPointType diligenceType = findOrCreatePointType("Chuyên cần", "Diligence");
        GamificationPointType competenceType = findOrCreatePointType("Năng lực", "Competence");
        GamificationPointType experienceType = findOrCreatePointType("Kinh nghiệm", "Experience");

        // Tạo behaviors với tên mới (tiếng Việt) và dùng behavior_point_types
        createBehaviorWithPointTypes(studyGroup, "Điểm danh đúng giờ",
                GamificationBehavior.FrequencyType.DAILY, 1,
                new PointTypeConfig(diligenceType, 5));

        createBehaviorWithPointTypes(studyGroup, "Đạt điểm cao (>80%)",
                GamificationBehavior.FrequencyType.ONCE, 1,
                new PointTypeConfig(competenceType, 10),
                new PointTypeConfig(experienceType, 5));

        createBehaviorWithPointTypes(studyGroup, "Đạt điểm xuất sắc (>95%)",
                GamificationBehavior.FrequencyType.ONCE, 1,
                new PointTypeConfig(competenceType, 20),
                new PointTypeConfig(experienceType, 10));

        createBehaviorWithPointTypes(activityGroup, "Đọc tin tức lần đầu",
                GamificationBehavior.FrequencyType.ONCE, 1,
                new PointTypeConfig(diligenceType, 2),
                new PointTypeConfig(experienceType, 2));

        createBehaviorWithPointTypes(activityGroup, "Đăng bài viết đầu tiên",
                GamificationBehavior.FrequencyType.ONCE, 1,
                new PointTypeConfig(experienceType, 10));

        createBehaviorWithPointTypes(interactionGroup, "Bài viết được yêu thích (>10 likes)",
                GamificationBehavior.FrequencyType.MONTHLY, 10,
                new PointTypeConfig(competenceType, 5),
                new PointTypeConfig(experienceType, 5));

        createBehaviorWithPointTypes(interactionGroup, "Chia sẻ kiến thức (Bài viết có link)",
                GamificationBehavior.FrequencyType.MONTHLY, 10,
                new PointTypeConfig(competenceType, 5),
                new PointTypeConfig(experienceType, 5));

        createBehaviorWithPointTypes(studyGroup, "Giáo viên chấm điểm 10",
                GamificationBehavior.FrequencyType.MONTHLY, 10,
                new PointTypeConfig(experienceType, 5));
    }

    private GamificationBehaviorGroup createGroupIfNotFound(String name) {
        Optional<GamificationBehaviorGroup> existing = groupRepository.findAll().stream()
                .filter(g -> g.getName().equalsIgnoreCase(name))
                .findFirst();

        if (existing.isPresent()) {
            return existing.get();
        }

        GamificationBehaviorGroup group = new GamificationBehaviorGroup();
        group.setName(name);
        return groupRepository.save(group);
    }

    private GamificationPointType findOrCreatePointType(String vietnameseName, String englishName) {
        // Tìm theo tên tiếng Việt chính xác trước
        Optional<GamificationPointType> byVietnamese = pointTypeRepository.findByName(vietnameseName);
        if (byVietnamese.isPresent()) {
            return byVietnamese.get();
        }
        
        // Nếu không tìm thấy, tìm theo tên tiếng Anh chính xác
        Optional<GamificationPointType> byEnglish = pointTypeRepository.findByName(englishName);
        if (byEnglish.isPresent()) {
            return byEnglish.get();
        }
        
        // Nếu vẫn không tìm thấy, tìm trong danh sách tất cả point types (case-insensitive, partial match)
        String vietnameseLower = vietnameseName.toLowerCase().trim();
        String englishLower = englishName.toLowerCase().trim();
        
        Optional<GamificationPointType> byPartialMatch = pointTypeRepository.findAll().stream()
                .filter(pt -> {
                    String ptName = pt.getName().toLowerCase().trim();
                    // Tìm theo tên chứa từ khóa (loại bỏ "Điểm " nếu có)
                    String vietnameseKey = vietnameseLower.replace("điểm ", "").trim();
                    return ptName.equals(vietnameseLower) || 
                           ptName.equals(englishLower) ||
                           ptName.contains(vietnameseKey) ||
                           ptName.contains(englishLower);
                })
                .findFirst();
        
        if (byPartialMatch.isPresent()) {
            return byPartialMatch.get();
        }
        
        // Nếu không tìm thấy, tạo mới point type
        GamificationPointType newPointType = new GamificationPointType();
        newPointType.setName(vietnameseName);
        newPointType.setDescription("Điểm thưởng " + vietnameseName);
        newPointType.setMaxPoints(0);
        newPointType.setIsActive(true);
        newPointType.setShowOnDashboard(true);
        return pointTypeRepository.save(newPointType);
    }

    private static class PointTypeConfig {
        final GamificationPointType pointType;
        final Integer points;

        PointTypeConfig(GamificationPointType pointType, Integer points) {
            this.pointType = pointType;
            this.points = points;
        }
    }

    private void createBehaviorWithPointTypes(GamificationBehaviorGroup group, String name,
            GamificationBehavior.FrequencyType frequencyType, int maxTimes,
            PointTypeConfig... pointTypeConfigs) {
        Optional<GamificationBehavior> existing = behaviorRepository.findByName(name);
        if (existing.isPresent()) {
            return;
        }

        // Tạo behavior với các trường cũ set về 0
        GamificationBehavior behavior = new GamificationBehavior();
        behavior.setGroup(group);
        behavior.setName(name);
        behavior.setFrequencyType(frequencyType);
        behavior.setMaxTimesPerFrequency(maxTimes);
        behavior.setPointDiligence(0);
        behavior.setPointCompetence(0);
        behavior.setPointExperience(0);
        GamificationBehavior saved = behaviorRepository.save(behavior);

        // Tạo behavior_point_types
        for (PointTypeConfig config : pointTypeConfigs) {
            if (config.pointType != null && config.points != null && config.points > 0) {
                BehaviorPointType behaviorPointType = new BehaviorPointType();
                behaviorPointType.setBehavior(saved);
                behaviorPointType.setPointType(config.pointType);
                behaviorPointType.setPoints(config.points);
                behaviorPointTypeRepository.save(behaviorPointType);
            }
        }
    }
}
