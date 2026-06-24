package com.tim.appTim.service;

import com.lowagie.text.pdf.BaseFont;
import com.tim.appTim.dto.response.ReceiptDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;

@Service
public class PdfService {

    @Autowired
    private SpringTemplateEngine templateEngine;

    public byte[] generateReceiptPdf(ReceiptDTO data) {
        try {
            Context context = new Context();
            context.setVariable("data", data);

            String htmlContent = templateEngine.process("receipt_template", context);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();

            // Load SVN fonts with BaseFont.EMBEDDED to ensure all Vietnamese characters are
            // included
            String fontBasePath = "/fonts/";

            // Use SVN-Times New Roman fonts for better Vietnamese support
            String[] fontFiles = {
                    "SVN-Times New Roman.ttf",
                    "SVN-Times New Roman Bold.ttf",
                    "SVN-Times New Roman Italic.ttf",
                    "SVN-Times New Roman Bold Italic.ttf"
            };

            for (String fontFile : fontFiles) {
                try {
                    String fontPath = new ClassPathResource(fontBasePath + fontFile).getURL().toString();
                    renderer.getFontResolver().addFont(
                            fontPath,
                            BaseFont.IDENTITY_H,
                            BaseFont.EMBEDDED);
                } catch (Exception e) {
                    System.err.println("Could not load font: " + fontFile + " - " + e.getMessage());
                }
            }

            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo PDF: " + e.getMessage(), e);
        }
    }
}
