package com.tim.appTim.service;

import com.tim.appTim.dto.BatchGradeUpdateDTO;
import com.tim.appTim.dto.StudentScoreEntryDTO;
import com.tim.appTim.entity.*;
import com.tim.appTim.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GradeServiceImplTest {

    @Mock
    private GradeRepository gradeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private GamificationService gamificationService;
    @Mock
    private ClassMemberRepository classMemberRepository;
    @Mock
    private ClassRepository classRepository;
    @Mock
    private ClassModuleRepository classModuleRepository;
    @Mock
    private ClassModuleTeacherRepository classModuleTeacherRepository;
    @Mock
    private GradeHistoryRepository gradeHistoryRepository;
    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private org.springframework.context.ApplicationContext applicationContext;

    @InjectMocks
    private GradeServiceImpl gradeService;

    private User teacher;
    private User student;
    private com.tim.appTim.entity.Class classEntity;
    private ClassModule classModule;
    private com.tim.appTim.entity.Module module;

    @BeforeEach
    void setUp() {
        teacher = new User();
        teacher.setId(1L);
        teacher.setUsername("teacher");
        teacher.setRoles(new HashSet<>());

        student = new User();
        student.setId(2L);
        student.setUsername("student");
        student.setRoles(new HashSet<>());

        classEntity = new com.tim.appTim.entity.Class();
        classEntity.setId(1L);
        classEntity.setClassName("Class 1");

        module = new com.tim.appTim.entity.Module();
        module.setId(1);
        module.setName("MODULE_1");

        classModule = new ClassModule();
        classModule.setId(1L);
        classModule.setClassEntity(classEntity);
        classModule.setModule(module);

        gradeService.setApplicationContext(applicationContext);
    }

    @Test
    void testBatchCreateOrUpdateGrades_Success() {
        BatchGradeUpdateDTO dto = new BatchGradeUpdateDTO();
        dto.setClassModuleId(1L);
        dto.setEntryDate(LocalDate.now());

        StudentScoreEntryDTO scoreEntry = new StudentScoreEntryDTO();
        scoreEntry.setStudentId(2L);
        Map<String, BigDecimal> components = new HashMap<>();
        components.put("Điểm lý thuyết", new BigDecimal("9.0"));
        scoreEntry.setComponents(components);
        dto.setScores(List.of(scoreEntry));

        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(classModuleTeacherRepository.existsByClassModuleIdAndUserId(1L, 1L)).thenReturn(true);
        when(applicationContext.getBean(GradeServiceImpl.class)).thenReturn(gradeService);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        when(classModuleRepository.findById(1L)).thenReturn(Optional.of(classModule));
        when(userRepository.findAllById(anyList())).thenReturn(List.of(student));
        when(gradeRepository.findByClassModuleIdAndStudentIdIn(eq(1L), anyList())).thenReturn(new ArrayList<>());
        when(gradeRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        gradeService.batchCreateOrUpdateGrades(dto, teacher);

        verify(gradeRepository, times(1)).saveAll(anyList());
        verify(notificationService, atLeastOnce()).createNotification(
                eq(2L), eq(1L), any(), anyString(), eq(1L), anyString(), anyString());
    }

    @Test
    void testBatchCreateOrUpdateGrades_UpdateExisting() {
        BatchGradeUpdateDTO dto = new BatchGradeUpdateDTO();
        dto.setClassModuleId(1L);
        dto.setEntryDate(LocalDate.now());

        StudentScoreEntryDTO scoreEntry = new StudentScoreEntryDTO();
        scoreEntry.setStudentId(2L);
        Map<String, BigDecimal> components = new HashMap<>();
        components.put("Điểm lý thuyết", new BigDecimal("10.0")); // Changed from 5.0
        scoreEntry.setComponents(components);
        dto.setScores(List.of(scoreEntry));

        Grade existingGrade = new Grade();
        existingGrade.setId(10L);
        existingGrade.setStudent(student);
        existingGrade.setClassModule(classModule);
        existingGrade.setTheoryScore(new BigDecimal("5.0"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(classModuleTeacherRepository.existsByClassModuleIdAndUserId(1L, 1L)).thenReturn(true);
        when(applicationContext.getBean(GradeServiceImpl.class)).thenReturn(gradeService);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        when(classModuleRepository.findById(1L)).thenReturn(Optional.of(classModule));
        when(userRepository.findAllById(anyList())).thenReturn(List.of(student));
        when(gradeRepository.findByClassModuleIdAndStudentIdIn(eq(1L), anyList())).thenReturn(List.of(existingGrade));
        when(gradeRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        gradeService.batchCreateOrUpdateGrades(dto, teacher);

        assertEquals(new BigDecimal("10.0"), existingGrade.getTheoryScore());
        verify(gradeRepository, times(1)).saveAll(anyList());
        verify(gradeHistoryRepository, atLeastOnce()).save(any(GradeHistory.class));
    }
}
