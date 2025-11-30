package com.tim.appTim.service;

import com.tim.appTim.dto.RankingDTO;
import com.tim.appTim.dto.RankingResponseDTO;
import com.tim.appTim.dto.UserRankPositionDTO;
import com.tim.appTim.entity.Class;
import com.tim.appTim.entity.ClassMember;
import com.tim.appTim.entity.Ranking;
import com.tim.appTim.entity.RankingMonthly;
import com.tim.appTim.entity.User;
import com.tim.appTim.entity.UserGamificationStats;
import com.tim.appTim.repository.ClassMemberRepository;
import com.tim.appTim.repository.ClassRepository;
import com.tim.appTim.repository.RankingMonthlyRepository;
import com.tim.appTim.repository.RankingRepository;
import com.tim.appTim.repository.UserGamificationStatsRepository;
import com.tim.appTim.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class RankingService {

    private final RankingRepository rankingRepository;
    private final RankingMonthlyRepository rankingMonthlyRepository;
    private final UserGamificationStatsRepository statsRepository;
    private final UserRepository userRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassRepository classRepository;

    public RankingService(
            RankingRepository rankingRepository,
            RankingMonthlyRepository rankingMonthlyRepository,
            UserGamificationStatsRepository statsRepository,
            UserRepository userRepository,
            ClassMemberRepository classMemberRepository,
            ClassRepository classRepository) {
        this.rankingRepository = rankingRepository;
        this.rankingMonthlyRepository = rankingMonthlyRepository;
        this.statsRepository = statsRepository;
        this.userRepository = userRepository;
        this.classMemberRepository = classMemberRepository;
        this.classRepository = classRepository;
    }

    @Transactional
    public void updateRanking(Long userId) {

        UserGamificationStats stats = statsRepository.findByUserId(userId)
                .orElse(null);
        
        if (stats == null) {

            stats = new UserGamificationStats();
            stats.setUserId(userId);
            stats.setTotalDiligence(0);
            stats.setTotalCompetence(0);
            stats.setTotalExperience(0);
        }


        Long classId = null;
        List<ClassMember> classMembers = classMemberRepository.findByUserId(userId);
        if (!classMembers.isEmpty()) {
            classId = classMembers.get(0).getClassId();
        }


        Ranking ranking = rankingRepository.findByUserId(userId)
                .orElse(new Ranking());

        ranking.setUserId(userId);
        ranking.setTotalDiligenceScore(stats.getTotalDiligence());
        ranking.setTotalCompetenceScore(stats.getTotalCompetence());
        ranking.setTotalExperienceScore(stats.getTotalExperience());
        ranking.setClassId(classId);
        ranking.setUpdateTime(LocalDateTime.now());

        rankingRepository.save(ranking);
    }


    @Transactional
    public void recalculateAllRankings(Long classId) {

        List<Ranking> rankings;
        if (classId != null) {
            rankings = rankingRepository.findByClassId(classId);
        } else {
            rankings = rankingRepository.findAll();
        }

        rankings.sort((r1, r2) -> {

            int expCompare = Integer.compare(
                r2.getTotalExperienceScore() != null ? r2.getTotalExperienceScore() : 0,
                r1.getTotalExperienceScore() != null ? r1.getTotalExperienceScore() : 0
            );
            if (expCompare != 0) return expCompare;

            int compCompare = Integer.compare(
                r2.getTotalCompetenceScore() != null ? r2.getTotalCompetenceScore() : 0,
                r1.getTotalCompetenceScore() != null ? r1.getTotalCompetenceScore() : 0
            );
            if (compCompare != 0) return compCompare;

            int dilCompare = Integer.compare(
                r2.getTotalDiligenceScore() != null ? r2.getTotalDiligenceScore() : 0,
                r1.getTotalDiligenceScore() != null ? r1.getTotalDiligenceScore() : 0
            );
            if (dilCompare != 0) return dilCompare;

            return Long.compare(r1.getUserId(), r2.getUserId());
        });

        rankingRepository.saveAll(rankings);
    }

    @Transactional(readOnly = true)
    public RankingResponseDTO getCurrentRanking(Long classId, String sortBy, Pageable pageable) {
        List<Ranking> rankings;

        if (sortBy == null || sortBy.equals("experience")) {
            rankings = rankingRepository.findAllOrderByExperience(classId);
        } else if (sortBy.equals("competence")) {
            rankings = rankingRepository.findAllOrderByCompetence(classId);
        } else if (sortBy.equals("diligence")) {
            rankings = rankingRepository.findAllOrderByDiligence(classId);
        } else if (sortBy.equals("total")) {
            rankings = rankingRepository.findAllOrderByTotal(classId);
        } else {

            rankings = rankingRepository.findAllOrderByExperience(classId);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), rankings.size());
        List<Ranking> pagedRankings = start < rankings.size() ? rankings.subList(start, end) : new ArrayList<>();

        List<RankingDTO> rankingDTOs = new ArrayList<>();
        for (int i = 0; i < pagedRankings.size(); i++) {
            Ranking ranking = pagedRankings.get(i);
            int rankPosition = start + i + 1;
            rankingDTOs.add(mapToRankingDTO(ranking, rankPosition));
        }

        RankingResponseDTO response = new RankingResponseDTO();
        response.setRankings(rankingDTOs);
        response.setTotal((long) rankings.size());
        response.setPage(pageable.getPageNumber());
        response.setSize(pageable.getPageSize());
        response.setSortBy(sortBy != null ? sortBy : "experience");

        return response;
    }

    @Transactional
    public RankingResponseDTO getMonthlyRanking(String monthYear, Long classId, String sortBy, Pageable pageable) {

        List<RankingMonthly> existingRankings = rankingMonthlyRepository.findByMonthYearOrderByRankPositionAsc(monthYear);

        if (existingRankings.isEmpty()) {
            createMonthlySnapshot(monthYear);
        }
        
        List<RankingMonthly> rankings;

        if (sortBy == null || sortBy.equals("experience")) {
            rankings = rankingMonthlyRepository.findByMonthYearOrderByExperience(monthYear, classId);
        } else if (sortBy.equals("competence")) {
            rankings = rankingMonthlyRepository.findByMonthYearOrderByCompetence(monthYear, classId);
        } else if (sortBy.equals("diligence")) {
            rankings = rankingMonthlyRepository.findByMonthYearOrderByDiligence(monthYear, classId);
        } else if (sortBy.equals("total")) {
            rankings = rankingMonthlyRepository.findByMonthYearOrderByTotal(monthYear, classId);
        } else {

            rankings = rankingMonthlyRepository.findByMonthYearOrderByExperience(monthYear, classId);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), rankings.size());
        List<RankingMonthly> pagedRankings = start < rankings.size() ? rankings.subList(start, end) : new ArrayList<>();

        List<RankingDTO> rankingDTOs = pagedRankings.stream()
                .map(rm -> mapMonthlyToRankingDTO(rm))
                .collect(Collectors.toList());

        RankingResponseDTO response = new RankingResponseDTO();
        response.setRankings(rankingDTOs);
        response.setTotal((long) rankings.size());
        response.setPage(pageable.getPageNumber());
        response.setSize(pageable.getPageSize());
        response.setSortBy(sortBy != null ? sortBy : "experience");
        response.setMonthYear(monthYear);

        return response;
    }

    @Transactional(readOnly = true)
    public UserRankPositionDTO getUserRankPosition(Long userId, String monthYear, Long classId, String sortBy) {
        if (monthYear != null && !monthYear.isEmpty()) {

            Optional<RankingMonthly> rankingMonthly = rankingMonthlyRepository.findByUserIdAndMonthYear(userId, monthYear);
            if (rankingMonthly.isPresent()) {
                return mapMonthlyToUserRankPositionDTO(rankingMonthly.get(), monthYear, classId, sortBy);
            }
            return null;
        } else {

            Optional<Ranking> ranking = rankingRepository.findByUserId(userId);
            if (ranking.isPresent()) {
                return mapToUserRankPositionDTO(ranking.get(), classId, sortBy);
            }
            return null;
        }
    }

    @Transactional
    public void createMonthlySnapshot(String monthYear) {
        if (monthYear == null || monthYear.isEmpty()) {

            monthYear = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }

        List<Ranking> rankings = rankingRepository.findAllOrderByExperience(null);

        int position = 1;
        for (Ranking ranking : rankings) {

            RankingMonthly monthly = rankingMonthlyRepository
                    .findByUserIdAndMonthYearAndClassId(ranking.getUserId(), monthYear, ranking.getClassId())
                    .orElse(new RankingMonthly());

            monthly.setUserId(ranking.getUserId());
            monthly.setMonthYear(monthYear);
            monthly.setTotalDiligenceScore(ranking.getTotalDiligenceScore());
            monthly.setTotalCompetenceScore(ranking.getTotalCompetenceScore());
            monthly.setTotalExperienceScore(ranking.getTotalExperienceScore());
            monthly.setRankPosition(position);
            monthly.setClassId(ranking.getClassId());

            rankingMonthlyRepository.save(monthly);
            position++;
        }
    }

    private RankingDTO mapToRankingDTO(Ranking ranking, int rankPosition) {
        RankingDTO dto = new RankingDTO();
        dto.setUserId(ranking.getUserId());
        dto.setTotalDiligenceScore(ranking.getTotalDiligenceScore());
        dto.setTotalCompetenceScore(ranking.getTotalCompetenceScore());
        dto.setTotalExperienceScore(ranking.getTotalExperienceScore());
        dto.setRankPosition(rankPosition);
        dto.setClassId(ranking.getClassId());

        if (ranking.getUser() != null) {
            dto.setUsername(ranking.getUser().getUsername());
            dto.setDisplayName(ranking.getUser().getFirstName() + " " + ranking.getUser().getLastName());
            dto.setProfileImage(ranking.getUser().getProfileImage());
        } else {
            User user = userRepository.findById(ranking.getUserId()).orElse(null);
            if (user != null) {
                dto.setUsername(user.getUsername());
                dto.setDisplayName(user.getFirstName() + " " + user.getLastName());
                dto.setProfileImage(user.getProfileImage());
            }
        }

        if (ranking.getClassEntity() != null) {
            dto.setClassName(ranking.getClassEntity().getClassName());
        } else if (ranking.getClassId() != null) {
            Optional<Class> classOpt = classRepository.findById(ranking.getClassId());
            if (classOpt.isPresent()) {
                dto.setClassName(classOpt.get().getClassName());
            }
        }

        return dto;
    }

    private RankingDTO mapMonthlyToRankingDTO(RankingMonthly monthly) {
        RankingDTO dto = new RankingDTO();
        dto.setUserId(monthly.getUserId());
        dto.setTotalDiligenceScore(monthly.getTotalDiligenceScore());
        dto.setTotalCompetenceScore(monthly.getTotalCompetenceScore());
        dto.setTotalExperienceScore(monthly.getTotalExperienceScore());
        dto.setRankPosition(monthly.getRankPosition());
        dto.setClassId(monthly.getClassId());

        if (monthly.getUser() != null) {
            dto.setUsername(monthly.getUser().getUsername());
            dto.setDisplayName(monthly.getUser().getFirstName() + " " + monthly.getUser().getLastName());
            dto.setProfileImage(monthly.getUser().getProfileImage());
        } else {
            User user = userRepository.findById(monthly.getUserId()).orElse(null);
            if (user != null) {
                dto.setUsername(user.getUsername());
                dto.setDisplayName(user.getFirstName() + " " + user.getLastName());
                dto.setProfileImage(user.getProfileImage());
            }
        }

        if (monthly.getClassEntity() != null) {
            dto.setClassName(monthly.getClassEntity().getClassName());
        } else if (monthly.getClassId() != null) {
            Optional<Class> classOpt = classRepository.findById(monthly.getClassId());
            if (classOpt.isPresent()) {
                dto.setClassName(classOpt.get().getClassName());
            }
        }

        return dto;
    }

    private UserRankPositionDTO mapToUserRankPositionDTO(Ranking ranking, Long classId, String sortBy) {
        UserRankPositionDTO dto = new UserRankPositionDTO();
        dto.setUserId(ranking.getUserId());
        dto.setTotalDiligenceScore(ranking.getTotalDiligenceScore());
        dto.setTotalCompetenceScore(ranking.getTotalCompetenceScore());
        dto.setTotalExperienceScore(ranking.getTotalExperienceScore());
        dto.setClassId(ranking.getClassId());

        User user = userRepository.findById(ranking.getUserId()).orElse(null);
        if (user != null) {
            dto.setUsername(user.getUsername());
        }

        if (ranking.getClassId() != null) {
            Optional<Class> classOpt = classRepository.findById(ranking.getClassId());
            if (classOpt.isPresent()) {
                dto.setClassName(classOpt.get().getClassName());
            }
        }

        List<Ranking> rankings = getRankingsBySortBy(classId, sortBy);
        int position = rankings.indexOf(ranking) + 1;
        dto.setRankPosition(position);
        dto.setTotalUsers((long) rankings.size());

        return dto;
    }

    private UserRankPositionDTO mapMonthlyToUserRankPositionDTO(RankingMonthly monthly, String monthYear, Long classId, String sortBy) {
        UserRankPositionDTO dto = new UserRankPositionDTO();
        dto.setUserId(monthly.getUserId());
        dto.setTotalDiligenceScore(monthly.getTotalDiligenceScore());
        dto.setTotalCompetenceScore(monthly.getTotalCompetenceScore());
        dto.setTotalExperienceScore(monthly.getTotalExperienceScore());
        dto.setRankPosition(monthly.getRankPosition());
        dto.setMonthYear(monthYear);
        dto.setClassId(monthly.getClassId());

        User user = userRepository.findById(monthly.getUserId()).orElse(null);
        if (user != null) {
            dto.setUsername(user.getUsername());
        }

        if (monthly.getClassId() != null) {
            Optional<Class> classOpt = classRepository.findById(monthly.getClassId());
            if (classOpt.isPresent()) {
                dto.setClassName(classOpt.get().getClassName());
            }
        }

        List<RankingMonthly> allMonthly = rankingMonthlyRepository.findByMonthYearOrderByRankPositionAsc(monthYear);
        dto.setTotalUsers((long) allMonthly.size());

        return dto;
    }

    private List<Ranking> getRankingsBySortBy(Long classId, String sortBy) {
        if (sortBy == null || sortBy.equals("experience")) {
            return rankingRepository.findAllOrderByExperience(classId);
        } else if (sortBy.equals("competence")) {
            return rankingRepository.findAllOrderByCompetence(classId);
        } else if (sortBy.equals("diligence")) {
            return rankingRepository.findAllOrderByDiligence(classId);
        } else if (sortBy.equals("total")) {
            return rankingRepository.findAllOrderByTotal(classId);
        } else {
            return rankingRepository.findAllOrderByExperience(classId);
        }
    }
}

