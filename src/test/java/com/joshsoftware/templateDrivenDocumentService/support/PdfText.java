package com.joshsoftware.templateDrivenDocumentService.support;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

// Reads a rendered PDF back with PDFBox, so tests can check its pages and text
public record PdfText(List<String> pages) {

    public static PdfText of(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            // Reading order on the page, so the footer's generated counters read "Page 1 of N"
            stripper.setSortByPosition(true);
            List<String> pages = new ArrayList<>();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                pages.add(stripper.getText(document));
            }
            return new PdfText(List.copyOf(pages));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public int pageCount() {
        return pages.size();
    }

    public String text() {
        return String.join("\n", pages);
    }
}
