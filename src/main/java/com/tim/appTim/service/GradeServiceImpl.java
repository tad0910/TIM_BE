package com.tim.appTim.service; // Đảm bảo đúng package

// THÊM IMPORT CÒN THIẾU
import com.tim.appTim.entity.Grade;

import com.tim.appTim.dto.GradebookDTO;
import com.tim.appTim.dto.StudentGradeDTO;
import com.tim.appTim.entity.ClassMember;
import com.tim.appTim.entity.ClassModule;
import com.tim.appTim.entity.User;
import com.tim.appTim.entity.Role;
import com.tim.appTim.exception.ForbiddenException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GradeServiceImpl implements GradeService { // Sẽ implement GradeService mới

    private final GradeRepository gradeRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassModuleRepository classModuleRepository;
    private final ClassModuleTeacherRepository classModuleTeacherRepository;
    private final UserRepository userRepository;

    public GradeServiceImpl(GradeRepository gradeRepository,
                            ClassMemberRepository classMemberRepository,
                            ClassModuleRepository classModuleRepository,
                            ClassModuleTeacherRepository classModuleTeacherRepository, UserRepository userRepository) {
        this.gradeRepository = gradeRepository;
        this.classMemberRepository = classMemberRepository;
        this.classModuleRepository = classModuleRepository;
        this.classModuleTeacherRepository = classModuleTeacherRepository;
        this.userRepository = userRepository;
    }

    // Kịch bản 1: Sửa (Long classModuleId, Integer studentId) -> (Long, Long)
    @Override
    public List<StudentGradeDTO> getMyGrades(Long classModuleId, Long studentId) {
        validateStudentMembership(classModuleId, studentId); // studentId giờ là Long
        return gradeRepository.findGradesForStudent(classModuleId, studentId); // studentId giờ là Long
    }

    // Kịch bản 2: Đã đúng (Long, Long)
    @Override
    public List<StudentGradeDTO> getStudentGrades(Long classModuleId, Long studentId) {
        // (Kiểm tra quyền của giảng viên nên được gọi ở Controller)
        return gradeRepository.findGradesForTeacher(classModuleId, studentId);
    }

    // Kịch bản 3: Đã đúng (Long, Long)
    @Override
    public GradebookDTO getGradebook(Long classModuleId, Long teacherId) {
        validateTeacherPermission(classModuleId, teacherId);

        ClassModule classModule = classModuleRepository.findById(classModuleId)
                // SỬA LỖI CÚ PHÁP: Xóa
                .orElseThrow(() -> new ResourceNotFoundException("ClassModule not found"));

        List<String> components = gradeRepository.findDistinctComponentNamesByClassModuleId(classModuleId);

        // Giả sử ClassModule có hàm getClassEntity() trả về đối tượng Class
        Long classId = classModule.getClassEntity().getId();
        List<ClassMember> members = classMemberRepository.findByClassId(classId);

        List<User> students = members.stream()
                // LỖI 'getVaiTro': Đảm bảo entity ClassMember có hàm getVaiTro()
                .filter(member -> "sinh_vien".equals(member.getRole()))
                // LỖI 'getUser': Đảm bảo entity ClassMember có hàm getUser()
                .map(ClassMember::getUser)
                .collect(Collectors.toList());

        // LỖI 'Grade': Đã thêm import
        List<Grade> allGrades = gradeRepository.findByClassModuleId(classModuleId);

        Map<Long, Map<String, BigDecimal>> gradesByStudent = allGrades.stream()
                .collect(Collectors.groupingBy(
                        // LỖI 'getStudent': Đảm bảo entity Grade có hàm getStudent()
                        grade -> grade.getStudent().getId(),
                        // LỖI 'getComponentName', 'getScore': Đảm bảo entity Grade có các hàm này
                        Collectors.toMap(Grade::getComponentName, Grade::getScore)
                ));

        GradebookDTO gradebook = new GradebookDTO();
        gradebook.setClassModuleId(classModuleId);
        // LỖI 'getName': Đảm bảo entity Class có hàm getName()
        gradebook.setClassName(classModule.getClassEntity().getClassName());
        // LỖI 'getModule', 'getName': Đảm bảo entity ClassModule có getModule() và Module có getName()
        gradebook.setModuleName(classModule.getModule().getName());
        gradebook.setComponents(components);

        List<GradebookDTO.StudentRow> studentRows = students.stream().map(student -> {
            GradebookDTO.StudentRow row = new GradebookDTO.StudentRow();
            // LỖI DTO: Đảm bảo setStudentId(Long) tồn tại
            row.setStudentId(student.getId()); // ID kiểu Long
            // LỖI 'getFirstname', 'getLastname': Đảm bảo entity User có các hàm này
            row.setStudentName(student.getFirstName() + " " + student.getLastName());

            Map<String, BigDecimal> studentScores = gradesByStudent.getOrDefault(student.getId(), Map.of());
            row.setGrades(studentScores);
            return row;
        }).collect(Collectors.toList());

        gradebook.setStudents(studentRows);
        return gradebook;
    }

    // --- Các hàm private kiểm tra quyền (ĐÃ SỬA) ---

    // Sửa (Long classModuleId, Integer studentId) -> (Long, Long)
    private void validateStudentMembership(Long classModuleId, Long studentId) {

        ClassModule classModule = classModuleRepository.findById(classModuleId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassModule not found"));

        Long classId = classModule.getClassEntity().getId();

        Optional<ClassMember> memberOpt = classMemberRepository.findByClassIdAndUserId(classId, studentId);

        if (memberOpt.isEmpty()) {
            throw new ForbiddenException("Access Denied: Student not found in this class");
        }

        // Lấy vai trò của thành viên
        ClassMember member = memberOpt.get();

        // Lấy đối tượng Enum vai trò (ví dụ: getVaiTro() hoặc getRole())
        ClassMember.Role vaiTroEnum = member.getRole();

        // Chuyển đổi Enum sang String để so sánh
        String vaiTroThucTe = (vaiTroEnum == null) ? "null" : vaiTroEnum.name();

        // So sánh vai trò
        if (!"sinh_vien".equals(vaiTroThucTe)) {
            throw new ForbiddenException("Access Denied: User is in this class, but not as a student");
        }
    }

    @Override
    public void validateTeacherPermission(Long classModuleId, Long teacherId) {

        // Bước 1: Kiểm tra xem user có phải là Admin không
        User user = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + teacherId));

        // SỬA: Lấy Set<Role> từ user
        Set<Role> userRoles = user.getRoles();

        // SỬA: Lặp qua Set để tìm quyền 'admin'
        // (Giả sử Role entity có hàm getName() trả về String)
        boolean isAdmin = userRoles.stream()
                .anyMatch(role -> "admin".equals(role.getName()));

        if (isAdmin) {
            return; // Admin -> Cho phép
        }

        // Bước 2: Nếu không phải Admin, kiểm tra xem có phải giáo viên được gán không
        boolean isTeaching = classModuleTeacherRepository.existsByClassModuleIdAndUserId(classModuleId, teacherId);

        if (!isTeaching) {
            // Nếu không phải Admin VÀ cũng không phải GV được gán -> Chặn
            throw new ForbiddenException("Access Denied: User is not an authorized teacher for this module or an admin");
        }
    }
}