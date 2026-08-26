package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.document.service.DocumentService;
import se.sundsvall.caremanagement.document.service.LifecareDocumentMirror;
import se.sundsvall.caremanagement.journal.service.JournalEntryService;
import se.sundsvall.caremanagement.journal.service.LifecareJournalEntryMirror;
import se.sundsvall.caremanagement.shared.HtmlText;
import se.sundsvall.caremanagement.shared.MirrorOutcome;
import se.sundsvall.caremanagement.types.financialassistance.api.model.JobStimulusPeriod;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareDocumentRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareJobStimulusParty;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareReminder;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareSupplements;
import se.sundsvall.caremanagement.types.financialassistance.api.model.MonitoringRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SupplementsIngestOutcome;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SupplementsIngestResult;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaMonitoringRepository;

import static java.time.ZoneId.systemDefault;
import static java.util.Optional.ofNullable;
import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * The receiving end of the RPA supplements delivery — the single endpoint's engine. The robot dumps one near-raw
 * {@link LifecareSupplements} envelope per errand; this service routes each section to its module and reports a
 * per-item receipt:
 *
 * <ul>
 * <li>{@code reminders[]} — upserted as {@code LIFECARE}-sourced monitorings keyed per {@code reminderId} (stable in
 * Lifecare).</li>
 * <li>{@code documents[]} — routed on {@code documentType}: {@code 3} → journal mirror, {@code 0} → document mirror,
 * anything else SKIPPED. Body HTML is decoded and stripped to plain text.</li>
 * <li>{@code jobStimulus} — the errand's full period set is replaced (Lifecare regenerates all period ids on every
 * save, so there is no per-period identity to upsert on).</li>
 * </ul>
 *
 * <p>
 * Partial success by design: one broken item never fails the batch — it is reported {@code FAILED} in the receipt and
 * the rest proceed, each in its own transaction (this service deliberately opens none of its own). An omitted section
 * means 'not fetched this run' and leaves the errand untouched. Re-deliveries are free: every path is an upsert or a
 * full replace, so two identical deliveries end in the same state.
 * </p>
 */
@Service
public class SupplementsIngestService {

	static final String SECTION_REMINDERS = "reminders";
	static final String SECTION_DOCUMENTS = "documents";
	static final String SECTION_JOB_STIMULUS = "jobStimulus";

	static final String OUTCOME_CREATED = "CREATED";
	static final String OUTCOME_UPDATED = "UPDATED";
	static final String OUTCOME_REPLACED = "REPLACED";
	static final String OUTCOME_SKIPPED = "SKIPPED";
	static final String OUTCOME_FAILED = "FAILED";

	static final String ROLE_APPLICANT = "APPLICANT";
	static final String ROLE_CO_APPLICANT = "CO_APPLICANT";

	private static final Logger LOG = LoggerFactory.getLogger(SupplementsIngestService.class);

	private static final String SOURCE_LIFECARE = "LIFECARE";
	private static final String DOCUMENT_TYPE_JOURNAL_NOTE = "3";
	private static final String DOCUMENT_TYPE_REGULAR = "0";
	private static final String FALLBACK_HEADING = "(utan rubrik)";
	private static final String FALLBACK_TYPE = "Okänd typ";
	private static final int MAX_HEADING = 255;
	private static final int MAX_TYPE = 255;
	private static final int MAX_CREATED_BY = 64;

	private final ErrandService errandService;
	private final MonitoringService monitoringService;
	private final FaMonitoringRepository monitoringRepository;
	private final JournalEntryService journalEntryService;
	private final DocumentService documentService;
	private final JobStimulusPeriodService jobStimulusPeriodService;

	SupplementsIngestService(final ErrandService errandService, final MonitoringService monitoringService,
		final FaMonitoringRepository monitoringRepository, final JournalEntryService journalEntryService,
		final DocumentService documentService, final JobStimulusPeriodService jobStimulusPeriodService) {
		this.errandService = errandService;
		this.monitoringService = monitoringService;
		this.monitoringRepository = monitoringRepository;
		this.journalEntryService = journalEntryService;
		this.documentService = documentService;
		this.jobStimulusPeriodService = jobStimulusPeriodService;
	}

