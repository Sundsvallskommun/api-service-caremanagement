package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefc.PersonBasedCalculationCalculationIncomeTypeDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationIncomePostDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFcIntegration;
import se.sundsvall.caremanagement.lifecare.service.mapper.ClassifiedIncomeToFcMapper;
import se.sundsvall.caremanagement.lifecare.service.mapper.NormberakningAssembler;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.caremanagement.lifecare.service.model.DraftRow;
import se.sundsvall.caremanagement.lifecare.service.model.NormberakningDraftBuild;
import se.sundsvall.caremanagement.lifecare.service.model.NormberakningResult;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Builds and posts the normberäkning to Lifecare FC from incomes already classified by the operaton regelverk.
 * caremanagement no longer fetches SSBTEK or evaluates the rålista — the regelverk lives in the process. This service
 * resolves each classified income's category to an FC income-type id (via {@link ClassifiedIncomeToFcMapper}),
 * assembles
 * the calculation against the applicant's proposal (via {@link NormberakningAssembler}), and posts it. It also reports
 * whether this month's normberäkning covers every income type the previous month's did — the EB process polls SSBTEK
 * daily until it does.
 */
@Service
public class NormberakningService {

	private static final Logger LOG = LoggerFactory.getLogger(NormberakningService.class);

	private final LifecareFcIntegration lifecareFcIntegration;
	private final LifecareEbCaseService lifecareEbCaseService;
	private final ObjectMapper objectMapper;

	public NormberakningService(final LifecareFcIntegration lifecareFcIntegration, final LifecareEbCaseService lifecareEbCaseService,
		final ObjectMapper objectMapper) {
		this.lifecareFcIntegration = lifecareFcIntegration;
		this.lifecareEbCaseService = lifecareEbCaseService;
		this.objectMapper = objectMapper;
	}

	/**
	 * Build and post the normberäkning from incomes already classified by the operaton regelverk, and report whether the
	 * information is complete — i.e. whether every income type on the previous month's normberäkning is present this
	 * month. The completeness lookup is best-effort: a failure reading the previous normberäkning never fails the just-
	 * created calculation; it is reported as complete (with no missing types) so the process is not wedged.
	 *
	 * @param  applicantPersonId     the applicant's personnummer (the FC proposal owner)
	 * @param  applicationMonth      the month the application concerns
	 * @param  classifiedIncomesJson the operaton {@code classifiedIncomes} JSON
	 * @return                       the created calculation id plus the completeness verdict + missing income types
	 */
	public NormberakningResult buildAndPostFromClassified(final String applicantPersonId, final YearMonth applicationMonth, final String classifiedIncomesJson) {
		final var classified = parse(classifiedIncomesJson);
		final var proposal = lifecareFcIntegration.getCalculationProposal(applicantPersonId);
		final var incomes = ClassifiedIncomeToFcMapper.toCalculationIncomes(classified, proposal);
		final var body = NormberakningAssembler.assemble(applicantPersonId, proposal, incomes, applicationMonth);
		final var calculationId = lifecareFcIntegration.createCalculation(body);

		final var missing = missingPreviousIncomeTypes(applicantPersonId, applicationMonth, classified, proposal);
		return new NormberakningResult(calculationId, missing.isEmpty(), missing);
	}

	/**
	 * Build the draft normberäkning from the classified incomes — the FC income rows plus the completeness verdict —
	 * <strong>without</strong> creating anything in Lifecare. The EB process stores the rows as an editable draft and
	 * polls SSBTEK daily until the information is complete; the Lifecare normberäkning is created later, on a beslut, from
	 * the (possibly edited) draft (see {@link #postDraftRows}).
	 *
	 * @param  applicantPersonId     the applicant's personnummer (the FC proposal owner)
	 * @param  applicationMonth      the month the application concerns
	 * @param  classifiedIncomesJson the operaton {@code classifiedIncomes} JSON
	 * @return                       the FC income rows + completeness verdict; nothing is written to Lifecare
	 */
	public NormberakningDraftBuild buildDraft(final String applicantPersonId, final YearMonth applicationMonth, final String classifiedIncomesJson) {
		final var classified = parse(classifiedIncomesJson);
		final var proposal = lifecareFcIntegration.getCalculationProposal(applicantPersonId);
		final var incomes = ClassifiedIncomeToFcMapper.toCalculationIncomes(classified, proposal);
		final var typeNamesById = incomeTypeNamesById(proposal);
		final var rows = incomes.stream().map(income -> toDraftRow(income, typeNamesById)).toList();
		final var missing = missingPreviousIncomeTypes(applicantPersonId, applicationMonth, classified, proposal);
		return new NormberakningDraftBuild(rows, missing.isEmpty(), missing);
	}

