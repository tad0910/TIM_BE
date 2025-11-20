package com.tim.appTim.service;

import com.tim.appTim.dto.*;
import com.tim.appTim.entity.*;
import com.tim.appTim.exception.ForbiddenException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate; // <-- THÊM IMPORT
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GradeServiceImpl implements GradeService, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(GradeServiceImpl.class);

    private final GradeRepository gradeRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassModuleRepository classModuleRepository;
    private final ClassModuleTeacherRepository classModuleTeacherRepository;
    private final UserRepository userRepository;
    private final GradeHistoryRepository gradeHistoryRepository;
    private final NotificationService notificationService;
    
    private ApplicationContext applicationContext;

    public GradeServiceImpl(GradeRepository gradeRepository,
                            ClassMemberRepository classMemberRepository,
                            ClassModuleRepository classModuleRepository,
                            ClassModuleTeacherRepository classModuleTeacherRepository,
                            UserRepository userRepository,
                            GradeHistoryRepository gradeHistoryRepository,
                            NotificationService notificationService) {
        this.gradeRepository = gradeRepository;
        this.classMemberRepository = classMemberRepository;
        this.classModuleRepository = classModuleRepository;
        this.classModuleTeacherRepository = classModuleTeacherRepository;
        this.userRepository = userRepository;
        this.gradeHistoryRepository = gradeHistoryRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    @Transactional
    public void batchCreateOrUpdateGrades(BatchGradeUpdateDTO dto, User teacher) {

            logger.info("========== BẮT ĐẦU batchCreateOrUpdateGrades ==========");
            Long classModuleId = dto.getClassModuleId();
            LocalDate entryDate = dto.getEntryDate();

            logger.info("Input: classModuleId={}, entryDate={}, teacherId={}, teacherName={}", 
                    classModuleId, entryDate, teacher.getId(), teacher.getUsername());
            logger.info("Số lượng scores cần xử lý: {}", dto.getScores() != null ? dto.getScores().size() : 0);

            logger.info("Đang validate teacher permission...");
            validateTeacherPermission(classModuleId, teacher.getId());
            logger.info("Teacher permission validated OK");

            logger.info("Đang tìm ClassModule với id={}...", classModuleId);
            ClassModule classModule = classModuleRepository.findById(classModuleId)
                    .orElseThrow(() -> new ResourceNotFoundException("ClassModule not found: " + classModuleId));
            String moduleName = classModule.getModule().getName();
            logger.info("Đã tìm thấy ClassModule: moduleName={}, classId={}", moduleName, classModule.getClassEntity().getId());

        for (StudentScoreEntryDTO studentEntry : dto.getScores()) {
            logger.info("--- Xử lý student entry ---");
            Long studentId = studentEntry.getStudentId();
            logger.info("studentId={}", studentId);
            
            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
            logger.info("Đã tìm thấy student: studentName={}", student.getUsername());

            Map<String, BigDecimal> scoresMap = studentEntry.getComponents();
            BigDecimal newTheoryScore = scoresMap.get("Điểm lý thuyết");
            BigDecimal newPracticeScore = scoresMap.get("Điểm thực hành");
            logger.info("Scores từ request: theoryScore={}, practiceScore={}", newTheoryScore, newPracticeScore);

            logger.info("Đang tìm existing grade...");
            Optional<Grade> existingGradeOpt = gradeRepository
                    .findByStudentIdAndClassModuleIdAndStatus(studentId, classModuleId, Grade.Status.ACTIVE);

            Grade grade;
            boolean isNewGrade = existingGradeOpt.isEmpty();
            logger.info("isNewGrade={}", isNewGrade);

            if (isNewGrade) {
                grade = new Grade();
                grade.setStudent(student);
                grade.setClassModule(classModule);
                grade.setStatus(Grade.Status.ACTIVE);
            } else {
                grade = existingGradeOpt.get();
            }

            BigDecimal oldTheoryScore = grade.getTheoryScore();
            BigDecimal oldPracticeScore = grade.getPracticeScore();

            grade.setEnteredBy(teacher);
            grade.setEntryDate(entryDate);
            grade.setTheoryScore(newTheoryScore);
            grade.setPracticeScore(newPracticeScore);

            Grade savedGrade;
            try {
                logger.info("========== BẮT ĐẦU LƯU GRADE ==========");
                logger.info("studentId={}, classModuleId={}, theoryScore={}, practiceScore={}, entryDate={}", 
                        studentId, classModuleId, newTheoryScore, newPracticeScore, entryDate);
                logger.info("Grade entity trước khi save: id={}, status={}, student={}, classModule={}", 
                        grade.getId(), grade.getStatus(), grade.getStudent() != null ? grade.getStudent().getId() : "null", 
                        grade.getClassModule() != null ? grade.getClassModule().getId() : "null");
                
                savedGrade = gradeRepository.save(grade);
                
                logger.info("Đã lưu Grade thành công: gradeId={}", savedGrade.getId());
                logger.info("========== KẾT THÚC LƯU GRADE ==========");
            } catch (Exception e) {
                logger.error("========== LỖI KHI LƯU GRADE ==========");
                logger.error("studentId={}, classModuleId={}", studentId, classModuleId);
                logger.error("Exception type: {}", e.getClass().getName());
                logger.error("Exception message: {}", e.getMessage());
                logger.error("Exception cause: {}", e.getCause() != null ? e.getCause().getMessage() : "null");
                logger.error("Full stack trace:", e);
                logger.error("=======================================");
                throw new RuntimeException("Không thể lưu điểm số: " + e.getMessage(), e);
            }

            boolean theoryChanged = isNewGrade ||
                    (newTheoryScore != null && oldTheoryScore == null) ||
                    (newTheoryScore == null && oldTheoryScore != null) ||
                    (newTheoryScore != null && oldTheoryScore != null && newTheoryScore.compareTo(oldTheoryScore) != 0);
            if (theoryChanged) {
                try {
                    Notification.NotificationType type = (isNewGrade || oldTheoryScore == null) ?
                            Notification.NotificationType.GRADE_NEW : Notification.NotificationType.GRADE_UPDATED;

                    saveHistoryAndNotify(savedGrade, "Điểm lý thuyết", oldTheoryScore, newTheoryScore,
                            teacher, student, moduleName, type);
                } catch (Exception e) {
                    logger.error("Lỗi khi lưu lịch sử/thông báo cho điểm lý thuyết (studentId: {}, gradeId: {}): {}", 
                            studentId, savedGrade.getId(), e.getMessage(), e);
                }
            }

            boolean practiceChanged = isNewGrade ||
                    (newPracticeScore != null && oldPracticeScore == null) ||
                    (newPracticeScore == null && oldPracticeScore != null) ||
                    (newPracticeScore != null && oldPracticeScore != null && newPracticeScore.compareTo(oldPracticeScore) != 0);
            if (practiceChanged) {
                try {
                    Notification.NotificationType type = (isNewGrade || oldPracticeScore == null) ?
                            Notification.NotificationType.GRADE_NEW : Notification.NotificationType.GRADE_UPDATED;

                    saveHistoryAndNotify(savedGrade, "Điểm thực hành", oldPracticeScore, newPracticeScore,
                            teacher, student, moduleName, type);
                } catch (Exception e) {
                    logger.error("Lỗi khi lưu lịch sử/thông báo cho điểm thực hành (studentId: {}, gradeId: {}): {}", 
                            studentId, savedGrade.getId(), e.getMessage(), e);
                }
            }

            logger.info("--- Hoàn thành xử lý student entry cho studentId={} ---", studentId);
        }
        
        boolean isActualTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
        boolean isCurrentTransactionReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        logger.info("Transaction status: isActive={}, isReadOnly={}", isActualTransactionActive, isCurrentTransactionReadOnly);
        
        logger.info("========== HOÀN THÀNH batchCreateOrUpdateGrades THÀNH CÔNG ==========");

    }

    private void saveHistoryAndNotify(Grade savedGrade, String componentName,
                                      BigDecimal oldScore, BigDecimal newScore,
                                      User teacher, User student, String moduleName,
                                      Notification.NotificationType type) {

        try {
            GradeHistory history = new GradeHistory();
            history.setGrade(savedGrade);
            history.setComponentChanged(componentName != null ? componentName : "unknown");
            history.setOldScore(oldScore);
            history.setNewScore(newScore);
            history.setChangedBy(teacher);
            gradeHistoryRepository.save(history);
            logger.debug("Đã lưu lịch sử: component={}, oldScore={}, newScore={}", 
                    componentName, oldScore, newScore);
        } catch (Exception e) {
            logger.error("Lỗi khi lưu GradeHistory (component: {}, gradeId: {}): {}. Lỗi này không ảnh hưởng việc lưu điểm.", 
                    componentName, savedGrade.getId(), e.getMessage(), e);
        }

        sendGradeNotification(teacher, student, moduleName, componentName, newScore,
                savedGrade.getClassModule().getId(), savedGrade.getId(), type);
    }

    private void sendGradeNotification(User teacher, User student, String moduleName,
                                       String componentName, BigDecimal score, Long classModuleId,
                                       Long gradeId, Notification.NotificationType type) {
        try {
            if (applicationContext != null) {
                logger.info("[Notification] ApplicationContext available, invoking transactional notification sender...");
                GradeServiceImpl self = applicationContext.getBean(GradeServiceImpl.class);
                logger.info("[Notification] Retrieved self bean: {}", self != null);
                if (self != null) {
                    self.sendGradeNotificationInNewTransaction(teacher, student, moduleName, componentName, score, classModuleId, gradeId, type);
                } else {
                    logger.warn("[Notification] Self bean is null even though applicationContext returned. Skipping notification.");
                }
            } else {
                logger.warn("[Notification] ApplicationContext not ready, skipping notification send.");
            }
        } catch (Exception e) {
            logger.error("Lỗi khi gửi thông báo (không ảnh hưởng việc lưu điểm): {}", e.getMessage(), e);
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendGradeNotificationInNewTransaction(User teacher, User student, String moduleName,
                                                      String componentName, BigDecimal score, Long classModuleId,
                                                      Long gradeId, Notification.NotificationType type) {
        try {
            logger.info("--- Bắt đầu gửi thông báo (transaction riêng) ---");
            logger.info("studentId={}, teacherId={}, type={}, componentName={}, score={}", 
                    student.getId(), teacher.getId(), type.name(), componentName, score);
            
            String title = (type == Notification.NotificationType.GRADE_NEW) ? "Bạn có điểm mới" : "Điểm của bạn đã được cập nhật";
            String content = String.format(
                    "Bạn có điểm [ %s ] môn [ %s ]: %.1f",
                    componentName, moduleName, (score != null ? score : 0)
            );

            logger.info("Đang gọi notificationService.createNotification...");
            notificationService.createNotification(
                    student.getId(),
                    teacher.getId(),
                    type,
                    "CLASS_MODULE",
                    classModuleId,
                    title,
                    content
            );
            logger.info("--- Gửi thông báo thành công ---");
        } catch (Exception e) {
            logger.error("========== LỖI KHI GỬI THÔNG BÁO (TRANSACTION RIÊNG) ==========");
            logger.error("Exception type: {}", e.getClass().getName());
            logger.error("Exception message: {}", e.getMessage());
            logger.error("Exception cause: {}", e.getCause() != null ? e.getCause().getMessage() : "null");
            logger.error("Full stack trace:", e);
            logger.error("================================================================");
        }
    }


    @Override
    public GradebookDTO getGradebook(Long classModuleId, Long teacherId, Pageable pageable) {
        validateTeacherPermission(classModuleId, teacherId);

        ClassModule classModule = classModuleRepository.findById(classModuleId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassModule not found"));

        Long classId = classModule.getClassEntity().getId();
        Page<ClassMember> studentMemberPage = classMemberRepository.findByClassIdAndRole(
                classId, ClassMember.Role.sinh_vien, pageable);

        List<Long> studentIdsOnPage = studentMemberPage.getContent().stream()
                .map(member -> member.getUser().getId())
                .collect(Collectors.toList());

        List<Grade> gradesForThisPage = (studentIdsOnPage.isEmpty())
                ? List.of()
                : gradeRepository.findByClassModuleIdAndStudentIdInAndStatus(
                classModuleId, studentIdsOnPage, Grade.Status.ACTIVE);

        Map<Long, Grade> gradeMap = gradesForThisPage.stream()
                .collect(Collectors.toMap(grade -> grade.getStudent().getId(), grade -> grade));

        GradebookDTO gradebook = new GradebookDTO();
        gradebook.setClassModuleId(classModuleId);
        gradebook.setClassName(classModule.getClassEntity().getClassName());
        gradebook.setModuleName(classModule.getModule().getName());
        gradebook.setComponents(List.of("Điểm lý thuyết", "Điểm thực hành"));

        List<GradebookDTO.StudentGradeRowDTO> studentRows = studentMemberPage.getContent().stream().map(member -> {
            GradebookDTO.StudentGradeRowDTO row = new GradebookDTO.StudentGradeRowDTO();
            User student = member.getUser();
            row.setStudentId(student.getId());
            row.setStudentName(student.getFirstName() + " " + student.getLastName());

            Grade grade = gradeMap.get(student.getId());
            if (grade != null) {
                row.setGradeId(grade.getId());
                row.setTheoryScore(grade.getTheoryScore());
                row.setPracticeScore(grade.getPracticeScore());
            }
            return row;
        }).collect(Collectors.toList());

        gradebook.setStudents(studentRows);
        gradebook.setCurrentPage(studentMemberPage.getNumber());
        gradebook.setTotalElements(studentMemberPage.getTotalElements());
        gradebook.setTotalPages(studentMemberPage.getTotalPages());
        return gradebook;
    }

    @Override
    public GradeDTO getMyGrades(Long classModuleId, Long studentId) {
        validateStudentMembership(classModuleId, studentId);

        Grade grade = gradeRepository.findByClassModuleIdAndStudentIdAndStatus(
                        classModuleId, studentId, Grade.Status.ACTIVE)
                .orElse(null);

        if (grade == null) {
            throw new ResourceNotFoundException("Bạn chưa có điểm cho môn học này.");
        }

        return new GradeDTO(grade);
    }

    @Override
    public List<GradeHistoryDTO> getGradeHistory(Long gradeId, User currentUser) {

        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Grade record not found (or deleted): " + gradeId));

        boolean isAuthorizedTeacher = false;
        boolean isStudentOwner = false;

        try {
            validateTeacherPermission(grade.getClassModule().getId(), currentUser.getId());
            isAuthorizedTeacher = true;
        } catch (Exception e) {
            isAuthorizedTeacher = false;
        }

        if (!isAuthorizedTeacher) {
            if (grade.getStudent().getId().equals(currentUser.getId())) {
                try {
                    validateStudentMembership(grade.getClassModule().getId(), currentUser.getId());
                    isStudentOwner = true;
                } catch (Exception e) {
                    isStudentOwner = false;
                }
            }
        }

        if (!isStudentOwner && !isAuthorizedTeacher) {
            throw new ForbiddenException("Access Denied: You do not have permission to view this grade history.");
        }

        List<GradeHistory> historyList = gradeHistoryRepository.findByGradeIdOrderByChangedAtDesc(gradeId);

        return historyList.stream()
                .map(GradeHistoryDTO::new)
                .toList();
    }

    @Override
    @Transactional
    public void deleteGrade(Long gradeId, User currentUser) {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Grade not found: " + gradeId));

        validateTeacherPermission(grade.getClassModule().getId(), currentUser.getId());

        gradeRepository.delete(grade);
    }

    private void validateStudentMembership(Long classModuleId, Long studentId) {
        ClassModule classModule = classModuleRepository.findById(classModuleId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassModule not found"));
        Long classId = classModule.getClassEntity().getId();
        Optional<ClassMember> memberOpt = classMemberRepository.findByClassIdAndUserId(classId, studentId);
        if (memberOpt.isEmpty()) {
            throw new ForbiddenException("Access Denied: Student not found in this class");
        }
        ClassMember member = memberOpt.get();
        ClassMember.Role vaiTroEnum = member.getRole();
        String vaiTroThucTe = (vaiTroEnum == null) ? "null" : vaiTroEnum.name();
        if (!"sinh_vien".equals(vaiTroThucTe)) {
            throw new ForbiddenException("Access Denied: User is in this class, but not as a student");
        }
    }

    @Override
    public void validateTeacherPermission(Long classModuleId, Long teacherId) {
        User user = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + teacherId));
        Set<Role> userRoles = user.getRoles();
        boolean isAdmin = userRoles.stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
        if (isAdmin) {
            return;
        }
        boolean isTeaching = classModuleTeacherRepository.existsByClassModuleIdAndUserId(classModuleId, teacherId);
        if (!isTeaching) {
            throw new ForbiddenException("Access Denied: User is not an authorized teacher for this module or an admin");
        }
    }
}