	/** Ingest one supplements delivery for the errand. Scoped: throws {@code 404} when the errand is missing here. */
	public SupplementsIngestResult ingest(final String municipalityId, final String namespace, final String errandId, final LifecareSupplements supplements) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)

		final var results = new ArrayList<SupplementsIngestOutcome>();
		ofNullable(supplements.reminders())
			.ifPresent(reminders -> reminders.forEach(reminder -> results.add(ingestReminder(municipalityId, namespace, errandId, reminder))));
		ofNullable(supplements.documents())
			.ifPresent(documents -> documents.forEach(document -> results.add(ingestDocument(municipalityId, namespace, errandId, document))));
		ofNullable(supplements.jobStimulus())
			.ifPresent(jobStimulus -> {
				final var periods = new ArrayList<JobStimulusPeriod>();
				collectPeriods(jobStimulus.applicant(), ROLE_APPLICANT, periods, results);
				collectPeriods(jobStimulus.coApplicant(), ROLE_CO_APPLICANT, periods, results);
				results.add(replaceJobStimulusPeriods(municipalityId, namespace, errandId, periods));
			});

		LOG.info("Supplements ingest for errand {}: {} item(s) processed", sanitizeForLogging(errandId), results.size());
		return new SupplementsIngestResult(results);
	}

	private SupplementsIngestOutcome ingestReminder(final String municipalityId, final String namespace, final String errandId, final LifecareReminder reminder) {
		if (reminder == null || !hasText(reminder.reminderId())) {
			return new SupplementsIngestOutcome(SECTION_REMINDERS, null, OUTCOME_SKIPPED, "reminderId missing — no stable key to upsert on");
		}
		final var lifecareId = reminder.reminderId().strip();
		try {
			final var startDate = parseRequiredDate(reminder.reminderDate(), "reminderDate");
			final var existed = monitoringRepository.findByErrandIdAndLifecareId(errandId, lifecareId).isPresent();
			monitoringService.create(municipalityId, namespace, errandId, MonitoringRequest.create()
				.withSource(SOURCE_LIFECARE)
				.withLifecareId(lifecareId)
				.withTitle(firstNonBlankTruncated(MAX_HEADING, reminder.typeText(), reminder.text(), "Bevakning"))
				.withDescription(reminder.text())
				.withStartDate(startDate)
				.withCreatedBy(truncate(reminder.caseworkerId(), MAX_CREATED_BY)));
			if (existed) {
				return new SupplementsIngestOutcome(SECTION_REMINDERS, lifecareId, OUTCOME_UPDATED, null);
			}
			return new SupplementsIngestOutcome(SECTION_REMINDERS, lifecareId, OUTCOME_CREATED, null);
		} catch (final Exception e) {
			return new SupplementsIngestOutcome(SECTION_REMINDERS, lifecareId, OUTCOME_FAILED, e.getMessage());
		}
	}

	private SupplementsIngestOutcome ingestDocument(final String municipalityId, final String namespace, final String errandId, final LifecareDocumentRow row) {
		if (row == null || !hasText(row.id())) {
			return new SupplementsIngestOutcome(SECTION_DOCUMENTS, null, OUTCOME_SKIPPED, "id missing — no stable key to upsert on");
		}
		final var lifecareId = row.id().strip();
		final var documentType = ofNullable(row.documentType()).map(String::strip).orElse("");
		try {
			return switch (documentType) {
				case DOCUMENT_TYPE_JOURNAL_NOTE -> toIngestOutcome(lifecareId, journalEntryService.mirrorFromLifecare(municipalityId, namespace, errandId,
					new LifecareJournalEntryMirror(lifecareId, mirroredType(row), mirroredHeading(row), HtmlText.toPlainText(row.content()),
						parseDateTime(row.date(), row.time()), truncate(row.updateSignature(), MAX_CREATED_BY))));
				case DOCUMENT_TYPE_REGULAR -> toIngestOutcome(lifecareId, documentService.mirrorFromLifecare(municipalityId, namespace, errandId,
					new LifecareDocumentMirror(lifecareId, mirroredType(row), mirroredHeading(row), HtmlText.toPlainText(row.content()),
						parseDateTime(row.date(), row.time()), truncate(row.updateSignature(), MAX_CREATED_BY))));
				default -> new SupplementsIngestOutcome(SECTION_DOCUMENTS, lifecareId, OUTCOME_SKIPPED,
					"unroutable documentType '" + documentType + "' — known: 3 (journal note), 0 (document)");
			};
		} catch (final Exception e) {
			return new SupplementsIngestOutcome(SECTION_DOCUMENTS, lifecareId, OUTCOME_FAILED, e.getMessage());
		}
	}

	private void collectPeriods(final LifecareJobStimulusParty party, final String role, final List<JobStimulusPeriod> periods, final List<SupplementsIngestOutcome> results) {
		ofNullable(party)
			.map(LifecareJobStimulusParty::periods)
			.ifPresent(partyPeriods -> partyPeriods.forEach(period -> {
				if (period == null || Boolean.TRUE.equals(period.markedForRemoval())) {
					return;
				}
				try {
					periods.add(new JobStimulusPeriod(role, parseRequiredDate(period.fromDate(), "fromDate"), parseOptionalDate(period.toDate())));
				} catch (final Exception e) {
					results.add(new SupplementsIngestOutcome(SECTION_JOB_STIMULUS, null, OUTCOME_FAILED, role + ": " + e.getMessage()));
				}
			}));
	}

	private SupplementsIngestOutcome replaceJobStimulusPeriods(final String municipalityId, final String namespace, final String errandId, final List<JobStimulusPeriod> periods) {
		try {
			final var stored = jobStimulusPeriodService.replaceAll(municipalityId, namespace, errandId, periods);
			return new SupplementsIngestOutcome(SECTION_JOB_STIMULUS, null, OUTCOME_REPLACED, stored + " period(s)");
		} catch (final Exception e) {
			return new SupplementsIngestOutcome(SECTION_JOB_STIMULUS, null, OUTCOME_FAILED, e.getMessage());
		}
	}

	private static SupplementsIngestOutcome toIngestOutcome(final String lifecareId, final MirrorOutcome outcome) {
		if (outcome.created()) {
			return new SupplementsIngestOutcome(SECTION_DOCUMENTS, lifecareId, OUTCOME_CREATED, null);
		}
		return new SupplementsIngestOutcome(SECTION_DOCUMENTS, lifecareId, OUTCOME_UPDATED, null);
	}

	private static String mirroredHeading(final LifecareDocumentRow row) {
		return firstNonBlankTruncated(MAX_HEADING, row.title(), row.type(), FALLBACK_HEADING);
	}

	private static String mirroredType(final LifecareDocumentRow row) {
		return firstNonBlankTruncated(MAX_TYPE, row.type(), row.typeCode(), FALLBACK_TYPE);
	}

	/**
	 * A required Lifecare date field ({@code yyyy-MM-dd}). Lifecare sends missing dates as the empty string, never
	 * {@code null} — both are rejected the same way here.
	 */
	private static LocalDate parseRequiredDate(final String value, final String fieldName) {
		if (!hasText(value)) {
			throw new IllegalArgumentException(fieldName + " missing");
		}
		try {
			return LocalDate.parse(value.strip());
		} catch (final DateTimeParseException e) {
			throw new IllegalArgumentException(fieldName + " unparseable: '" + value + "'");
		}
	}

	private static LocalDate parseOptionalDate(final String value) {
		if (!hasText(value)) {
			return null;
		}
		return LocalDate.parse(value.strip());
	}

	/** Lifecare's date + optional {@code HH:mm} time, combined at the system zone; midnight when the time is absent. */
	private static OffsetDateTime parseDateTime(final String date, final String time) {
		final var day = parseRequiredDate(date, "date");
		final LocalTime timeOfDay;
		if (hasText(time)) {
			try {
				timeOfDay = LocalTime.parse(time.strip());
			} catch (final DateTimeParseException e) {
				throw new IllegalArgumentException("time unparseable: '" + time + "'");
			}
		} else {
			timeOfDay = LocalTime.MIDNIGHT;
		}
		final var local = day.atTime(timeOfDay);
		return local.atOffset(systemDefault().getRules().getOffset(local));
	}

	private static String firstNonBlankTruncated(final int maxLength, final String... candidates) {
		for (final var candidate : candidates) {
			if (hasText(candidate)) {
				return truncate(candidate.strip(), maxLength);
			}
		}
		return null;
	}

	private static String truncate(final String value, final int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}
}
