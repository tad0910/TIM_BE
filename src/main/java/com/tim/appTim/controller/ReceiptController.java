package com.tim.appTim.controller;

import com.tim.appTim.dto.ReceiptDTO;
import com.tim.appTim.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

        @Autowired
        private PdfService pdfService;

        @Autowired
        private com.tim.appTim.service.TuitionTransactionService tuitionTransactionService;

        @GetMapping("/{id}/download")
        @Transactional(readOnly = true)
        public ResponseEntity<byte[]> downloadReceipt(
                        @PathVariable Long id,
                        @RequestParam(required = false) String name,
                        @RequestParam(required = false) String reason) {

                com.tim.appTim.entity.TuitionTransaction transaction = tuitionTransactionService.getTransactionById(id);
                com.tim.appTim.entity.User student = transaction.getStudentTuition().getStudent();

                BigDecimal amount = transaction.getAmount();

                String moneyText = com.tim.appTim.util.NumberToWordsVietnamese.convert(amount);
                String moneyFormatted = com.tim.appTim.util.NumberToWordsVietnamese.formatMoney(amount);

                String finalPayerName = (name != null && !name.isEmpty()) ? name
                                : (student.getLastName() + " " + student.getFirstName());
                String finalReason = (reason != null && !reason.isEmpty()) ? reason : transaction.getDescription();

                ReceiptDTO receiptData = ReceiptDTO.builder()
                                .companyName("CodeGym Hà Nội")
                                .companyAddress(
                                                "Nhà số 23, Lô TT01, Đường Hàm Nghi, Khu đô thị Mon City, Mỹ Đình 2, Nam Từ Liêm, Hà Nội")
                                .receiptId("PT-" + transaction.getId())
                                .paymentDate(transaction.getTransactionDate().toLocalDate())
                                .payerName(finalPayerName)
                                .payerAddress("HN-C1025G1-JV101")
                                .paymentReason(finalReason)
                                .amountNumber(moneyFormatted)
                                .amountInWords(moneyText)
                                .attachment("................")
                                .build();

                byte[] pdfBytes = pdfService.generateReceiptPdf(receiptData);

                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=receipt_" + id + ".pdf")
                                .contentType(MediaType.APPLICATION_PDF)
                                .body(pdfBytes);
        }
}