package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.attachments.service.AttachmentService;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.service.DecisionService;
import se.sundsvall.caremanagement.formsnapshot.api.model.FormSnapshot;
import se.sundsvall.caremanagement.formsnapshot.service.FormSnapshotService;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateFinancialAssistanceRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceView;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static java.util.Optional.ofNullable;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_RECEIVED;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.applicationTypeForSlug;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinancialAssistanceMapper.toEntity;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinancialAssistanceMapper.toStakeholders;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinancialAssistanceMapper.toView;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinancialAssistanceMapper.updateEntity;

/**
 * Creates and reads financial-assistance errands. The envelope is owned by the exposed core {@link ErrandService};
 * the {@code typeSlug} is one of the three financial assistance slugs (new / renewal / supplementary) and the stored
 * {@code applicationType}
 * is derived from it server-side, so the slug stays authoritative. Initial status is {@code RECEIVED}. The
 * strongly-typed
 * application data lives on this module's own table, keyed by the envelope id. The title falls back to the financial
 * assistance display
 * name when the client omits one.
 */
@Service
@Transactional
public class FinancialAssistanceErrandService {

	private static final String DEFAULT_TITLE = "Financial assistance";

	/** Decisions recorded by the automated pipelines, written as the drakel system actor. */
	private static final String RECOMMENDATION_TYPE = "RECOMMENDATION";

	private final ErrandService errandService;
	private final FinancialAssistanceRepository financialAssistanceRepository;
	private final StakeholderService stakeholderService;
	private final AttachmentService attachmentService;
	private final FormSnapshotService formSnapshotService;
	private final DecisionService decisionService;
	private final SectionApprovalService sectionApprovalService;

	FinancialAssistanceErrandService(final ErrandService errandService, final FinancialAssistanceRepository financialAssistanceRepository, final StakeholderService stakeholderService,
		final AttachmentService attachmentService, final FormSnapshotService formSnapshotService, final DecisionService decisionService,
		final SectionApprovalService sectionApprovalService) {
		this.errandService = errandService;
		this.financialAssistanceRepository = financialAssistanceRepository;
		this.stakeholderService = stakeholderService;
		this.attachmentService = attachmentService;
		this.formSnapshotService = formSnapshotService;
		this.decisionService = decisionService;
		this.sectionApprovalService = sectionApprovalService;
	}

	/**
	 * Create the financial assistance errand, persist its strongly-typed data, promote the application's persons
	 * (applicant/co-applicant) to
	 * core stakeholder rows on the errand, and — when the citizen supplied supporting files — store each attachment plus a
	 * single combined PDF merging them all. Attachments are type-agnostic: the list is handed to the attachments module
	 * as-is. The optional {@code caseData} file is the application snapshot (case data): it is stored as a single
	 * {@code CASE_DATA} attachment, renamed to {@code {errandNumber}.pdf}, so the whole errand is created in one call.
	 * The optional {@code formSnapshot} is the self-describing JSON snapshot of the form as the applicant saw it (every
	 * question, help/info/notice text, option label and answer): it is captured write-once for the legal record.
	 */
	public String create(final String municipalityId, final String namespace, final String typeSlug, final CreateFinancialAssistanceRequest request,
		final List<MultipartFile> attachments, final MultipartFile caseData, final String formSnapshot) {
		final var envelope = Errand.create()
			.withTypeSlug(typeSlug)
			.withTitle(ofNullable(request.getTitle()).orElse(DEFAULT_TITLE))
			.withStatus(STATUS_RECEIVED)
			.withDescription(request.getDescription())
			.withPriority(request.getPriority())
			.withReporterUserId(request.getReporterUserId())
			.withAssignedUserId(request.getAssignedUserId());

		final var errandId = errandService.createTypedErrand(municipalityId, namespace, envelope);

		final var entity = ofNullable(toEntity(request.getData(), errandId))
			.orElseGet(() -> FinancialAssistanceEntity.create().withErrandId(errandId));
		entity.setApplicationType(applicationTypeForSlug(typeSlug)); // slug is authoritative — overrides any client-sent value
		financialAssistanceRepository.save(entity);

		ofNullable(attachments)
			.filter(files -> !files.isEmpty())
			.ifPresent(files -> attachmentService.storeAndCombine(municipalityId, namespace, errandId, files));

		ofNullable(caseData)
			.ifPresent(file -> attachmentService.createCaseDataAttachment(municipalityId, namespace, errandId, file));

		ofNullable(formSnapshot)
			.filter(StringUtils::hasText)
			.ifPresent(payload -> formSnapshotService.saveErrandFormSnapshot(municipalityId, namespace, errandId, typeSlug, payload));

		// Promote the application's persons to stakeholders last: stakeholderService.create publishes an event, so keep it
		// after the other persistence steps to shrink the window where a later failure rolls back the row while the event
		// has already been published.
		toStakeholders(ofNullable(request.getData()).map(FinancialAssistanceData::getPersons).orElse(null))
			.forEach(stakeholder -> stakeholderService.create(municipalityId, namespace, errandId, stakeholder));

		return errandId;
	}

	/**
	 * The immutable form snapshot of an errand — the citizen-facing application form as it was rendered and answered, for
	 * re-display. Scoped: throws {@code 404} when the errand is missing in this namespace/municipality, or when no
	 * snapshot was captured.
	 */
	@Transactional(readOnly = true)
	public FormSnapshot readFormSnapshot(final String municipalityId, final String namespace, final String errandId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return formSnapshotService.readErrandFormSnapshot(errandId);
	}

	@Transactional(readOnly = true)
	public FinancialAssistanceView read(final String municipalityId, final String namespace, final String errandId) {
		final var envelope = errandService.readErrand(municipalityId, namespace, errandId);
		final var entity = financialAssistanceRepository.findByErrandId(errandId).orElse(null);
		return toView(envelope, entity)
			.withRecommendation(latestRecommendation(municipalityId, namespace, errandId))
			.withSectionApprovals(sectionApprovalService.approvals(errandId));
	}

	/** The most recent {@code RECOMMENDATION} decision on the errand (the automated recommendation), or null when none. */
	private Decision latestRecommendation(final String municipalityId, final String namespace, final String errandId) {
		return decisionService.readAll(municipalityId, namespace, errandId).stream()
			.filter(decision -> RECOMMENDATION_TYPE.equals(decision.getDecisionType()))
			.findFirst()
			.orElse(null);
	}

	/**
	 * Applies the non-null fields of {@code data} onto the errand's stored application data (PATCH semantics — null
	 * fields leave the existing values untouched). The server-owned fields ({@code applicationType},
	 * {@code lastDailyRunAt}, timestamps) are never written from client data.
	 */
	public void updateData(final String municipalityId, final String namespace, final String errandId, final FinancialAssistanceData data) {
		// Scope check — throws 404 when the errand is missing in this namespace/municipality.
		errandService.readErrand(municipalityId, namespace, errandId);
		final var entity = financialAssistanceRepository.findByErrandId(errandId)
			.orElseGet(() -> FinancialAssistanceEntity.create().withErrandId(errandId));
		financialAssistanceRepository.save(updateEntity(entity, data));
	}
}
