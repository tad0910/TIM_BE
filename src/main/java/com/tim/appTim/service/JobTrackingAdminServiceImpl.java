package com.tim.appTim.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tim.appTim.dto.AdminJobLeadDTO;
import com.tim.appTim.dto.JobActivityDTO;
import com.tim.appTim.dto.JobTrackingOverviewClassDTO;
import com.tim.appTim.dto.JobTrackingOverviewFilter;
import com.tim.appTim.dto.JobTrackingOverviewSummaryDTO;
import com.tim.appTim.dto.JobTrackingRowDTO;
import com.tim.appTim.dto.JobTrackingUpdateRequest;
import com.tim.appTim.entity.ClassMember;
import com.tim.appTim.entity.Class;
import com.tim.appTim.entity.JobActivity;
import com.tim.appTim.entity.JobApplication;
import com.tim.appTim.entity.JobActivityType;
import com.tim.appTim.entity.JobLead;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.ClassMemberRepository;
import com.tim.appTim.repository.ClassRepository;
import com.tim.appTim.repository.JobActivityRepository;
import com.tim.appTim.repository.JobApplicationRepository;
import com.tim.appTim.repository.JobLeadRepository;
import com.tim.appTim.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobTrackingAdminServiceImpl implements JobTrackingAdminService {

    private final ClassRepository classRepository;
    private final ClassMemberRepository classMemberRepository;
    private final JobLeadRepository jobLeadRepository;
    private final JobActivityRepository jobActivityRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<JobTrackingRowDTO> getJobTrackingByClass(Long classId) {
        classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId));

        List<ClassMember> members = classMemberRepository.findByClassId(classId).stream()
                .filter(member -> member.getRole() == ClassMember.Role.sinh_vien)
                .collect(Collectors.toList());

        if (members.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> studentIds = members.stream()
                .map(ClassMember::getUserId)
                .collect(Collectors.toList());

        Map<Long, JobLead> latestLeadByStudent = collectLatestLeads(studentIds);
        Map<Long, JobApplication> bestApplicationByStudent = collectBestApplications(classId);

        return members.stream()
                .map(member -> buildRowDTO(
                        member,
                        latestLeadByStudent.get(member.getUserId()),
                        bestApplicationByStudent.get(member.getUserId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public JobTrackingRowDTO updateJobInterest(Long classId, Long studentId, JobTrackingUpdateRequest request) {
        classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId));

        ClassMember member = classMemberRepository.findByClassIdAndUserId(classId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Học viên không thuộc lớp này"));

        JobLead lead = jobLeadRepository.findTopByStudentIdOrderByCreatedAtDesc(studentId)
                .orElseGet(() -> createPlaceholderLead(member));

        lead.setJobInterest(request.isJobInterest());
        jobLeadRepository.save(lead);

        return buildRowDTO(
                member,
                lead,
                null);
    }

    private Map<Long, JobLead> collectLatestLeads(List<Long> studentIds) {
        List<JobLead> leads = studentIds.isEmpty()
                ? Collections.emptyList()
                : jobLeadRepository.findByStudentIdIn(studentIds);

        Map<Long, JobLead> latestLead = new HashMap<>();
        for (JobLead lead : leads) {
            if (lead.getStudent() == null || lead.getStudent().getId() == null) {
                continue;
            }
            Long studentId = lead.getStudent().getId();
            JobLead existing = latestLead.get(studentId);
            if (existing == null || isAfter(lead.getCreatedAt(), existing.getCreatedAt())) {
                latestLead.put(studentId, lead);
            }
        }
        return latestLead;
    }

    private Map<Long, JobApplication> collectBestApplications(Long classId) {
        List<JobApplication> applications = jobApplicationRepository.findByClassEntityId(classId);
        Map<Long, List<JobApplication>> grouped = applications.stream()
                .filter(app -> app.getStudent() != null && app.getStudent().getId() != null)
                .collect(Collectors.groupingBy(app -> app.getStudent().getId()));

        Map<Long, JobApplication> result = new HashMap<>();
        for (Map.Entry<Long, List<JobApplication>> entry : grouped.entrySet()) {
            JobApplication best = entry.getValue().stream()
                    .max(Comparator.comparing(JobApplication::getAppliedAt, Comparator.nullsLast(LocalDateTime::compareTo)))
                    .orElse(null);
            if (best != null) {
                result.put(entry.getKey(), best);
            }
        }
        return result;
    }

    private JobTrackingRowDTO buildRowDTO(ClassMember member, JobLead lead, JobApplication fallbackApplication) {
        User student = Optional.ofNullable(member.getUser())
                .orElseGet(() -> userRepository.findById(member.getUserId()).orElse(null));

        List<JobActivity> activities = lead == null ? Collections.emptyList()
                : jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(lead.getId());

        JobActivity latestActivity = null;

        String studentName = student != null ?
                List.of(student.getFirstName(), student.getLastName()).stream()
                        .filter(part -> part != null && !part.isBlank())
                        .collect(Collectors.joining(" "))
                : "";
        if (studentName.isBlank() && student != null) {
            studentName = student.getUsername();
        }

        String companyName = "-";
        String offerAmount = "-";
        String probationSalary = "-";
        String officialSalary = "-";
        String statusCode = "NONE";
        String statusLabel = "Chưa có đầu mối";
        boolean jobInterest = true;
        LocalDateTime lastUpdated = null;

        if (lead != null) {
            companyName = defaultString(lead.getCompanyName(), "-");
            statusCode = lead.getStatus() != null ? lead.getStatus().name() : statusCode;
            statusLabel = lead.getStatus() != null ? lead.getStatus().getDisplayName() : statusLabel;
            jobInterest = lead.isJobInterest();
            lastUpdated = lead.getCreatedAt();
        }

        for (JobActivity activity : activities) {
            if (activity == null || activity.getActivityType() == null) {
                continue;
            }

            if (latestActivity == null || isAfter(activity.getCreatedAt(), latestActivity.getCreatedAt())) {
                latestActivity = activity;
            }

            String salaryAmount = activity.getSalaryAmount();
            if (salaryAmount != null && !salaryAmount.isBlank()) {
                salaryAmount = salaryAmount.trim();
                JobActivityType activityType = activity.getActivityType();
                switch (activityType) {
                    case OFFER_RECEIVED -> {
                        if ("-".equals(offerAmount)) {
                            offerAmount = salaryAmount;
                        }
                    }
                    case PROBATION_CONTRACT -> {
                        if ("-".equals(probationSalary)) {
                            probationSalary = salaryAmount;
                        }
                    }
                    case OFFICIAL_CONTRACT -> {
                        if ("-".equals(officialSalary)) {
                            officialSalary = salaryAmount;
                        }
                    }
                    default -> {
                    }
                }
            }
        }

        if (latestActivity != null) {
            lastUpdated = latestActivity.getCreatedAt();
        }

        if (lead == null && latestActivity != null) {
            statusCode = latestActivity.getActivityType().name();
            statusLabel = latestActivity.getActivityType().getDisplayName();
        }

        if (lead == null && fallbackApplication != null) {
            companyName = fallbackApplication.getCompany() != null
                    ? fallbackApplication.getCompany().getName()
                    : "-";
            statusCode = fallbackApplication.getStatus().name();
            statusLabel = fallbackApplication.getStatus().getDisplayName();
            lastUpdated = fallbackApplication.getAppliedAt();
        }

        return JobTrackingRowDTO.builder()
                .studentId(member.getUserId())
                .studentName(studentName)
                .username(student != null ? student.getUsername() : null)
                .companyName(companyName)
                .jobStatusCode(statusCode)
                .jobStatusLabel(statusLabel)
                .offerAmount(offerAmount)
                .probationSalary(probationSalary)
                .officialSalary(officialSalary)
                .jobInterest(jobInterest)
                .lastUpdated(lastUpdated)
                .build();
    }

    private JobLead createPlaceholderLead(ClassMember member) {
        User student = Optional.ofNullable(member.getUser())
                .orElseGet(() -> userRepository.findById(member.getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin học viên")));

        JobLead lead = new JobLead();
        lead.setStudent(student);
        lead.setCompanyName("Chưa xác định");
        lead.setStatus(JobLead.LeadStatus.NEW);
        lead.setCreatedAt(LocalDateTime.now());
        lead.setJobInterest(true);
        return jobLeadRepository.save(lead);
    }

    private boolean isAfter(LocalDateTime current, LocalDateTime previous) {
        if (current == null) {
            return false;
        }
        if (previous == null) {
            return true;
        }
        return current.isAfter(previous);
    }

    private String defaultString(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    @Override
    @Transactional(readOnly = true)
    public JobTrackingOverviewSummaryDTO getJobTrackingOverview(JobTrackingOverviewFilter filter) {
        List<Class> classes = classRepository.findAll();
        JobTrackingOverviewFilter safeFilter = filter != null ? filter : new JobTrackingOverviewFilter();

        List<JobTrackingOverviewClassDTO> classSummaries = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime recentThreshold = now.minusDays(14);

        int totalClasses = 0;
        int totalStudents = 0;
        int totalOffers = 0;
        int totalActiveInterest = 0;
        int totalRecentUpdates = 0;

        for (Class clazz : classes) {
            if (safeFilter.getProgramId() != null && !Objects.equals(clazz.getProgramId(), safeFilter.getProgramId())) {
                continue;
            }

            if (safeFilter.getMentorId() != null) {
                boolean mentorBelongs = classMemberRepository.existsByClassIdAndUserIdAndRole(
                        clazz.getId(), safeFilter.getMentorId(), ClassMember.Role.giao_vien);
                if (!mentorBelongs) {
                    continue;
                }
            }

            List<JobTrackingRowDTO> rows = getJobTrackingByClass(clazz.getId());
            int classStudents = rows.size();
            int classOffers = (int) rows.stream()
                    .map(JobTrackingRowDTO::getJobStatusCode)
                    .filter(status -> status != null &&
                            ("OFFER".equalsIgnoreCase(status) || "OFFICIAL".equalsIgnoreCase(status)))
                    .count();

            int classActiveInterest = (int) rows.stream().filter(JobTrackingRowDTO::isJobInterest).count();
            int classRecentUpdates = (int) rows.stream()
                    .map(JobTrackingRowDTO::getLastUpdated)
                    .filter(Objects::nonNull)
                    .filter(timestamp -> !timestamp.isBefore(recentThreshold))
                    .count();

            int classTotalLeads = (int) rows.stream()
                    .map(JobTrackingRowDTO::getJobStatusCode)
                    .filter(status -> status != null && !status.equalsIgnoreCase("NONE"))
                    .count();

            LocalDateTime lastActivityAt = rows.stream()
                    .map(JobTrackingRowDTO::getLastUpdated)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            int classRecentPercent = classStudents == 0
                    ? 0
                    : Math.round((classRecentUpdates * 100f) / classStudents);

            classSummaries.add(JobTrackingOverviewClassDTO.builder()
                    .classId(clazz.getId())
                    .className(clazz.getClassName())
                    .programId(clazz.getProgramId())
                    .programName(clazz.getProgram() != null ? clazz.getProgram().getName() : null)
                    .totalStudents(classStudents)
                    .totalLeads(classTotalLeads)
                    .offerCount(classOffers)
                    .activeJobInterest(classActiveInterest)
                    .updatedWithin14Days(classRecentUpdates)
                    .recentUpdatePercent(classRecentPercent)
                    .lastActivityAt(lastActivityAt)
                    .build());

            totalClasses++;
            totalStudents += classStudents;
            totalOffers += classOffers;
            totalActiveInterest += classActiveInterest;
            totalRecentUpdates += classRecentUpdates;
        }

        int recentPercent = totalStudents == 0 ? 0 : Math.round((totalRecentUpdates * 100f) / totalStudents);

        return JobTrackingOverviewSummaryDTO.builder()
                .totalClasses(totalClasses)
                .totalStudents(totalStudents)
                .totalOffers(totalOffers)
                .activeJobInterest(totalActiveInterest)
                .updatedWithin14Days(totalRecentUpdates)
                .recentUpdatePercent(recentPercent)
                .classes(classSummaries)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminJobLeadDTO> getStudentLeads(Long classId, Long studentId) {
        classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId));

        classMemberRepository.findByClassIdAndUserId(classId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Học viên không thuộc lớp này"));

        List<JobLead> leads = jobLeadRepository.findByStudentIdOrderByCreatedAtDesc(studentId);

        return leads.stream()
                .map(lead -> AdminJobLeadDTO.builder()
                        .id(lead.getId())
                        .companyName(lead.getCompanyName())
                        .shortName(lead.getShortName())
                        .address(lead.getAddress())
                        .website(lead.getWebsite())
                        .statusCode(lead.getStatus() != null ? lead.getStatus().name() : null)
                        .statusLabel(lead.getStatus() != null ? lead.getStatus().getDisplayName() : "Chưa có trạng thái")
                        .jobInterest(lead.isJobInterest())
                        .createdAt(lead.getCreatedAt())
                        .fromAdmin(false)
                        .activities(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(lead.getId()).stream()
                                .map(this::toJobActivityDTO)
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    private JobActivityDTO toJobActivityDTO(JobActivity activity) {
        return new JobActivityDTO(
                activity.getId(),
                activity.getJobLeadId(),
                activity.getActivityType() != null ? activity.getActivityType().name() : null,
                activity.getContent(),
                activity.getHappenedAt(),
                activity.getCreatedAt(),
                activity.getSalaryAmount(),
                activity.getNote(),
                activity.getFileUrl()
        );
    }

    @Override
    @Transactional
    public AdminJobLeadDTO createJobLead(Long classId, Long studentId, String companyName, String shortName, String address, String website) {
        classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId));

        ClassMember member = classMemberRepository.findByClassIdAndUserId(classId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Học viên không thuộc lớp này"));

        User student = Optional.ofNullable(member.getUser())
                .orElseGet(() -> userRepository.findById(member.getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin học viên")));

        JobLead lead = new JobLead();
        lead.setStudent(student);
        lead.setCompanyName(companyName);
        lead.setShortName(shortName);
        lead.setAddress(address);
        lead.setWebsite(website);
        lead.setStatus(JobLead.LeadStatus.NEW);
        lead.setJobInterest(true);
        lead.setCreatedAt(LocalDateTime.now());

        JobLead savedLead = jobLeadRepository.save(lead);

        return AdminJobLeadDTO.builder()
                .id(savedLead.getId())
                .companyName(savedLead.getCompanyName())
                .shortName(savedLead.getShortName())
                .address(savedLead.getAddress())
                .website(savedLead.getWebsite())
                .statusCode(savedLead.getStatus().name())
                .statusLabel(savedLead.getStatus().getDisplayName())
                .jobInterest(savedLead.isJobInterest())
                .createdAt(savedLead.getCreatedAt())
                .fromAdmin(true)
                .activities(Collections.emptyList())
                .build();
    }
}
