package se.sundsvall.caremanagement.attachments.service;

/**
 * A single uploaded file read fully into memory once, so it can be both persisted as an attachment and fed to
 * {@link PdfCombiner} without re-reading the underlying multipart stream.
 *
 * @param fileName    the original filename (may be {@code null})
 * @param contentType the reported MIME type (may be {@code null})
 * @param content     the raw bytes (never {@code null})
 */
record SourceFile(String fileName, String contentType, byte[] content) {}
