package com.joshsoftware.templateDrivenDocumentService.dto;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

// An open stored PDF: stream it, then close it (the S3 connection is held until then).
// contentLength is -1 when the store did not report a size.
public record StoredDocument(InputStream content, long contentLength) implements Closeable {

    @Override
    public void close() throws IOException {
        content.close();
    }
}
