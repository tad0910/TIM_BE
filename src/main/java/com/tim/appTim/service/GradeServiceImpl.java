package com.tim.appTim.service;


import com.tim.appTim.dto.*;
import com.tim.appTim.entity.*;

import com.tim.appTim.exception.ForbiddenException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GradeServiceImpl implements GradeService {

    private final GradeRepository gradeRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassModuleRepository classModuleRepository;
    private final ClassModuleTeacherRepository classModuleTeacherRepository;
    private final UserRepository userRepository;
    private final GradeHistoryRepository gradeHistoryRepository;
    private final NotificationService notificationService;

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
    public List<StudentGradeDTO> getMyGrades(Long classModuleId, Long studentId) {
        validateStudentMembership(classModuleId, studentId);
        return gradeRepository.findGradesForStudent(classModuleId, studentId);
    }

    @Override
    public List<StudentGradeDTO> getStudentGrades(Long classModuleId, Long studentId) {
        return gradeRepository.findGradesForTeacher(classModuleId, studentId);
    }

    @Override
    public GradebookDTO getGradebook(Long classModuleId, Long teacherId, Pageable pageable) {
        validateTeacherPermission(classModuleId, teacherId);

        ClassModule classModule = classModuleRepository.findById(classModuleId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassModule not found"));

        List<String> components = gradeRepository.findDistinctComponentNamesByClassModuleId(classModuleId);

        Long classId = classModule.getClassEntity().getId();
        Page<ClassMember> studentMemberPage = classMemberRepository.findByClassIdAndRole(
                classId, ClassMember.Role.sinh_vien, pageable);

        List<User> studentsOnThisPage = studentMemberPage.getContent().stream()
                .map(ClassMember::getUser)
                .collect(Collectors.toList());

        List<Long> studentIdsOnPage = studentsOnThisPage.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        List<Grade> gradesForThisPage = (studentIdsOnPage.isEmpty())
                ? List.of()
                : gradeRepository.findByClassModuleIdAndStudentIdIn(classModuleId, studentIdsOnPage);

        Map<Long, Map<String, BigDecimal>> gradesByStudent = gradesForThisPage.stream()
                .collect(Collectors.groupingBy(
                        grade -> grade.getStudent().getId(),
                        Collectors.toMap(Grade::getComponentName, Grade::getScore)
                ));

        GradebookDTO gradebook = new GradebookDTO();
        gradebook.setClassModuleId(classModuleId);
        gradebook.setClassName(classModule.getClassEntity().getClassName());
        gradebook.setModuleName(classModule.getModule().getName());
        gradebook.setComponents(components);

        List<GradebookDTO.StudentRow> studentRows = studentsOnThisPage.stream().map(student -> {
            GradebookDTO.StudentRow row = new GradebookDTO.StudentRow();
            row.setStudentId(student.getId());
            row.setStudentName(student.getFirstName() + " " + student.getLastName());

            Map<String, BigDecimal> studentScores = gradesByStudent.getOrDefault(student.getId(), Map.of());
            row.setGrades(studentScores);
            return row;
        }).collect(Collectors.toList());

        gradebook.setStudents(studentRows);
        gradebook.setCurrentPage(studentMemberPage.getNumber());
        gradebook.setTotalElements(studentMemberPage.getTotalElements());
        gradebook.setTotalPages(studentMemberPage.getTotalPages());
        return gradebook;
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
                .anyMatch(role -> "admin".equals(role.getName()));

        if (isAdmin) {
            return;
        }

        boolean isTeaching = classModuleTeacherRepository.existsByClassModuleIdAndUserId(classModuleId, teacherId);

        if (!isTeaching) {
            throw new ForbiddenException("Access Denied: User is not an authorized teacher for this module or an admin");
        }
    }

    @Override
    @Transactional
    public StudentGradeDTO updateGrade(Long gradeId, GradeUpdateDTO dto, User teacher) {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Grade record not found with id: " + gradeId));

        Long classModuleId = grade.getClassModule().getId();
        validateTeacherPermission(classModuleId, teacher.getId());
        BigDecimal oldScore = grade.getScore();

        GradeHistory history = new GradeHistory();
        history.setGrade(grade);
        history.setOldScore(oldScore);
        history.setNewScore(dto.getNewScore());
        history.setChangeReason(dto.getChangeReason());
        history.setChangedBy(teacher);

        gradeHistoryRepository.save(history);

        grade.setScore(dto.getNewScore());
        grade.setEnteredBy(teacher);
        Grade updatedGrade = gradeRepository.save(grade);

        try {
            User student = updatedGrade.getStudent();
            String moduleName = updatedGrade.getClassModule().getModule().getName();
            String title = "Điểm của bạn đã được cập nhật";
            String content = String.format(
                    "Điểm [ %s ] môn [ %s ] của bạn đã được cập nhật thành: %.1f",
                    updatedGrade.getComponentName(), moduleName, updatedGrade.getScore()
            );

            notificationService.createNotification(
                    student.getId(),
                    teacher.getId(),
                    Notification.NotificationType.GRADE_UPDATED,
                    "CLASS_MODULE",
                    classModuleId,
                    title,
                    content
            );
        } catch (Exception e) {
        }

        return new StudentGradeDTO(
                updatedGrade.getId(),
                updatedGrade.getComponentName(),
                updatedGrade.getScore(),
                updatedGrade.getMaxScore(),
                updatedGrade.getWeightPercent(),
                updatedGrade.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public StudentGradeDTO createGrade(GradeCreateDTO dto, User teacher) {

        validateTeacherPermission(dto.getClassModuleId(), teacher.getId());

        User student = userRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + dto.getStudentId()));

        ClassModule classModule = classModuleRepository.findById(dto.getClassModuleId())
                .orElseThrow(() -> new ResourceNotFoundException("ClassModule not found with id: " + dto.getClassModuleId()));

        Grade newGrade = new Grade();
        newGrade.setStudent(student);
        newGrade.setClassModule(classModule);
        newGrade.setComponentName(dto.getComponentName());
        newGrade.setScore(dto.getScore());
        newGrade.setMaxScore(dto.getMaxScore());
        newGrade.setWeightPercent(dto.getWeightPercent());
        newGrade.setEnteredBy(teacher);

        Grade savedGrade = gradeRepository.save(newGrade);

        try {
            Long classModuleId = savedGrade.getClassModule().getId();
            String moduleName = savedGrade.getClassModule().getModule().getName();
            String title = "Bạn có điểm mới";
            String content = String.format(
                    "Bạn có điểm mới [ %s ] môn [ %s ]: %.1f",
                    savedGrade.getComponentName(), moduleName, savedGrade.getScore()
            );

            notificationService.createNotification(
                    student.getId(),
                    teacher.getId(),
                    Notification.NotificationType.GRADE_NEW,
                    "CLASS_MODULE",
                    classModuleId,
                    title,
                    content
            );
        } catch (Exception e) {
        }

        return new StudentGradeDTO(savedGrade.getId(), savedGrade.getComponentName(), savedGrade.getScore(), savedGrade.getMaxScore(), savedGrade.getWeightPercent(), savedGrade.getUpdatedAt());
    }


    @Override
    public List<GradeHistoryDTO> getGradeHistory(Long gradeId, User currentUser) {

        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Grade record not found with id: " + gradeId));

        boolean isAuthorizedTeacher = false;
        boolean isStudentOwner = false;

        try {
            Long classModuleId = grade.getClassModule().getId();
            validateTeacherPermission(classModuleId, currentUser.getId());
            isAuthorizedTeacher = true;
        } catch (ForbiddenException | ResourceNotFoundException e) {
            isAuthorizedTeacher = false;
        }

        if (!isAuthorizedTeacher) {
            boolean isSamePerson = grade.getStudent().getId().equals(currentUser.getId());

            if (isSamePerson) {
                try {
                    Long classModuleId = grade.getClassModule().getId();
                    validateStudentMembership(classModuleId, currentUser.getId());
                    isStudentOwner = true;
                } catch (ForbiddenException | ResourceNotFoundException e) {
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
                .collect(Collectors.toList());
    }
}