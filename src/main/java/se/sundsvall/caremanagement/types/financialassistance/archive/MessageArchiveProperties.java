package se.sundsvall.caremanagement.types.financialassistance.archive;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the conversation-archiving job (see {@code MessageArchiveScheduler}). The job snapshots the
 * conversation of every closed EB errand into a {@code {errandNumber}_meddelandehistorik.pdf} once the errand has been
 * quiet for {@code daysAfterClose} days, then uploads it to the errand's Lifecare actualisation.
 *
 * @param municipalityId             the municipality whose errands are archived
 * @param namespace                  the namespace whose errands are archived (EB = {@code FINANCIAL_ASSISTANCE})
 * @param daysAfterClose             the settle delay — an errand is only archived once it has been closed (and
 *                                   otherwise untouched) for at least this many days. {@code 0} means archive on the
 *                                   next run after it closes
 * @param lifecareDocumentType       the Lifecare {@code InsertDocumentType} code for the uploaded document
 * @param lifecareDocumentSenderType the Lifecare {@code InsertDocumentSenderType} code for the uploaded document
 * @param documentLabel              the leading label of the document file name (and the Lifecare title), e.g.
 *                                   {@code Meddelanden och bilagor från Draken}
 * @param lifecareSenderName         the sender name shown in Lifecare
 */
@Validated
@ConfigurationProperties(prefix = "archive.message")
public record MessageArchiveProperties(

	@NotBlank @DefaultValue("2281") String municipalityId,

	@NotBlank @DefaultValue("FINANCIAL_ASSISTANCE") String namespace,

	@PositiveOrZero @DefaultValue("30") int daysAfterClose,

	@NotBlank @DefaultValue("MEDDELANDEHISTORIK") String lifecareDocumentType,

	@NotBlank @DefaultValue("MYNDIGHET") String lifecareDocumentSenderType,

	@NotBlank @DefaultValue("Meddelanden och bilagor från Draken") String documentLabel,

	@NotBlank @DefaultValue("Sundsvalls kommun") String lifecareSenderName) {
}
