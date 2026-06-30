package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApproval;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApprovals;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaSectionApprovalRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaSectionApprovalEntity;
import se.sundsvall.dept44.problem.Problem;

import static java.util.stream.Collectors.toMap;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * The caseworker approval state of the three Draken EB view sections — {@code CALCULATION} (calculation),
 * {@code PAYMENT} (payment) and {@code DECISION} (decision). Each section is an upsertable acknowledgeable object on
 * the errand: a caseworker verifies a section as approved (stamping who/when), or withdraws an earlier approval. A
 * section that has never been touched reads back as {@code approved=false}, so the bundle always carries all three.
 */
@Service
public class SectionApprovalService {

	public static final String SECTION_CALCULATION = "CALCULATION";
	public static final String SECTION_PAYMENT = "PAYMENT";
	public static final String SECTION_DECISION = "DECISION";

	private final FaSectionApprovalRepository repository;

	SectionApprovalService(final FaSectionApprovalRepository repository) {
		this.repository = repository;
	}

	/** The three sections' approval state bundled into one object — sections never touched default to not-approved. */
	@Transactional(readOnly = true)
	public SectionApprovals approvals(final String errandId) {
		final var bySection = repository.findByErrandId(errandId).stream()
			.collect(toMap(FaSectionApprovalEntity::getSection, entity -> entity, (a, _) -> a));
		return SectionApprovals.create()
			.withCalculation(toApproval(SECTION_CALCULATION, bySection.get(SECTION_CALCULATION)))
			.withPayment(toApproval(SECTION_PAYMENT, bySection.get(SECTION_PAYMENT)))
			.withDecision(toApproval(SECTION_DECISION, bySection.get(SECTION_DECISION)));
	}

	/**
	 * Set a section's approval (a caseworker action) — upserts the one row for {@code (errandId, section)}. Approving
	 * stamps {@code approvedBy}/{@code approvedAt}; withdrawing clears them. Throws {@code 400} when the section is not one
	 * of the three.
	 */
	@Transactional
	public SectionApproval setApproval(final String errandId, final String section, final boolean approved, final String approvedBy) {
		final var target = validateSection(section);
		final var entity = repository.findByErrandIdAndSection(errandId, target)
			.orElseGet(() -> FaSectionApprovalEntity.create().withErrandId(errandId).withSection(target));

		entity.setApproved(approved);
		final String resolvedApprovedBy;
		final OffsetDateTime resolvedApprovedAt;
		if (approved) {
			resolvedApprovedBy = approvedBy;
			resolvedApprovedAt = OffsetDateTime.now(ZoneId.systemDefault());
		} else {
			resolvedApprovedBy = null;
			resolvedApprovedAt = null;
		}
		entity.setApprovedBy(resolvedApprovedBy);
		entity.setApprovedAt(resolvedApprovedAt);

		return toApproval(target, repository.save(entity));
	}

	private static String validateSection(final String section) {
		if (!SECTION_CALCULATION.equals(section) && !SECTION_PAYMENT.equals(section) && !SECTION_DECISION.equals(section)) {
			throw Problem.valueOf(BAD_REQUEST, "section must be CALCULATION, PAYMENT or DECISION");
		}
		return section;
	}

	/** An entity (or absence) → its API view; a missing row reads as the section not yet approved. */
	private static SectionApproval toApproval(final String section, final FaSectionApprovalEntity entity) {
		if (entity == null) {
			return SectionApproval.create().withSection(section).withApproved(false);
		}
		return SectionApproval.create()
			.withSection(entity.getSection())
			.withApproved(entity.isApproved())
			.withApprovedBy(entity.getApprovedBy())
			.withApprovedAt(entity.getApprovedAt());
	}
}
