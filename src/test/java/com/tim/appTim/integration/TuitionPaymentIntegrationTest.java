package com.tim.appTim.integration;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.entity.TuitionReceipt;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.service.TuitionTransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/test-data.sql")
public class TuitionPaymentIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private TuitionTransactionService transactionService;

        private final String BASE_URL = "/api/tuition-payment";

        @Test
        @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
        void payTuition_WhenAdmin_ShouldReturn200() throws Exception {
                PaymentRequestDTO request = new PaymentRequestDTO();
                request.setStudentId(1L);
                request.setAmount(BigDecimal.valueOf(1000000));
                request.setPaymentMethod("CASH");
                request.setNote("Payment note");

                TuitionReceipt receipt = new TuitionReceipt();
                receipt.setId(1L);
                receipt.setReceiptCode("REC-001");
                receipt.setAmount(BigDecimal.valueOf(1000000));

                doReturn(receipt).when(transactionService).processPayment(any(PaymentRequestDTO.class),
                                any(User.class));

                mockMvc.perform(post(BASE_URL + "/pay")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Thanh toán thành công!"))
                                .andExpect(jsonPath("$.receiptId").value(1L))
                                .andExpect(jsonPath("$.receiptCode").value("REC-001"));
        }

        @Test
        @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
        void payTuition_WhenTeacher_ShouldReturn200() throws Exception {
                PaymentRequestDTO request = new PaymentRequestDTO();
                request.setStudentId(1L);
                request.setAmount(BigDecimal.valueOf(500000));
                request.setPaymentMethod("BANK_TRANSFER");

                TuitionReceipt receipt = new TuitionReceipt();
                receipt.setId(2L);
                receipt.setReceiptCode("REC-002");
                receipt.setAmount(BigDecimal.valueOf(500000));

                doReturn(receipt).when(transactionService).processPayment(any(PaymentRequestDTO.class),
                                any(User.class));

                mockMvc.perform(post(BASE_URL + "/pay")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.receiptId").value(2L));
        }

        @Test
        @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
        void payTuition_WhenUser_ShouldReturn403() throws Exception {
                PaymentRequestDTO request = new PaymentRequestDTO();
                request.setStudentId(1L);
                request.setAmount(BigDecimal.valueOf(1000000));
                request.setPaymentMethod("CASH");

                mockMvc.perform(post(BASE_URL + "/pay")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
        void payTuition_WhenInvalidInput_ShouldReturn400() throws Exception {
                // Missing studentId
                PaymentRequestDTO request = new PaymentRequestDTO();
                request.setAmount(BigDecimal.valueOf(1000000));
                request.setPaymentMethod("CASH");

                mockMvc.perform(post(BASE_URL + "/pay")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                // Missing amount
                PaymentRequestDTO request2 = new PaymentRequestDTO();
                request2.setStudentId(1L);
                request2.setPaymentMethod("CASH");

                mockMvc.perform(post(BASE_URL + "/pay")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request2)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
        void payTuition_WhenStudentSchedulesNotFound_ShouldReturn404() throws Exception {
                PaymentRequestDTO request = new PaymentRequestDTO();
                request.setStudentId(999L);
                request.setAmount(BigDecimal.valueOf(1000000));
                request.setPaymentMethod("CASH");

                doThrow(new ResourceNotFoundException("Không tìm thấy lộ trình học phí cho học viên này"))
                                .when(transactionService).processPayment(any(PaymentRequestDTO.class), any(User.class));

                mockMvc.perform(post(BASE_URL + "/pay")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message")
                                                .value("Không tìm thấy lộ trình học phí cho học viên này"));
        }

        @Test
        @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
        void payTuition_WhenAmountZero_ShouldReturn400() throws Exception {
                PaymentRequestDTO request = new PaymentRequestDTO();
                request.setStudentId(1L);
                request.setAmount(BigDecimal.ZERO);
                request.setPaymentMethod("CASH");

                doThrow(new BadRequestException("Số tiền đóng phải lớn hơn 0"))
                                .when(transactionService).processPayment(any(PaymentRequestDTO.class), any(User.class));

                mockMvc.perform(post(BASE_URL + "/pay")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Số tiền đóng phải lớn hơn 0"));
        }
}

