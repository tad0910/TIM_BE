package com.tim.appTim.controller;

import com.tim.appTim.dto.PaymentRequestDTO;
import com.tim.appTim.entity.TuitionReceipt;
import com.tim.appTim.service.TuitionTransactionService;
import com.tim.appTim.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/tuition-payment")
public class TuitionPaymentController {

    @Autowired
    private TuitionTransactionService transactionService;

    @PostMapping("/pay")
    @PreAuthorize("hasAnyAuthority('tuition:update', 'ROLE_ADMIN', 'ROLE_GIAO_VIEN')")
    public ResponseEntity<?> payTuition(@RequestBody @Valid PaymentRequestDTO request, Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        TuitionReceipt receipt = transactionService.processPayment(request, userDetails.getUser());

        return ResponseEntity.ok(Map.of(
                "message", "Thanh toán thành công!",
                "receiptId", receipt.getId(),
                "receiptCode", receipt.getReceiptCode(),
                "amount", receipt.getAmount()
        ));
    }
}