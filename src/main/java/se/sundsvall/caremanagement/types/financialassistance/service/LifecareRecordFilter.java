package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareNodeValues.textOrEmpty;

/**
 * Picks the ekonomiskt bistånd rows out of a person's Lifecare journal and document list. ProfessionalWeb's list
 * (Document/GetDocumentsListForClient) is keyed on the person and spans every akt the person has across IFO — Vux,
 * BoU, LVM, the 14 kap utredningar — and an EB handläggare is to see only the EB ones.
 *
 * <p>
 * A row belongs to ekonomiskt bistånd when its owner, the insats, aktualisering or utredning it is written under, is of
 * an EB type. The row names that type as text ({@code ownerTypeText}), and Lifecare's catalogue starts every type name
 * with its area: {@code EK} for ekonomiskt bistånd (”EK Ekonomiskt bistånd”, ”EK Återansökan Digital Ekonomiskt
 * bistånd”), {@code Vux}, {@code BoU}. The row also carries its owner's id and type code, but telling an EB owner from
 * another by them would take reading every akt on the person, so the area is read from the name, as Lifecare's own
 * list shows it. The areas are configured per environment.
 * </p>
 *
 * <p>
 * The filter fails closed: a row whose owner type is unnamed, or whose area is not configured, is left out, so a
 * mistake hides an EB row rather than showing another unit's.
 * </p>
 */
@Component
public class LifecareRecordFilter {

	private static final String FIELD_DOCUMENT_MODELS = "documentModels";
	private static final String FIELD_OWNER_TYPE_TEXT = "ownerTypeText";

	private final Set<String> areas;

	public LifecareRecordFilter(@Value("${financial-assistance.lifecare.record-areas:EK}") final Set<String> areas) {
		this.areas = areas.stream()
			.map(String::strip)
			.filter(area -> !area.isEmpty())
			.map(area -> area.toUpperCase(Locale.ROOT))
			.collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * Whether a row of the list is written under an ekonomiskt bistånd owner.
	 *
	 * @param  row a row of GetDocumentsListForClient
	 * @return     true when the owner type's area is one of the configured areas
	 */
	public boolean isFinancialAssistance(final JsonNode row) {
		final var ownerType = textOrEmpty(row.path(FIELD_OWNER_TYPE_TEXT)).strip();
		if (ownerType.isEmpty()) {
			return false;
		}
		final var area = ownerType.split("\\s+", 2)[0].toUpperCase(Locale.ROOT);
		return areas.contains(area);
	}

	/**
	 * The list with only its ekonomiskt bistånd rows, in Lifecare's order. Everything else in the answer is kept.
	 *
	 * @param  list the GetDocumentsListForClient answer
	 * @return      a copy holding only the EB rows
	 */
	public JsonNode financialAssistanceOnly(final JsonNode list) {
		final ObjectNode filtered;
		if (list != null && list.isObject()) {
			filtered = (ObjectNode) list.deepCopy();
		} else {
			filtered = JsonNodeFactory.instance.objectNode();
		}
		final var rows = filtered.putArray(FIELD_DOCUMENT_MODELS);
		if (list != null) {
			list.path(FIELD_DOCUMENT_MODELS).valueStream()
				.filter(this::isFinancialAssistance)
				.forEach(rows::add);
		}
		return filtered;
	}
}
