package se.sundsvall.caremanagement.document.service;

import java.time.OffsetDateTime;

/**
 * The fields of a Lifecare document to mirror onto an errand — the input to
 * {@link DocumentService#mirrorFromLifecare}. {@code lifecareId} is the document's id in Lifecare's document list (the
 * upsert key), the rest are the mirrored Lifecare fields: Typ, Rubrik, the plain-text body, Datum/Tid and Upprättad av.
 */
public record LifecareDocumentMirror(
	String lifecareId,
	String type,
	String heading,
	String text,
	OffsetDateTime documentDateTime,
	String createdBy) {}
