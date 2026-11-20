package com.joshsoftware.templateDrivenDocumentService.engine.render;

import com.joshsoftware.templateDrivenDocumentService.engine.definition.DocumentDefinition;

import java.util.Map;

// Fills the definition's template with the typed model and returns XHTML for the PDF renderer
public interface TemplateRenderer {

    // Throws RenderException if the template cannot be processed
    String renderXhtml(DocumentDefinition def, Map<String, Object> model);
}
