package com.tim.appTim.config;

import com.tim.appTim.entity.GamificationBehavior;
import com.tim.appTim.entity.GamificationBehaviorGroup;
import com.tim.appTim.repository.GamificationBehaviorGroupRepository;
import com.tim.appTim.repository.GamificationBehaviorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class GamificationDataLoader implements CommandLineRunner {

    private final GamificationBehaviorGroupRepository groupRepository;
    private final GamificationBehaviorRepository behaviorRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. Create Groups
        GamificationBehaviorGroup studyGroup = createGroupIfNotFound("Học tập");
        GamificationBehaviorGroup interactionGroup = createGroupIfNotFound("Tương tác");
        GamificationBehaviorGroup activityGroup = createGroupIfNotFound("Hoạt động");

        // 2. Create Behaviors
        // Group: Học tập
        createBehaviorIfNotFound(studyGroup, "ATTEND_ON_TIME",
                GamificationBehavior.FrequencyType.DAILY, 1, 5, 0, 0);
        createBehaviorIfNotFound(studyGroup, "HIGH_POINT_1",
                GamificationBehavior.FrequencyType.ONCE, 1, 0, 10, 5);
        createBehaviorIfNotFound(studyGroup, "HIGH_POINT_2",
                GamificationBehavior.FrequencyType.ONCE, 1, 0, 20, 10);
        createBehaviorIfNotFound(activityGroup, "READ_BLOG",
                GamificationBehavior.FrequencyType.ONCE, 1, 2, 0, 2);

        // Group: Tương tác
        createBehaviorIfNotFound(activityGroup, "FIRST_POST",
                GamificationBehavior.FrequencyType.ONCE, 1, 0, 0, 10);
        createBehaviorIfNotFound(interactionGroup, "POST'S_LIKE",
                GamificationBehavior.FrequencyType.MONTHLY, 10, 0, 5, 5);
        createBehaviorIfNotFound(interactionGroup, "POST_SHARE",
                GamificationBehavior.FrequencyType.MONTHLY, 10, 0, 5, 5);
        createBehaviorIfNotFound(studyGroup, "GIVING_SCORES",
                GamificationBehavior.FrequencyType.MONTHLY, 10, 0, 0, 5);
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

    private void createBehaviorIfNotFound(GamificationBehaviorGroup group, String name,
            GamificationBehavior.FrequencyType frequencyType, int maxTimes,
            int diligence, int competence, int experience) {
        Optional<GamificationBehavior> existing = behaviorRepository.findByName(name);
        if (existing.isPresent()) {
            return;
        }

        GamificationBehavior behavior = new GamificationBehavior();
        behavior.setGroup(group);
        behavior.setName(name);
        behavior.setFrequencyType(frequencyType);
        behavior.setMaxTimesPerFrequency(maxTimes);
        behavior.setPointDiligence(diligence);
        behavior.setPointCompetence(competence);
        behavior.setPointExperience(experience);
        behaviorRepository.save(behavior);
    }
}
