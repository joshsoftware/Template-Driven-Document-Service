package com.joshsoftware.templateDrivenDocumentService.engine.render;

import com.joshsoftware.templateDrivenDocumentService.engine.exception.RenderException;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.IOException;
import java.io.OutputStream;

// OpenHTMLtoPDF in memory: no base URI, built-in fonts, and only data: URIs can be loaded
public final class OpenHtmlPdfRenderer implements PdfRenderer {

    @Override
    public void render(String xhtml, OutputStream out) {
        try {
            // A new builder per call: PdfRendererBuilder is not thread-safe
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(xhtml, null);
            builder.useUriResolver(new DataUriOnlyResolver());
            builder.toStream(out);
            builder.run();
        } catch (IOException | RuntimeException e) {
            throw new RenderException("PDF could not be rendered", e);
        }
    }
}
