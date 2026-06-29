package se.sundsvall.caremanagement.attachments.service;

/**
 * A single file to feed into {@link AttachmentService#combineToPdf(java.util.List)} — the cross-module input to the
 * platform's heterogeneous PDF combiner. PDFs are inlined as-is, images are rasterised onto a page, {@code .docx} is
 * rendered, and anything else becomes a one-page placeholder, so an odd file never blocks the merge.
 *
 * @param fileName the file name (used for type detection and the placeholder page)
 * @param mimeType the MIME type (used for type detection)
 * @param content  the raw file bytes
 */
public record CombineSource(String fileName, String mimeType, byte[] content) {
}
