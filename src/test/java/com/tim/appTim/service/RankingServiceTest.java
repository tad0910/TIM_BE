package com.tim.appTim.service;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;

import com.tim.appTim.entity.Class;
import com.tim.appTim.entity.ClassMember;
import com.tim.appTim.entity.Ranking;
import com.tim.appTim.entity.RankingMonthly;
import com.tim.appTim.entity.User;
import com.tim.appTim.entity.UserGamificationStats;
import com.tim.appTim.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock
    private RankingRepository rankingRepository;
    @Mock
    private RankingMonthlyRepository rankingMonthlyRepository;
    @Mock
    private UserGamificationStatsRepository statsRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ClassMemberRepository classMemberRepository;
    @Mock
    private ClassRepository classRepository;

    @InjectMocks
    private RankingService rankingService;

    private User user;
    private UserGamificationStats stats;
    private Ranking ranking;
    private ClassMember classMember;
    private Class classEntity;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setFirstName("Test");
        user.setLastName("User");

        stats = new UserGamificationStats();
        stats.setUserId(1L);
        stats.setTotalDiligence(10);
        stats.setTotalCompetence(20);
        stats.setTotalExperience(30);

        ranking = new Ranking();
        ranking.setUserId(1L);
        ranking.setTotalDiligenceScore(10);
        ranking.setTotalCompetenceScore(20);
        ranking.setTotalExperienceScore(30);
        ranking.setClassId(1L);
        ranking.setUser(user);

        classEntity = new Class();
        classEntity.setId(1L);
        classEntity.setClassName("Class A");
        ranking.setClassEntity(classEntity);

        classMember = new ClassMember();
        classMember.setUserId(1L);
        classMember.setClassId(1L);
    }

    @Test
    void updateRanking_ExistingStats_ShouldUpdateRanking() {
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(classMemberRepository.findByUserId(1L)).thenReturn(List.of(classMember));
        when(rankingRepository.findByUserId(1L)).thenReturn(Optional.of(ranking));

        rankingService.updateRanking(1L);

        verify(rankingRepository).save(any(Ranking.class));
        assertThat(ranking.getTotalDiligenceScore()).isEqualTo(10);
        assertThat(ranking.getTotalCompetenceScore()).isEqualTo(20);
        assertThat(ranking.getTotalExperienceScore()).isEqualTo(30);
        assertThat(ranking.getClassId()).isEqualTo(1L);
    }

    @Test
    void updateRanking_NoStats_ShouldCreateDefaultStats() {
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(classMemberRepository.findByUserId(1L)).thenReturn(List.of(classMember));
        when(rankingRepository.findByUserId(1L)).thenReturn(Optional.of(ranking));

        rankingService.updateRanking(1L);

        verify(rankingRepository).save(any(Ranking.class));
        assertThat(ranking.getTotalDiligenceScore()).isEqualTo(0);
    }

    @Test
    void recalculateAllRankings_WithClassId_ShouldSortAndSave() {
        Ranking r1 = new Ranking();
        r1.setUserId(1L);
        r1.setTotalExperienceScore(100);

        Ranking r2 = new Ranking();
        r2.setUserId(2L);
        r2.setTotalExperienceScore(200);

        List<Ranking> rankings = new ArrayList<>();
        rankings.add(r1);
        rankings.add(r2);

        when(rankingRepository.findByClassId(1L)).thenReturn(rankings);

        rankingService.recalculateAllRankings(1L);

        verify(rankingRepository).saveAll(anyList());
        // Should be sorted desc by experience
        assertThat(rankings.get(0).getUserId()).isEqualTo(2L);
        assertThat(rankings.get(1).getUserId()).isEqualTo(1L);
    }

    @Test
    void recalculateAllRankings_NoClassId_ShouldSortAndSaveAll() {
        when(rankingRepository.findAll()).thenReturn(new ArrayList<>());
        rankingService.recalculateAllRankings(null);
        verify(rankingRepository).saveAll(anyList());
    }

    @Test
    void getCurrentRanking_ShouldReturnPagedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Ranking> rankings = List.of(ranking);

        when(rankingRepository.findAllOrderByExperience(1L)).thenReturn(rankings);
        // mapToRankingDTO calls
        // ranking.getUser() is set in setUp
        // ranking.getClassEntity() is set in setUp

        RankingResponseDTO response = rankingService.getCurrentRanking(1L, "experience", pageable);

        assertThat(response.getRankings()).hasSize(1);
        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getRankings().get(0).getUsername()).isEqualTo("testuser");
    }

    @Test
    void getCurrentRanking_SortByCompetence() {
        Pageable pageable = PageRequest.of(0, 10);
        when(rankingRepository.findAllOrderByCompetence(1L)).thenReturn(List.of(ranking));
        rankingService.getCurrentRanking(1L, "competence", pageable);
        verify(rankingRepository).findAllOrderByCompetence(1L);
    }

    @Test
    void getMonthlyRanking_ExistingSnapshot_ShouldReturnResults() {
        String monthYear = "2023-10";
        Pageable pageable = PageRequest.of(0, 10);

        RankingMonthly rm = new RankingMonthly();
        rm.setUserId(1L);
        rm.setMonthYear(monthYear);
        rm.setUser(user);
        rm.setClassEntity(classEntity);

        when(rankingMonthlyRepository.findByMonthYearOrderByRankPositionAsc(monthYear)).thenReturn(List.of(rm));
        when(rankingMonthlyRepository.findByMonthYearOrderByExperience(monthYear, 1L)).thenReturn(List.of(rm));

        RankingResponseDTO response = rankingService.getMonthlyRanking(monthYear, 1L, "experience", pageable);

        assertThat(response.getRankings()).hasSize(1);
        verify(rankingMonthlyRepository, never()).save(any());
    }

    @Test
    void getMonthlyRanking_NoSnapshot_ShouldCreateSnapshot() {
        String monthYear = "2023-10";
        Pageable pageable = PageRequest.of(0, 10);

        when(rankingMonthlyRepository.findByMonthYearOrderByRankPositionAsc(monthYear))
                .thenReturn(Collections.emptyList());

        // Mock creation logic
        when(rankingRepository.findAllOrderByExperience(null)).thenReturn(List.of(ranking));
        when(rankingMonthlyRepository.findByUserIdAndMonthYearAndClassId(1L, monthYear, 1L))
                .thenReturn(Optional.empty());

        // Mock fetch logic after creation
        RankingMonthly rm = new RankingMonthly();
        rm.setUserId(1L);
        rm.setUser(user);
        when(rankingMonthlyRepository.findByMonthYearOrderByExperience(monthYear, 1L)).thenReturn(List.of(rm));

        rankingService.getMonthlyRanking(monthYear, 1L, "experience", pageable);

        verify(rankingMonthlyRepository).save(any(RankingMonthly.class));
    }

    @Test
    void getUserRankPosition_Monthly_ShouldReturnPosition() {
        String monthYear = "2023-10";
        RankingMonthly rm = new RankingMonthly();
        rm.setUserId(1L);
        rm.setRankPosition(5);
        rm.setUser(user);
        rm.setClassEntity(classEntity);

        when(rankingMonthlyRepository.findByUserIdAndMonthYear(1L, monthYear)).thenReturn(Optional.of(rm));
        when(rankingMonthlyRepository.findByMonthYearOrderByRankPositionAsc(monthYear)).thenReturn(List.of(rm)); // For
                                                                                                                 // total
                                                                                                                 // users
                                                                                                                 // count

        UserRankPositionDTO dto = rankingService.getUserRankPosition(1L, monthYear, 1L, "experience");

        assertThat(dto).isNotNull();
        assertThat(dto.getRankPosition()).isEqualTo(5);
    }

    @Test
    void getUserRankPosition_Current_ShouldReturnPosition() {
        when(rankingRepository.findByUserId(1L)).thenReturn(Optional.of(ranking));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));

        // Mock getRankingsBySortBy
        when(rankingRepository.findAllOrderByExperience(1L)).thenReturn(List.of(ranking));

        UserRankPositionDTO dto = rankingService.getUserRankPosition(1L, null, 1L, "experience");

        assertThat(dto).isNotNull();
        assertThat(dto.getRankPosition()).isEqualTo(1);
    }

    @Test
    void getUserRankPosition_NotFound_ShouldReturnNull() {
        when(rankingRepository.findByUserId(99L)).thenReturn(Optional.empty());
        UserRankPositionDTO dto = rankingService.getUserRankPosition(99L, null, 1L, "experience");
        assertThat(dto).isNull();
    }
}

