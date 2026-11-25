package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The payment view's allowlist against an SSBTEK-shaped answer: the payment fields of Försäkringskassan,
 * Pensionsmyndigheten and the a-kassor survive with their values, and the identity fields and every other agency are
 * gone. The persons are made up; the personnummer are not real.
 */
class SsbtekPaymentViewFieldsTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final String APPLICANT_PERSONAL_NUMBER = "199001011234";
	private static final String RECIPIENT_PERSONAL_NUMBER = "199202021234";

	/** What identifies a person in the answers below; none of it may be in the result. */
	private static final List<String> IDENTITY = List.of(APPLICANT_PERSONAL_NUMBER, RECIPIENT_PERSONAL_NUMBER, "Testsson", "Testgatan", "85000", "Testkommun", "Testkassan", "HL000001", "162000000001");

	/**
	 * The answers per agency as api-service-financial-aid hands them over: fk as LEFI JSON (Försäkringskassan under
	 * formansinformation, Pensionsmyndigheten at the top), the others converted from the SOAP XML. The first payment of
	 * each kind has its codes and amounts as objects, the later ones as plain text and numbers, and a lone item stands
	 * where a list is usual, because the readers accept all of it.
	 */
	private static final String ANSWER = """
		{
		  "af": {
		    "Svar": {
		      "Schemaversion": "4.0",
		      "IdentitetsbeteckningFysiskPerson": "199001011234",
		      "Fornamn": "Test",
		      "Efternamn": "Testsson",
		      "Sekretessmarkering": false,
		      "ArbetssokandeInfo": { "Arbetssokande": true, "ArbetssokandeSKAT": "Anställd" },
		      "Akassetillhorighet": "Test ak"
		    }
		  },
		  "csn": { "Personer": { "Person": { "personnummer": "199001011234", "Studiemedel": { "belopp": 0 } } } },
		  "skv": {
		    "personnummer": "199001011234",
		    "namn": "Test Testsson",
		    "adress": { "gatuadress": "Testgatan 1", "postnummer": "85000", "postort": "Sundsvall" },
		    "inkomst": [ { "belopp": 1000, "arbetsgivare": "Testkommun" } ]
		  },
		  "tns": { "resultat": "OK", "fordonsinnehav": { "fordon": { "regnr": "ABC001", "fabrikat": "Volvo" } } },
		  "miv": { "personnummer": "199001011234", "uppehallstillstand": { "giltigTom": "2027-01-01" } },
		  "fk": {
		    "utlamnare": [
		      { "utlamnare": { "kod": "FK", "namn": "Försäkringskassan" }, "tillstand": { "kod": "OK", "beskrivning": "Tillgänglig" } }
		    ],
		    "formansinformation": {
		      "generellPersoninformation": [
		        {
		          "personnummer": "199001011234",
		          "foddes": "1990-01-01",
		          "fornamn": "Test",
		          "efternamn": "Testsson",
		          "adress": { "gatuadress": "Testgatan 1", "postnummer": "85000", "postort": "Sundsvall" }
		        }
		      ],
		      "utbetalningsuppgift": [
		        {
		          "typ": { "id": 1, "beskrivning": "Månad" },
		          "formansfamilj": { "id": "BOB", "beskrivning": "Bostadsbidrag" },
		          "datum": "2026-09-25",
		          "period": { "fran": "2026-09-01", "till": "2026-09-30" },
		          "nettobelopp": { "summa": 4500, "valuta": "SEK" },
		          "utbetalningsmottagare": { "personnummer": "199202021234", "namn": "Alva Testsson", "adress": { "gatuadress": "Testgatan 1" } }
		        },
		        {
		          "typ": "Daglig",
		          "formansfamilj": "Dagersättning",
		          "datum": "2026-08-21",
		          "period": { "from": "2026-08-01", "tom": "2026-08-31" },
		          "nettobelopp": 513,
		          "bruttobelopp": "600,50"
		        }
		      ],
		      "preliminarautbetalningar": [
		        {
		          "typ": { "id": 4, "beskrivning": "preliminär" },
		          "formansfamilj": { "id": "DAG", "beskrivning": "Dagersättning" },
		          "datum": "2026-09-27",
		          "bruttobelopp": { "summa": 3248, "valuta": "SEK" },
		          "skattebelopp": { "summa": 0, "valuta": "SEK" },
		          "avdragsbelopp": { "summa": 0, "valuta": "SEK" },
		          "nettobelopp": { "summa": 3248, "valuta": "SEK" },
		          "utbetalningsdetalj": [
		            {
		              "forman": { "id": "AMP", "beskrivning": "Arbetsmarknadspolitiskt program" },
		              "period": { "fran": "2026-08-01", "till": "2026-08-26" },
		              "beloppstyp": { "id": "EE", "namn": "Etableringsersättning" },
		              "omfattning": { "taljare": 50, "namnare": 100 },
		              "dagar": 19,
		              "timmar": 0,
		              "bruttobelopp": { "summa": 2831, "valuta": "SEK" },
		              "nettobelopp": 2831,
		              "avdragsbelopp": { "summa": 0 },
		              "skattebelopp": 0,
		              "arendeId": "A-1",
		              "mottagare": { "personnummer": "199202021234" }
		            },
		            {
		              "forman": "Arbetsmarknadspolitiskt program",
		              "period": { "from": "2026-08-27", "tom": "2026-08-31" },
		              "beloppstyp": "Etableringsersättning",
		              "bruttobelopp": 417
		            }
		          ]
		        }
		      ],
		      "programjobdagar": [ { "antalForbrukade": 0, "harForbrukatMaxAntal": false } ]
		    },
		    "utbetalningar": [
		      {
		        "utbetalningsdatum": "2026-08-18",
		        "utbetalningsperiod": { "from": "2026-08-01", "tom": "2026-08-31" },
		        "bruttobelopp": 16361,
		        "nettobelopp": 14109,
		        "mottagare": { "personnummer": "199001011234", "namn": "Test Testsson" },
		        "avdrag": [
		          { "belopp": -2252, "avdragstyp": { "kod": "PM:PS", "beskrivning": "Preliminär skatt" } },
		          { "belopp": -100, "avdragstyp": { "kod": "PM:UH", "namn": "Underhållsavdrag" }, "betalmottagare": "Testsson" }
		        ],
		        "utbetalningsrader": [
		          {
		            "formansgrupp": { "kod": "PM:BTI", "beskrivning": "Bostads- och inkomststöd" },
		            "utbetalningsforman": { "kod": "PM:BTP", "beskrivning": "Bostadstillägg" },
		            "beloppstyp": { "kod": "PM:BTP", "beskrivning": "Bostadstillägg" },
		            "belopp": 852
		          },
		          {
		            "formansgrupp": { "kod": "PM:EF", "beskrivning": "Efterlevandeförmåner" },
		            "utbetalningsforman": "Efterlevandepension",
		            "beloppstyp": "Änkepension",
		            "belopp": 4135,
		            "beslutsnummer": "B-77"
		          }
		        ]
		      }
		    ],
		    "preliminaraUtbetalningar": [
		      {
		        "utbetalningsdatum": "2026-09-18",
		        "utbetalningsperiod": { "fran": "2026-09-01", "till": "2026-09-30" },
		        "nettobelopp": 5000,
		        "avdrag": { "belopp": -100, "avdragstyp": { "kod": "PM:UH", "beskrivning": "Underhållsavdrag" } },
		        "utbetalningsrader": {
		          "formansgrupp": { "kod": "PM:EF", "namn": "Efterlevandeförmåner" },
		          "utbetalningsforman": { "kod": "PM:EP", "beskrivning": "Efterlevandepension" },
		          "beloppstyp": { "kod": "PM:ANKEP", "beskrivning": "Änkepension" },
		          "belopp": 5100
		        }
		      }
		    ]
		  },
		  "so": {
		    "OrganisationsnummerIngivare": "162000000001",
		    "NamnIngivare": "Testkommun",
		    "KontonamnHandlaggare": "HL000001",
		    "Referensnummer": "AID00000001",
		    "From": "2026-08-01",
		    "Tom": "2026-10-01",
		    "IdentificationNumber": "199001011234",
		    "ArbetsloshetsersattningLista": {
		      "Arbetsloshetsersattning": [
		        {
		          "SvarandeOrganisation": "Testkassan Svarar",
		          "StatusSvarandeOrganisation": "all clear",
		          "Utbetalningar": [
		            {
		              "Utbetalningsdatum": "2026-09-25",
		              "NettoEfterSkatt": "1600.00",
		              "NettoEfterAvdrag": "1600.00",
		              "Ersattningsdagar": "4.5",
		              "AvserFrom": "2026-09-01",
		              "AvserTom": "2026-09-05"
		            },
		            { "Utbetalningsdatum": "2026-08-25", "NettoEfterSkatt": 900.5, "AvserFrom": "2026-08-01", "AvserTom": "2026-08-05" }
		          ]
		        },
		        { "SvarandeOrganisation": "Testkassan Tyst", "StatusSvarandeOrganisation": "1" },
		        {
		          "SvarandeOrganisation": "Testkassan Enkel",
		          "Utbetalningar": {
		            "Utbetalningsdatum": "2026-07-23",
		            "NettoEfterSkatt": "1250.50",
		            "NettoEfterAvdrag": "1200.50",
		            "AvserFrom": "2026-07-03",
		            "AvserTom": "2026-07-16"
		          }
		        }
		      ]
		    }
		  }
		}
		""";

	/** The same answers as the payment view has them: the payment fields, with the shapes the agencies gave them. */
	private static final String RETAINED = """
		{
		  "fk": {
		    "formansinformation": {
		      "utbetalningsuppgift": [
		        {
		          "typ": { "beskrivning": "Månad" },
		          "formansfamilj": { "beskrivning": "Bostadsbidrag" },
		          "datum": "2026-09-25",
		          "period": { "fran": "2026-09-01", "till": "2026-09-30" },
		          "nettobelopp": { "summa": 4500 }
		        },
		        {
		          "typ": "Daglig",
		          "formansfamilj": "Dagersättning",
		          "datum": "2026-08-21",
		          "period": { "from": "2026-08-01", "tom": "2026-08-31" },
		          "nettobelopp": 513,
		          "bruttobelopp": "600,50"
		        }
		      ],
		      "preliminarautbetalningar": [
		        {
		          "typ": { "beskrivning": "preliminär" },
		          "formansfamilj": { "beskrivning": "Dagersättning" },
		          "datum": "2026-09-27",
		          "bruttobelopp": { "summa": 3248 },
		          "skattebelopp": { "summa": 0 },
		          "avdragsbelopp": { "summa": 0 },
		          "nettobelopp": { "summa": 3248 },
		          "utbetalningsdetalj": [
		            {
		              "forman": { "beskrivning": "Arbetsmarknadspolitiskt program" },
		              "period": { "fran": "2026-08-01", "till": "2026-08-26" },
		              "beloppstyp": { "namn": "Etableringsersättning" },
		              "omfattning": { "taljare": 50, "namnare": 100 },
		              "dagar": 19,
		              "timmar": 0,
		              "bruttobelopp": { "summa": 2831 },
		              "nettobelopp": 2831,
		              "avdragsbelopp": { "summa": 0 },
		              "skattebelopp": 0
		            },
		            {
		              "forman": "Arbetsmarknadspolitiskt program",
		              "period": { "from": "2026-08-27", "tom": "2026-08-31" },
		              "beloppstyp": "Etableringsersättning",
		              "bruttobelopp": 417
		            }
		          ]
		        }
		      ]
		    },
		    "utbetalningar": [
		      {
		        "utbetalningsdatum": "2026-08-18",
		        "utbetalningsperiod": { "from": "2026-08-01", "tom": "2026-08-31" },
		        "bruttobelopp": 16361,
		        "nettobelopp": 14109,
		        "avdrag": [
		          { "belopp": -2252, "avdragstyp": { "kod": "PM:PS", "beskrivning": "Preliminär skatt" } },
		          { "belopp": -100, "avdragstyp": { "kod": "PM:UH", "namn": "Underhållsavdrag" } }
		        ],
		        "utbetalningsrader": [
		          {
		            "formansgrupp": { "kod": "PM:BTI", "beskrivning": "Bostads- och inkomststöd" },
		            "utbetalningsforman": { "beskrivning": "Bostadstillägg" },
		            "beloppstyp": { "beskrivning": "Bostadstillägg" },
		            "belopp": 852
		          },
		          {
		            "formansgrupp": { "kod": "PM:EF", "beskrivning": "Efterlevandeförmåner" },
		            "utbetalningsforman": "Efterlevandepension",
		            "beloppstyp": "Änkepension",
		            "belopp": 4135
		          }
		        ]
		      }
		    ],
		    "preliminaraUtbetalningar": [
		      {
		        "utbetalningsdatum": "2026-09-18",
		        "utbetalningsperiod": { "fran": "2026-09-01", "till": "2026-09-30" },
		        "nettobelopp": 5000,
		        "avdrag": { "belopp": -100, "avdragstyp": { "kod": "PM:UH", "beskrivning": "Underhållsavdrag" } },
		        "utbetalningsrader": {
		          "formansgrupp": { "kod": "PM:EF", "namn": "Efterlevandeförmåner" },
		          "utbetalningsforman": { "beskrivning": "Efterlevandepension" },
		          "beloppstyp": { "beskrivning": "Änkepension" },
		          "belopp": 5100
		        }
		      }
		    ]
		  },
		  "so": {
		    "ArbetsloshetsersattningLista": {
		      "Arbetsloshetsersattning": [
		        {
		          "Utbetalningar": [
		            {
		              "Utbetalningsdatum": "2026-09-25",
		              "NettoEfterSkatt": "1600.00",
		              "NettoEfterAvdrag": "1600.00",
		              "AvserFrom": "2026-09-01",
		              "AvserTom": "2026-09-05"
		            },
		            { "Utbetalningsdatum": "2026-08-25", "NettoEfterSkatt": 900.5, "AvserFrom": "2026-08-01", "AvserTom": "2026-08-05" }
		          ]
		        },
		        {},
		        {
		          "Utbetalningar": {
		            "Utbetalningsdatum": "2026-07-23",
		            "NettoEfterSkatt": "1250.50",
		            "NettoEfterAvdrag": "1200.50",
		            "AvserFrom": "2026-07-03",
		            "AvserTom": "2026-07-16"
		          }
		        }
		      ]
		    }
		  }
		}
		""";

	@Test
	void returnsThePaymentFieldsAndNothingElse() {
		final var result = SsbtekPaymentViewFields.ALLOWLIST.retainIn(agencies(ANSWER));

		assertThat(result).isEqualTo(agencies(RETAINED));
	}

	@Test
	void dropsEveryIdentityFieldAndEveryAgencyThePaymentViewDoesNotRead() throws Exception {
		final var result = SsbtekPaymentViewFields.ALLOWLIST.retainIn(agencies(ANSWER));

		assertThat(result).containsOnlyKeys("fk", "so");
		assertThat(JSON.writeValueAsString(result)).doesNotContain(IDENTITY);
		assertThat(keyNames(result)).doesNotContain(
			"personnummer", "foddes", "fornamn", "efternamn", "adress", "gatuadress", "postnummer", "postort",
			"generellPersoninformation", "utbetalningsmottagare", "mottagare", "betalmottagare", "utlamnare", "tillstand",
			"IdentificationNumber", "NamnIngivare", "OrganisationsnummerIngivare", "KontonamnHandlaggare", "SvarandeOrganisation");
	}

	@Test
	void letsNothingThroughThatIsNotAnAllowedPath() {
		final var result = SsbtekPaymentViewFields.ALLOWLIST.retainIn(agencies(ANSWER));

		assertThat(valuePaths(result)).isNotEmpty().isSubsetOf(SsbtekPaymentViewFields.PATHS);
	}

	@Test
	void hasNoPathTwiceAndNoneOutsideTheAgenciesItReads() {
		assertThat(SsbtekPaymentViewFields.PATHS)
			.doesNotHaveDuplicates()
			.allSatisfy(path -> assertThat(path).matches("(fk|so)\\..+"));
	}

	@ParameterizedTest
	@MethodSource("allowedPaths")
	void keepsAnAllowedPathWithItsValueAmongIdentityFields(final String path) {
		final var steps = List.of(path.split("\\."));
		final var value = "value of " + path;

		for (final var listed : List.of(false, true)) {
			final var answer = nest(steps.subList(1, steps.size()), value, true, listed);
			final var expected = nest(steps.subList(1, steps.size()), value, false, listed);

			assertThat(SsbtekPaymentViewFields.ALLOWLIST.retainIn(Map.of(steps.getFirst(), answer)))
				.as("%s, each level as a list: %s", path, listed)
				.isEqualTo(Map.of(steps.getFirst(), expected));
		}
	}

	@Test
	void answersNoAgenciesWhenNoneAnswered() {
		assertThat(SsbtekPaymentViewFields.ALLOWLIST.retainIn(null)).isNotNull().isEmpty();
		assertThat(SsbtekPaymentViewFields.ALLOWLIST.retainIn(Map.of())).isNotNull().isEmpty();
	}

	@Test
	void keepsAnAgencyThatAnsweredWithoutPayments() {
		final var answer = agencies("""
			{
			  "fk": {
			    "utlamnare": [ { "utlamnare": { "kod": "FK", "namn": "Försäkringskassan" } } ],
			    "formansinformation": { "generellPersoninformation": [ { "personnummer": "199001011234" } ], "utbetalningsuppgift": [], "preliminarautbetalningar": [] },
			    "utbetalningar": [],
			    "preliminaraUtbetalningar": []
			  },
			  "so": {}
			}
			""");

		assertThat(SsbtekPaymentViewFields.ALLOWLIST.retainIn(answer)).isEqualTo(agencies("""
			{
			  "fk": { "formansinformation": { "utbetalningsuppgift": [], "preliminarautbetalningar": [] }, "utbetalningar": [], "preliminaraUtbetalningar": [] },
			  "so": {}
			}
			"""));
	}

	@Test
	void keepsNoAnswerOfAnAgencyThePaymentViewDoesNotRead() {
		final var answer = new HashMap<String, Map<String, Object>>();
		answer.put("af", Map.of("Svar", Map.of("Fornamn", "Test")));
		answer.put("csn", null);
		answer.put("skv", Map.of());

		assertThat(SsbtekPaymentViewFields.ALLOWLIST.retainIn(answer)).isEmpty();
	}

	static Stream<String> allowedPaths() {
		return SsbtekPaymentViewFields.PATHS.stream();
	}

	/**
	 * The answer that holds only this path's value, in the given steps. With noise every object also holds identity
	 * fields next to it, and when listed every object below the agency's answer stands as the only item of a list.
	 */
	private static Map<String, Object> nest(final List<String> steps, final Object value, final boolean noise, final boolean listed) {
		final var object = new LinkedHashMap<String, Object>();
		Object child = value;
		if (steps.size() > 1) {
			child = nest(steps.subList(1, steps.size()), value, noise, listed);
			if (listed) {
				child = List.of(child);
			}
		}
		object.put(steps.getFirst(), child);
		if (noise) {
			object.put("personnummer", APPLICANT_PERSONAL_NUMBER);
			object.put("adress", Map.of("gatuadress", "Testgatan 1"));
		}
		return object;
	}

	private static Map<String, Map<String, Object>> agencies(final String json) {
		return JSON.readValue(json, new TypeReference<Map<String, Map<String, Object>>>() {});
	}

	/** Every path to a plain value in the answers, a list not counting as a step. */
	private static Set<String> valuePaths(final Map<String, Map<String, Object>> agencies) {
		final var paths = new TreeSet<String>();
		agencies.forEach((agency, answer) -> collectValuePaths(answer, agency, paths));
		return paths;
	}

	private static void collectValuePaths(final Object node, final String path, final Set<String> paths) {
		if (node instanceof final Map<?, ?> map) {
			map.forEach((key, child) -> collectValuePaths(child, path + "." + key, paths));
		} else if (node instanceof final Iterable<?> items) {
			items.forEach(item -> collectValuePaths(item, path, paths));
		} else {
			paths.add(path);
		}
	}

	private static Set<String> keyNames(final Map<String, Map<String, Object>> agencies) {
		final var names = new TreeSet<String>();
		valuePaths(agencies).forEach(path -> names.addAll(List.of(path.split("\\."))));
		return names;
	}
}
