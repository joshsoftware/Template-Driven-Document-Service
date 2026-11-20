package com.joshsoftware.templateDrivenDocumentService.engine.render;

import com.openhtmltopdf.extend.FSUriResolver;

// Lets the PDF load only data: URIs embedded in the template; anything else would reach the network or disk
final class DataUriOnlyResolver implements FSUriResolver {

    private static final String DATA_SCHEME = "data:";

    @Override
    public String resolveURI(String baseUri, String uri) {
        // Lower case only: the library's data: loader matches "data:" exactly
        if (uri != null && uri.startsWith(DATA_SCHEME)) {
            return uri;
        }
        // Throw, not return null: on null the library skips the resource and renders the PDF without it
        throw new IllegalArgumentException("only data: URIs may be loaded");
    }
}
