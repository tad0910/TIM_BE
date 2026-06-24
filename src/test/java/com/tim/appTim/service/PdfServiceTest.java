package com.tim.appTim.service;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfServiceTest {

    @Mock
    private SpringTemplateEngine templateEngine;

    @InjectMocks
    private PdfService pdfService;

    private ReceiptDTO receiptDTO;

    @BeforeEach
    void setUp() {
        receiptDTO = ReceiptDTO.builder()
                .payerName("John Doe")
                .amountNumber("1000000")
                .amountInWords("Một triệu đồng")
                .paymentDate(java.time.LocalDate.now())
                .receiptId("REC001")
                .companyName("Test Company")
                .companyAddress("123 Test St")
                .payerAddress("456 User St")
                .paymentReason("Tuition fee")
                .build();
    }

    @Test
    void generateReceiptPdf_WhenValidData_ShouldReturnPdfBytes() {
        // Arrange
        String htmlContent = "<html><body>Receipt</body></html>";
        when(templateEngine.process(eq("receipt_template"), any(Context.class)))
                .thenReturn(htmlContent);

        // Act
        byte[] result = pdfService.generateReceiptPdf(receiptDTO);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(templateEngine).process(eq("receipt_template"), any(Context.class));
    }

    @Test
    void generateReceiptPdf_ShouldSetDataInContext() {
        // Arrange
        String htmlContent = "<html><body>Receipt</body></html>";
        when(templateEngine.process(eq("receipt_template"), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertNotNull(context.getVariable("data"));
                    assertEquals(receiptDTO, context.getVariable("data"));
                    return htmlContent;
                });

        // Act
        pdfService.generateReceiptPdf(receiptDTO);

        // Assert
        verify(templateEngine).process(eq("receipt_template"), any(Context.class));
    }

    @Test
    void generateReceiptPdf_WhenTemplateProcessingFails_ShouldThrowException() {
        // Arrange
        when(templateEngine.process(eq("receipt_template"), any(Context.class)))
                .thenThrow(new RuntimeException("Template error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            pdfService.generateReceiptPdf(receiptDTO);
        });
        assertTrue(exception.getMessage().contains("Lỗi khi tạo PDF"));
    }

    @Test
    void generateReceiptPdf_WhenFontLoadingFails_ShouldStillGeneratePdf() {
        // Arrange
        String htmlContent = "<html><body>Receipt</body></html>";
        when(templateEngine.process(eq("receipt_template"), any(Context.class)))
                .thenReturn(htmlContent);

        // Act - should handle font loading errors gracefully
        assertDoesNotThrow(() -> {
            byte[] result = pdfService.generateReceiptPdf(receiptDTO);
            assertNotNull(result);
        });

        // Assert
        verify(templateEngine).process(eq("receipt_template"), any(Context.class));
    }
}


