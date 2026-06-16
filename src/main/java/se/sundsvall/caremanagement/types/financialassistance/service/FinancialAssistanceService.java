package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.YearMonth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.lifecare.service.NormberakningService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateFinancialAssistanceRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormberakningRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormberakningResponse;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static java.util.Optional.ofNullable;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_INKOMMEN;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.applicationTypeForSlug;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinancialAssistanceMapper.toEntity;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinancialAssistanceMapper.toView;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.NormberakningMapper.toResponse;

/**
 * Creates and reads financial-assistance (EB) errands. The envelope is owned by the exposed core {@link ErrandService};
 * the {@code typeSlug} is one of the three EB slugs (new / renewal / supplementary) and the stored
 * {@code applicationType}
 * is derived from it server-side, so the slug stays authoritative. Initial status is {@code INKOMMEN}. The
 * strongly-typed
 * application data lives on this module's own table, keyed by the envelope id. The title falls back to the EB display
 * name when the client omits one.
 */
@Service
@Transactional
public class FinancialAssistanceService {

	private static final String DEFAULT_TITLE = "Ekonomiskt bistånd";

	private final ErrandService errandService;
	private final FinancialAssistanceRepository repository;
	private final NormberakningService normberakningService;

	FinancialAssistanceService(final ErrandService errandService, final FinancialAssistanceRepository repository, final NormberakningService normberakningService) {
		this.errandService = errandService;
		this.repository = repository;
		this.normberakningService = normberakningService;
	}

	public String create(final String municipalityId, final String namespace, final String typeSlug, final CreateFinancialAssistanceRequest request) {
		final var envelope = Errand.create()
			.withTypeSlug(typeSlug)
			.withTitle(ofNullable(request.getTitle()).orElse(DEFAULT_TITLE))
			.withStatus(STATUS_INKOMMEN)
			.withDescription(request.getDescription())
			.withPriority(request.getPriority())
			.withReporterUserId(request.getReporterUserId())
			.withAssignedUserId(request.getAssignedUserId());

		final var errandId = errandService.createErrand(municipalityId, namespace, envelope);

		final var entity = ofNullable(toEntity(request.getData(), errandId))
			.orElseGet(() -> FinancialAssistanceEntity.create().withErrandId(errandId));
		entity.setApplicationType(applicationTypeForSlug(typeSlug)); // slug is authoritative — overrides any client-sent value
		repository.save(entity);
		return errandId;
	}

	@Transactional(readOnly = true)
	public FinancialAssistanceView read(final String municipalityId, final String namespace, final String errandId) {
		final var envelope = errandService.readErrand(municipalityId, namespace, errandId);
		final var entity = repository.findByErrandId(errandId).orElse(null);
		return toView(envelope, entity);
	}

	public void updateData(final String municipalityId, final String namespace, final String errandId, final FinancialAssistanceData data) {
		// Scope check — throws 404 when the errand is missing in this namespace/municipality.
		errandService.readErrand(municipalityId, namespace, errandId);
		repository.save(toEntity(data, errandId));
	}

	/**
	 * Build the SSBTEK-driven normberäkning for the application month and post it to Lifecare FC, returning the created
	 * calculation id plus the income warnings the handläggare must review.
	 */
	public NormberakningResponse createNormberakning(final String municipalityId, final NormberakningRequest request) {
		final var result = normberakningService.buildAndPost(municipalityId, request.getApplicant(), request.getCoApplicant(), YearMonth.parse(request.getApplicationMonth()));
		return toResponse(result);
	}
}
