package se.sundsvall.caremanagement.types.financialassistance.service.event;

import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.stakeholders.api.model.Stakeholder;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderService;
import se.sundsvall.caremanagement.stakeholders.service.event.StakeholderMutated;

import static java.util.stream.Collectors.joining;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.ROLE_APPLICANT;

/**
 * Keeps the errand envelope's denormalized {@code applicantName} in sync with the financial assistance applicant. The
 * errand envelope has
 * no JPA relation to stakeholders (separate module/table), so the errand list can't otherwise be sorted or searched by
 * applicant — this listener maintains a sortable read-model copy on the envelope.
 *
 * <p>
 * Runs asynchronously after the stakeholder write commits ({@code @ApplicationModuleListener}, durably staged in the
 * Spring Modulith outbox). It recomputes the name from the errand's current {@code APPLICANT} stakeholder and writes it
 * via a targeted update that does <b>not</b> bump {@code touched}. Since only the financial assistance type contributes
 * an
 * {@code APPLICANT} role, non-financial-assistance errands resolve to no applicant and are left untouched (null stays
 * null). Errands that
 * have vanished by the time the event is handled (e.g. deleted) are skipped, so a delete never gets stuck retrying.
 */
@Component
class ApplicantNameSyncListener {

	private final ErrandRepository errandRepository;
	private final StakeholderService stakeholderService;

	ApplicantNameSyncListener(final ErrandRepository errandRepository, final StakeholderService stakeholderService) {
		this.errandRepository = errandRepository;
		this.stakeholderService = stakeholderService;
	}

	@ApplicationModuleListener
	void on(final StakeholderMutated event) {
		errandRepository.findByIdAndNamespaceAndMunicipalityId(event.errandId(), event.namespace(), event.municipalityId())
			.ifPresent(errand -> {
				final var applicantName = resolveApplicantName(event);
				if (!Objects.equals(errand.getApplicantName(), applicantName)) {
					errandRepository.updateApplicantName(event.errandId(), applicantName);
				}
			});
	}

	private String resolveApplicantName(final StakeholderMutated event) {
		return stakeholderService.readAll(event.municipalityId(), event.namespace(), event.errandId()).stream()
			.filter(stakeholder -> ROLE_APPLICANT.equals(stakeholder.getRole()))
			.findFirst() // at most one applicant per errand (maxOccurrences = 1)
			.map(ApplicantNameSyncListener::toDisplayName)
			.filter(StringUtils::hasText)
			.orElse(null);
	}

	/** Organisation name when present, otherwise the person's given + family name. */
	private static String toDisplayName(final Stakeholder stakeholder) {
		if (StringUtils.hasText(stakeholder.getOrganizationName())) {
			return stakeholder.getOrganizationName().trim();
		}
		return Stream.of(stakeholder.getFirstName(), stakeholder.getLastName())
			.filter(StringUtils::hasText)
			.map(String::trim)
			.collect(joining(" "));
	}
}