	/**
	 * Create the normberäkning in Lifecare FC from the (possibly handläggare-edited) draft income rows — called on a
	 * beslut. Assembles against the applicant's proposal and posts; returns the created calculation id.
	 */
	public Integer postDraftRows(final String applicantPersonId, final YearMonth applicationMonth, final List<DraftRow> rows) {
		final var proposal = lifecareFcIntegration.getCalculationProposal(applicantPersonId);
		final var incomes = ofNullable(rows).orElseGet(List::of).stream().map(NormberakningService::toPostDto).toList();
		final var body = NormberakningAssembler.assemble(applicantPersonId, proposal, incomes, applicationMonth);
		return lifecareFcIntegration.createCalculation(body);
	}

	private static Map<Integer, String> incomeTypeNamesById(final PersonBasedCalculationProposalDTO proposal) {
		return ofNullable(proposal)
			.map(PersonBasedCalculationProposalDTO::getCalculationIncomeTypes)
			.orElseGet(List::of).stream()
			.filter(type -> (type.getId() != null) && (type.getName() != null))
			.collect(java.util.stream.Collectors.toMap(PersonBasedCalculationCalculationIncomeTypeDTO::getId,
				PersonBasedCalculationCalculationIncomeTypeDTO::getName, (first, second) -> first, LinkedHashMap::new));
	}

	private static DraftRow toDraftRow(final PersonBasedCalculationIncomePostDTO income, final Map<Integer, String> typeNamesById) {
		return new DraftRow(income.getId(), typeNamesById.get(income.getId()),
			income.getApplicantAmount(), toIso(income.getApplicantAmountDate()),
			income.getCoApplicantAmount(), toIso(income.getCoApplicantAmountDate()),
			income.getNote());
	}

	private static PersonBasedCalculationIncomePostDTO toPostDto(final DraftRow row) {
		return new PersonBasedCalculationIncomePostDTO()
			.id(row.typeId())
			.applicantAmount(row.applicantAmount())
			.applicantAmountDate(fromIso(row.applicantAmountDate()))
			.coApplicantAmount(row.coApplicantAmount())
			.coApplicantAmountDate(fromIso(row.coApplicantAmountDate()))
			.note(row.note());
	}

	private static String toIso(final OffsetDateTime date) {
		return ofNullable(date).map(OffsetDateTime::toString).orElse(null);
	}

	private static OffsetDateTime fromIso(final String iso) {
		return ofNullable(iso).filter(StringUtils::hasText).map(OffsetDateTime::parse).orElse(null);
	}

	private List<String> missingPreviousIncomeTypes(final String applicantPersonId, final YearMonth applicationMonth,
		final List<ClassifiedIncome> classified, final PersonBasedCalculationProposalDTO proposal) {
		try {
			final var previousTypes = lifecareEbCaseService.previousNormberakningIncomeTypes(applicantPersonId, applicationMonth);
			return ClassifiedIncomeToFcMapper.missingPreviousIncomeTypes(previousTypes, classified, proposal);
		} catch (final RuntimeException e) {
			LOG.warn("Could not determine normberäkning completeness against the previous month — treating as complete", e);
			return List.of();
		}
	}

	private List<ClassifiedIncome> parse(final String classifiedIncomesJson) {
		try {
			return List.of(objectMapper.readValue(classifiedIncomesJson, ClassifiedIncome[].class));
		} catch (final JacksonException e) {
			throw Problem.valueOf(BAD_REQUEST, "Invalid classifiedIncomes JSON");
		}
	}
}
