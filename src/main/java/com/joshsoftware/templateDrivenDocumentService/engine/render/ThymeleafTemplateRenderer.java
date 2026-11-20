package com.joshsoftware.templateDrivenDocumentService.engine.render;

import com.joshsoftware.templateDrivenDocumentService.engine.definition.DocumentDefinition;
import com.joshsoftware.templateDrivenDocumentService.engine.exception.RenderException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateEngineException;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Locale;
import java.util.Map;

// Standalone Thymeleaf (no Spring): the template text comes from the definition, not from files
public final class ThymeleafTemplateRenderer implements TemplateRenderer {

    // Month names and number formats are the same whatever the server's default locale is
    private static final Locale LOCALE = Locale.ENGLISH;

    private final TemplateEngine engine;

    public ThymeleafTemplateRenderer() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        // Each version's template is a different string, so caching by content would only grow memory
        resolver.setCacheable(false);
        this.engine = new TemplateEngine();
        this.engine.setTemplateResolver(resolver);
    }

    @Override
    public String renderXhtml(DocumentDefinition def, Map<String, Object> model) {
        try {
            return engine.process(def.templateHtml(), new Context(LOCALE, model));
        } catch (TemplateEngineException e) {
            // Thymeleaf's message quotes the template; the handler logs only the cause type
            throw new RenderException("template could not be rendered", e);
        }
    }
}
