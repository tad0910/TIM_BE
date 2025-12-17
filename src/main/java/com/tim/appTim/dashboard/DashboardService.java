package com.tim.appTim.dashboard;

import com.tim.appTim.dashboard.dto.DashboardJobLead;
import com.tim.appTim.dashboard.dto.DashboardJobLeadsResponse;
import com.tim.appTim.dashboard.dto.DashboardPendingItem;
import com.tim.appTim.dashboard.dto.DashboardPendingResponse;
import com.tim.appTim.dashboard.dto.DashboardStatsResponse;
import com.tim.appTim.dashboard.dto.GrowthPoint;
import com.tim.appTim.dashboard.dto.GrowthResponse;
import com.tim.appTim.entity.JobLead;
import com.tim.appTim.entity.StudentForm;
import com.tim.appTim.repository.ClassRepository;
import com.tim.appTim.repository.ClassMemberRepository;
import com.tim.appTim.repository.JobLeadRepository;
import com.tim.appTim.repository.StudentFormRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ClassRepository classRepository;
    private final StudentFormRepository studentFormRepository;
    private final JobLeadRepository jobLeadRepository;
    private final ClassMemberRepository classMemberRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        int students = (int) classMemberRepository.countByRole(com.tim.appTim.entity.ClassMember.Role.sinh_vien);
        int activeMentors = (int) classMemberRepository.countByRole(com.tim.appTim.entity.ClassMember.Role.giao_vien);
        int activeClasses = (int) classRepository.count();
        int pendingForms = (int) studentFormRepository.countByStatus(StudentForm.FormStatus.PENDING);
        int jobPending = (int) jobLeadRepository.countByStatus(JobLead.LeadStatus.NEW);

        return new DashboardStatsResponse(students, pendingForms, jobPending, activeClasses, activeMentors);
    }

    @Transactional(readOnly = true)
    public DashboardPendingResponse getPendingRequests() {
        List<DashboardPendingItem> items = studentFormRepository
                .findTop5ByStatusOrderByCreatedAtDesc(StudentForm.FormStatus.PENDING)
                .stream()
                .map(form -> new DashboardPendingItem(
                        form.getTemplate() != null ? form.getTemplate().getName() : "Đơn học viên",
                        form.getReason(),
                        form.getTemplate() != null ? form.getTemplate().getCode() : "FORM",
                        form.getStatus().name()
                ))
                .toList();

        return new DashboardPendingResponse(items);
    }

    @Transactional(readOnly = true)
    public DashboardJobLeadsResponse getJobLeads() {
        List<DashboardJobLead> items = jobLeadRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(lead -> new DashboardJobLead(
                        lead.getCompanyName(),
                        lead.getShortName() != null ? lead.getShortName() : "N/A",
                        lead.getStatus() != null ? lead.getStatus().name() : JobLead.LeadStatus.NEW.name(),
                        null,
                        lead.getCreatedAt() != null ? toDateString(lead.getCreatedAt().toLocalDate()) : ""
                ))
                .toList();
        return new DashboardJobLeadsResponse(items);
    }

    @Transactional(readOnly = true)
    public GrowthResponse getStudentGrowth(int months) {
        int safeMonths = Math.max(1, Math.min(months, 24));
        LocalDate fromDate = LocalDate.now().minusMonths(safeMonths - 1).withDayOfMonth(1);
        LocalDateTime fromDateTime = fromDate.atStartOfDay();

        List<Object[]> rows = classMemberRepository.countStudentsByMonth(fromDateTime);
        Map<String, Integer> byMonth = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String ym = (String) row[0];
            Number cnt = (Number) row[1];
            byMonth.put(ym, cnt != null ? cnt.intValue() : 0);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        List<GrowthPoint> points = java.util.stream.IntStream.range(0, safeMonths)
                .mapToObj(i -> fromDate.plusMonths(i))
                .map(d -> {
                    String ym = d.format(fmt);
                    return new GrowthPoint(ym, byMonth.getOrDefault(ym, 0));
                })
                .toList();

        return new GrowthResponse(points);
    }

    private String toDateString(LocalDate date) {
        return date != null ? date.toString() : "";
    }
}
