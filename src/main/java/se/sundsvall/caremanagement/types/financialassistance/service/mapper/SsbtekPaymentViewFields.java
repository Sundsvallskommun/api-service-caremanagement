package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.util.List;
import java.util.stream.Stream;

/**
 * The fields of SSBTEK's agency answers that leave caremanagement on
 * {@code GET .../financial-assistance/{errandId}/ssbtek}:
 * what the caseworker's payment view (Draken, Hämta från SSBTEK) reads to list the payments Försäkringskassan,
 * Pensionsmyndigheten and the a-kassor report — benefit, payment date, type, amounts, periods and the delförmåner of a
 * specified payment. Nothing identifying is on the list: no personnummer, names or addresses, and no other agency.
 *
 * <p>
 * Dataminimering: this list is the whole of what a caller can get. A field is added only with a display need and a
 * mapping for it, and a screen that shows more later (the AF, CSN, SKV, MIV and TNS tab, say) adds its own paths here
 * — one group per agency and screen, each commented with the reader that needs it — never a wildcard and never a
 * whole answer. A field the reader stops using is removed. The path rules are those of {@link SsbtekFieldAllowlist}.
 * </p>
 *
 * <p>
 * The reader is Draken's backend, {@code backend/src/utils/ssbtek-*-payments.ts}; it takes each of these fields
 * defensively (a missing field, a lone item where a list is expected, a text where an object is expected, a number
 * given
 * as text), so the shapes below are a list of what it looks for, in the forms it accepts:
 * </p>
 * <ul>
 * <li>a code node is read as its {@code beskrivning}, else its {@code namn}, else the node itself as text — so the
 * node and both fields are listed</li>
 * <li>an amount of Försäkringskassan is read as its {@code summa}, else the node itself as a number — so the node and
 * {@code summa} are listed; the currency is not read</li>
 * <li>a period is read from {@code fran}/{@code till} (Försäkringskassan) or {@code from}/{@code tom}
 * (Pensionsmyndigheten)</li>
 * </ul>
 */
public final class SsbtekPaymentViewFields {

	/** A period as either agency writes it. */
	private static final List<String> PERIOD = List.of("fran", "till", "from", "tom");

	private static final String NETTOBELOPP = "nettobelopp";
	private static final String BRUTTOBELOPP = "bruttobelopp";

	/**
	 * A delförmån of one of Försäkringskassan's payments (ssbtek-fk-payments.ts, toPart): its förmån and beloppstyp, its
	 * period, extent as a fraction, hours and days, and its amounts.
	 */
	private static final List<String> FK_DETAIL = concat(
		under("period", PERIOD),
		described("forman"),
		described("beloppstyp"),
		List.of("omfattning.taljare", "omfattning.namnare", "timmar", "dagar"),
		amount(NETTOBELOPP),
		amount(BRUTTOBELOPP),
		amount("avdragsbelopp"),
		amount("skattebelopp"));

	/**
	 * One of Försäkringskassan's payments, made or announced (ssbtek-fk-payments.ts, toPayment): its förmånsfamilj, date,
	 * kind, period and amounts, and its delförmåner.
	 */
	private static final List<String> FK_PAYMENT = concat(
		List.of("datum"),
		described("typ"),
		described("formansfamilj"),
		under("period", PERIOD),
		amount(NETTOBELOPP),
		amount(BRUTTOBELOPP),
		amount("avdragsbelopp"),
		amount("skattebelopp"),
		under("utbetalningsdetalj", FK_DETAIL));

	/**
	 * One of Pensionsmyndigheten's payments, made or announced, which SSBTEK answers at the top of Försäkringskassan's
	 * answer (ssbtek-pension-payments.ts, toPayment): its date, period and totals, the deductions with their kind (told
	 * apart as tax or other by the code and the text), and the rows per förmån with the förmånsgrupp whose code prefix
	 * names the paying agency.
	 */
	private static final List<String> PM_PAYMENT = concat(
		List.of("utbetalningsdatum", NETTOBELOPP, BRUTTOBELOPP),
		under("utbetalningsperiod", PERIOD),
		under("avdrag", concat(List.of("belopp", "avdragstyp.kod"), described("avdragstyp"))),
		under("utbetalningsrader", concat(
			List.of("belopp", "formansgrupp.kod"),
			described("formansgrupp"),
			described("utbetalningsforman"),
			described("beloppstyp"))));

	/** One payment of an a-kassa (ssbtek-unemployment-payments.ts, toPayment): its date, period and net amount. */
	private static final List<String> SO_PAYMENT = List.of("Utbetalningsdatum", "NettoEfterAvdrag", "NettoEfterSkatt", "AvserFrom", "AvserTom");

	/**
	 * The payment view's paths: Försäkringskassan's made and announced payments, Pensionsmyndigheten's made and announced
	 * payments (both under {@code fk}), and the a-kassor's payments (under {@code so}). Add a screen's paths as a new
	 * group here.
	 */
	static final List<String> PATHS = concat(
		under("fk.formansinformation.utbetalningsuppgift", FK_PAYMENT),
		under("fk.formansinformation.preliminarautbetalningar", FK_PAYMENT),
		under("fk.utbetalningar", PM_PAYMENT),
		under("fk.preliminaraUtbetalningar", PM_PAYMENT),
		under("so.ArbetsloshetsersattningLista.Arbetsloshetsersattning.Utbetalningar", SO_PAYMENT));

	/** {@link #PATHS} ready to apply. */
	public static final SsbtekFieldAllowlist ALLOWLIST = SsbtekFieldAllowlist.of(PATHS);

	private SsbtekPaymentViewFields() {}

	/** A code node: the node itself as text, or its {@code beskrivning}, or its {@code namn}. */
	private static List<String> described(final String node) {
		return List.of(node, node + ".beskrivning", node + ".namn");
	}

	/** An amount of Försäkringskassan: the node itself as a number, or its {@code summa}. */
	private static List<String> amount(final String node) {
		return List.of(node, node + ".summa");
	}

	private static List<String> under(final String prefix, final List<String> fields) {
		return fields.stream().map(field -> prefix + "." + field).toList();
	}

	@SafeVarargs
	private static List<String> concat(final List<String>... groups) {
		return Stream.of(groups).flatMap(List::stream).toList();
	}
}
