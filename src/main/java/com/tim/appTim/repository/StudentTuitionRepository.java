package com.tim.appTim.repository;
import com.tim.appTim.entity.StudentTuition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentTuitionRepository extends JpaRepository<StudentTuition, Long> {
    boolean existsByStudentIdAndTuitionRoute_ProgramId(Long studentId, Long programId);

    java.util.List<StudentTuition> findByStudent_Id(Long studentId);

    @Query("select coalesce(sum(tr.totalListedFee),0), coalesce(sum(tr.admissionFee),0) " +
           "from StudentTuition st join st.tuitionRoute tr where st.student.id = :studentId")
    java.util.List<Object[]> sumListedAndAdmissionByStudent(@Param("studentId") Long studentId);
}