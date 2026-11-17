package com.joshsoftware.templateDrivenDocumentService.enums;

// Channel the customer asked for the document from.
// Recorded for reporting only; not verified against the caller in Stage 1.
public enum Channel {
    // Mobile banking app
    MOBILE,
    // Net banking
    NET_BANKING,
    // WhatsApp banking
    WHATSAPP
}
