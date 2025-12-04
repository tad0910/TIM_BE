package com.tim.appTim.integration;

import com.tim.appTim.entity.TuitionReceipt;
import com.tim.appTim.entity.TuitionTransaction;
import com.tim.appTim.repository.TuitionReceiptRepository;
import com.tim.appTim.repository.TuitionTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
@Transactional
public class ReceiptIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TuitionReceiptRepository receiptRepository;

    @Autowired
    private TuitionTransactionRepository transactionRepository;

    @Autowired
    private com.tim.appTim.repository.TuitionRouteRepository tuitionRouteRepository;

    @Autowired
    private com.tim.appTim.repository.StudentTuitionRepository studentTuitionRepository;

    @Test
    @WithMockUser(username = "admin_user")
    void testDownloadReceipt_Success_ShouldReturnPdf() throws Exception {
        // Setup data: Create a transaction and receipt
        // 1. Create Route
        com.tim.appTim.entity.TuitionRoute route = new com.tim.appTim.entity.TuitionRoute();
        route.setName("Standard Route");
        route.setTotalListedFee(new BigDecimal("10000000"));
        route.setType(com.tim.appTim.entity.TuitionRoute.TuitionRouteType.FULL_TIME);

        com.tim.appTim.entity.Programs program = new com.tim.appTim.entity.Programs();
        program.setId(100); // From test-data.sql
        route.setProgram(program);
        route = tuitionRouteRepository.save(route);

        // 2. Create StudentTuition
        com.tim.appTim.entity.StudentTuition st = new com.tim.appTim.entity.StudentTuition();
        com.tim.appTim.entity.User student = new com.tim.appTim.entity.User();
        student.setId(2L); // another_user
        st.setStudent(student);
        st.setTuitionRoute(route);
        st = studentTuitionRepository.save(st);

        // 3. Create Transaction
        TuitionTransaction transaction = new TuitionTransaction();
        transaction.setStudentTuition(st);
        transaction.setAmount(new BigDecimal("1000000"));
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setDescription("Test Tuition Fee");
        transaction.setType(TuitionTransaction.TransactionType.PAYMENT); // Required field
        transaction = transactionRepository.save(transaction);

        TuitionReceipt receipt = new TuitionReceipt();
        receipt.setTransaction(transaction);
        receipt.setReceiptCode("REC-001");
        receipt = receiptRepository.save(receipt);

        mockMvc.perform(get("/api/receipts/{id}/download", receipt.getId()))
                .andExpect(status().isOk())
                .andExpect(
                        header().string("Content-Disposition", "inline; filename=receipt_" + receipt.getId() + ".pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    @WithMockUser(username = "admin_user")
    void testDownloadReceipt_NotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/receipts/{id}/download", 9999L))
                .andExpect(status().isNotFound());
    }
}
