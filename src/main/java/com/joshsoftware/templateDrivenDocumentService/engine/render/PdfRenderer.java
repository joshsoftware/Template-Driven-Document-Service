package com.joshsoftware.templateDrivenDocumentService.engine.render;

import java.io.OutputStream;

// Turns XHTML into PDF bytes written to the caller's stream
public interface PdfRenderer {

    // Throws RenderException if the XHTML cannot be laid out or loads anything but data: URIs
    void render(String xhtml, OutputStream out);
}
