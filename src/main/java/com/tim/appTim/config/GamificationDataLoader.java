package com.tim.appTim.config;

import com.tim.appTim.entity.GamificationBehavior;
import com.tim.appTim.entity.GamificationBehaviorGroup;
import com.tim.appTim.repository.GamificationBehaviorGroupRepository;
import com.tim.appTim.repository.GamificationBehaviorRepository;
import com.tim.appTim.constants.GamificationBehaviorNames;
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
        createBehaviorIfNotFound(studyGroup, GamificationBehaviorNames.ATTEND_ON_TIME,
                GamificationBehavior.FrequencyType.DAILY, 1, 5, 0, 0);
        createBehaviorIfNotFound(studyGroup, GamificationBehaviorNames.HIGH_POINT_1,
                GamificationBehavior.FrequencyType.ONCE, 1, 0, 10, 5);
        createBehaviorIfNotFound(studyGroup, GamificationBehaviorNames.HIGH_POINT_2,
                GamificationBehavior.FrequencyType.ONCE, 1, 0, 20, 10);
        createBehaviorIfNotFound(studyGroup, GamificationBehaviorNames.READ_BLOG,
                GamificationBehavior.FrequencyType.ONCE, 1, 2, 0, 2);

        // Group: Tương tác
        createBehaviorIfNotFound(interactionGroup, GamificationBehaviorNames.FIRST_POST,
                GamificationBehavior.FrequencyType.ONCE, 1, 0, 0, 10);
        createBehaviorIfNotFound(interactionGroup, GamificationBehaviorNames.POSTS_LIKE,
                GamificationBehavior.FrequencyType.MONTHLY, 10, 0, 5, 5);
        createBehaviorIfNotFound(interactionGroup, GamificationBehaviorNames.POST_SHARE,
                GamificationBehavior.FrequencyType.MONTHLY, 10, 0, 5, 5);
        createBehaviorIfNotFound(interactionGroup, GamificationBehaviorNames.GIVING_SCORES,
                GamificationBehavior.FrequencyType.MONTHLY, 10, 0, 0, 5);
    }

    private GamificationBehaviorGroup createGroupIfNotFound(String name) {
        // Assuming name is unique or we just want to check if any group with this name
        // exists
        // Since repository might not have findByName, we can check all or just try to
        // find one.
        // But better to implement findByName in repository or just iterate.
// For simplicity, let's assume we can't easily find by name without adding
        // method to repo.
        // Wait, I should check if findByName exists in repo.
        // Based on file list, GamificationBehaviorGroupRepository is small (343 bytes),
        // likely just extends JpaRepository.
        // I'll assume I need to add findByName or use Example.
        // Let's try to use Example or just find all and filter. Since there are few
        // groups, findAll is fine.

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
