package com.documind.pipeline.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class PdfExtractionService {

    public String extractTextFromPdf(InputStream pdfStream) throws Exception {
        byte[] pdfBytes = pdfStream.readAllBytes();
        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
