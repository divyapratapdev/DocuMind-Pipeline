package com.documind.pipeline.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class PdfExtractionService {

    /**
     * Extracts raw text from a PDF InputStream.
     * Handles edge cases: empty PDFs, corrupt PDFs, and image-only PDFs.
     */
    public String extractTextFromPdf(InputStream pdfStream) throws IOException {
        byte[] pdfBytes = pdfStream.readAllBytes();

        if (pdfBytes.length == 0) {
            log.warn("PDF stream was empty (0 bytes)");
            return "[EMPTY DOCUMENT]";
        }

        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdfBytes)) {
            if (document.getNumberOfPages() == 0) {
                log.warn("PDF has 0 pages");
                return "[EMPTY DOCUMENT - NO PAGES]";
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text == null || text.trim().isEmpty()) {
                log.warn("PDF contained no extractable text (likely scanned/image-only)");
                return "[NO EXTRACTABLE TEXT - SCANNED DOCUMENT]";
            }

            log.info("Successfully extracted {} characters from {} page PDF", text.length(), document.getNumberOfPages());
            return text;
        }
    }
}
