package com.tim.appTim.service; // Đảm bảo đúng package

// THÊM IMPORT CÒN THIẾU
import com.tim.appTim.dto.GradeHistoryDTO;
import com.tim.appTim.entity.Grade;

import com.tim.appTim.dto.GradeUpdateDTO;
import com.tim.appTim.dto.GradebookDTO;
import com.tim.appTim.dto.StudentGradeDTO;
import com.tim.appTim.entity.GradeHistory;
import com.tim.appTim.entity.ClassMember;
import com.tim.appTim.entity.ClassModule;
import com.tim.appTim.entity.User;
import com.tim.appTim.entity.Role;
import com.tim.appTim.exception.ForbiddenException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.*;
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

    public GradeServiceImpl(GradeRepository gradeRepository,
                            ClassMemberRepository classMemberRepository,
                            ClassModuleRepository classModuleRepository,
                            ClassModuleTeacherRepository classModuleTeacherRepository, UserRepository userRepository, GradeHistoryRepository gradeHistoryRepository) {
        this.gradeRepository = gradeRepository;
        this.classMemberRepository = classMemberRepository;
        this.classModuleRepository = classModuleRepository;
        this.classModuleTeacherRepository = classModuleTeacherRepository;
        this.userRepository = userRepository;
        this.gradeHistoryRepository = gradeHistoryRepository;
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
    public GradebookDTO getGradebook(Long classModuleId, Long teacherId) {
        validateTeacherPermission(classModuleId, teacherId);

        ClassModule classModule = classModuleRepository.findById(classModuleId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassModule not found"));

        List<String> components = gradeRepository.findDistinctComponentNamesByClassModuleId(classModuleId);

        Long classId = classModule.getClassEntity().getId();
        List<ClassMember> members = classMemberRepository.findByClassId(classId);

        List<User> students = members.stream()
                .filter(member -> "sinh_vien".equals(member.getRole()))
                .map(ClassMember::getUser)
                .collect(Collectors.toList());

        List<Grade> allGrades = gradeRepository.findByClassModuleId(classModuleId);

        Map<Long, Map<String, BigDecimal>> gradesByStudent = allGrades.stream()
                .collect(Collectors.groupingBy(
                        grade -> grade.getStudent().getId(),
                        Collectors.toMap(Grade::getComponentName, Grade::getScore)
                ));

        GradebookDTO gradebook = new GradebookDTO();
        gradebook.setClassModuleId(classModuleId);
        gradebook.setClassName(classModule.getClassEntity().getClassName());
        gradebook.setModuleName(classModule.getModule().getName());
        gradebook.setComponents(components);

        List<GradebookDTO.StudentRow> studentRows = students.stream().map(student -> {
            GradebookDTO.StudentRow row = new GradebookDTO.StudentRow();
            row.setStudentId(student.getId());
            row.setStudentName(student.getFirstName() + " " + student.getLastName());

            Map<String, BigDecimal> studentScores = gradesByStudent.getOrDefault(student.getId(), Map.of());
            row.setGrades(studentScores);
            return row;
        }).collect(Collectors.toList());

        gradebook.setStudents(studentRows);
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