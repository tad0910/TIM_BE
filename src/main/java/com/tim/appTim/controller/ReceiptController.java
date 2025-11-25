package com.tim.appTim.controller;

import com.tim.appTim.dto.ReceiptDTO;
import com.tim.appTim.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/receipts")
public class    ReceiptController {

    @Autowired
    private PdfService pdfService;

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadReceipt(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Người nộp tiền") String name,
            @RequestParam(required = false, defaultValue = "Thanh toán") String reason) {

        BigDecimal amount = new BigDecimal("9500000");

        String moneyText = com.tim.appTim.utils.NumberToWordsVietnamese.convert(amount);
        String moneyFormatted = com.tim.appTim.utils.NumberToWordsVietnamese.formatMoney(amount);

        ReceiptDTO receiptData = ReceiptDTO.builder()
                .companyName("CodeGym Hà Nội")
                .companyAddress(
                        "Nhà số 23, Lô TT01, Đường Hàm Nghi, Khu đô thị Mon City, Mỹ Đình 2, Nam Từ Liêm, Hà Nội")
                .receiptId("PT-" + id)
                .paymentDate(java.time.LocalDate.of(2025, 11, 21))
                .payerName(name)
                .payerAddress("HN-C1025G1-JV101")
                .paymentReason(reason)
                .amountNumber(moneyFormatted)
                .amountInWords(moneyText)
                // -----------------
                .attachment("................")
                .build();

        byte[] pdfBytes = pdfService.generateReceiptPdf(receiptData);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=receipt_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}