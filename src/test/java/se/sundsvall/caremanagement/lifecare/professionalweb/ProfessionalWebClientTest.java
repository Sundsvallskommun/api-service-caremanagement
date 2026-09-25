package se.sundsvall.caremanagement.lifecare.professionalweb;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Drives the whole ProfessionalWeb transport against a fake Lifecare and identity provider: the SAML sign-in through
 * MobilityGuard's hidden fields, the session headers on a data call, both session escalations and the error mapping.
 * The fake pages are modelled on the flow captured from a real browser.
 */
class ProfessionalWebClientTest {

	private static final String API = "/WESE.FC.ProfessionalWeb/api2/Thing/Get";

	private WireMockServer wireMock;
	private ProfessionalWebClient client;
	private ProfessionalWebSession session;

	@BeforeEach
	void setUp() {
		wireMock = new WireMockServer(options().dynamicPort());
		wireMock.start();

		final var properties = properties("user", "sec ret&1", Duration.ofMinutes(20));
		final var http = new ProfessionalWebHttp(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build(), Duration.ofSeconds(5));
		session = new ProfessionalWebSession(properties, new ProfessionalWebSignIn(properties, http), http);
		client = new ProfessionalWebClient(properties, session, http);

		stubSignIn();
	}

	@AfterEach
	void tearDown() {
		wireMock.stop();
	}

	@Test
	void signsInAndSendsTheSessionOnADataCall() {
		wireMock.stubFor(get(urlPathEqualTo(API)).willReturn(okJson("{\"id\":7}")));

		final var result = client.get("api2/Thing/Get", Map.of("id", "7"));

		assertThat(result.get("id").asInt()).isEqualTo(7);
		assertThat(session.isEstablished()).isTrue();
		wireMock.verify(postRequestedFor(urlPathEqualTo("/idp/login/post"))
			.withRequestBody(containing("uid=user"))
			.withRequestBody(containing("otp=sec+ret%261"))
			.withRequestBody(containing("state=s1")));
		wireMock.verify(postRequestedFor(urlPathEqualTo("/IdentityPortalWeb/acs")).withRequestBody(containing("SAMLResponse=PHNhbWw%2B")));
		wireMock.verify(getRequestedFor(urlPathEqualTo(API))
			.withQueryParam("id", equalTo("7"))
			.withHeader("X-LEGACY-TOKEN", equalTo("tok"))
			.withHeader("X-Requested-With", equalTo("XMLHttpRequest"))
			.withHeader("Cookie", containing("LEGACY-TOKEN=tok"))
			.withHeader("Cookie", containing("metadomain=Domain")));
	}

	@Test
	void bootstrapsTheModuleWhenLifecareAsksForASession() {
		wireMock.stubFor(get(urlPathEqualTo(API)).inScenario("module").whenScenarioStateIs(STARTED)
			.willReturn(aResponse().withStatus(360)).willSetStateTo("bootstrapped"));
		wireMock.stubFor(get(urlPathEqualTo(API)).inScenario("module").whenScenarioStateIs("bootstrapped")
			.willReturn(okJson("{\"ok\":true}")));
		wireMock.stubFor(get(urlPathEqualTo("/WESE.FC.ProfessionalWeb/Heartbeat")).willReturn(ok("<script></script>")));

		assertThat(client.get("api2/Thing/Get", Map.of()).get("ok").asBoolean()).isTrue();
		wireMock.verify(1, getRequestedFor(urlPathEqualTo("/WESE.FC.ProfessionalWeb/Heartbeat")));
		wireMock.verify(1, postRequestedFor(urlPathEqualTo("/idp/login/post")));
	}

	@Test
	void signsInAgainWhenTheSessionIsGone() {
		wireMock.stubFor(post(urlPathEqualTo(API)).inScenario("dead").whenScenarioStateIs(STARTED)
			.willReturn(aResponse().withStatus(302).withHeader("Location", "/IdentityPortalWeb/login")).willSetStateTo("still"));
		wireMock.stubFor(post(urlPathEqualTo(API)).inScenario("dead").whenScenarioStateIs("still")
			.willReturn(aResponse().withStatus(200).withHeader("Content-Type", "text/html").withBody("<html>login</html>")).willSetStateTo("fresh"));
		wireMock.stubFor(post(urlPathEqualTo(API)).inScenario("dead").whenScenarioStateIs("fresh")
			.withRequestBody(equalToJson("{\"a\":1}"))
			.willReturn(okJson("{\"saved\":1}")));
		wireMock.stubFor(get(urlPathEqualTo("/WESE.FC.ProfessionalWeb/Heartbeat")).willReturn(ok()));

		assertThat(client.post("api2/Thing/Get", Map.of(), Map.of("a", 1)).get("saved").asInt()).isEqualTo(1);
		wireMock.verify(2, postRequestedFor(urlPathEqualTo("/idp/login/post")));
	}

	@Test
	void givesUpWhenAFreshSessionIsRefused() {
		wireMock.stubFor(get(urlPathEqualTo(API)).willReturn(aResponse().withStatus(401)));
		wireMock.stubFor(get(urlPathEqualTo("/WESE.FC.ProfessionalWeb/Heartbeat")).willReturn(ok()));

		assertThatThrownBy(() -> client.get("api2/Thing/Get", Map.of()))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("would not accept a freshly established session");
	}

	@Test
	void mapsARefusal() {
		wireMock.stubFor(post(urlPathEqualTo(API)).willReturn(aResponse().withStatus(461).withBody("{\"exceptionMessage\":\"Datum i framtiden\"}")));

		assertThatThrownBy(() -> client.post("api2/Thing/Get", Map.of(), Map.of()))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(422);
				assertThat(problem.getDetail()).isEqualTo("Datum i framtiden");
			});
	}

	@Test
	void deleteSendsTheIdInTheBodyAndAcceptsAnEmptyAnswer() {
		wireMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.delete(urlPathEqualTo(API))
			.withRequestBody(equalToJson("{\"id\":5}"))
			.willReturn(aResponse().withStatus(200)));

		assertThat(client.delete("api2/Thing/Get", Map.of("id", 5)).isMissingNode()).isTrue();
	}

	@Test
	void pdfIsCheckedForItsSignature() {
		wireMock.stubFor(get(urlPathEqualTo("/WESE.FC.ProfessionalWeb/RenderPdf/Print")).inScenario("pdf").whenScenarioStateIs(STARTED)
			.willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/pdf").withBody("%PDF-1.7 x")).willSetStateTo("broken"));
		wireMock.stubFor(get(urlPathEqualTo("/WESE.FC.ProfessionalWeb/RenderPdf/Print")).inScenario("pdf").whenScenarioStateIs("broken")
			.willReturn(okJson("{}")));

		assertThat(client.getPdf("RenderPdf/Print", Map.of())).startsWith((byte) '%', (byte) 'P');
		assertThatThrownBy(() -> client.getPdf("RenderPdf/Print", Map.of())).hasMessageContaining("did not answer with a PDF");
	}

	@Test
	void notJson() {
		wireMock.stubFor(get(urlPathEqualTo(API)).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("<nope")));

		assertThatThrownBy(() -> client.get("api2/Thing/Get", Map.of())).hasMessageContaining("not JSON");
	}

	@Test
	void unconfigured() {
		final var properties = new ProfessionalWebProperties(null, null, "a", "saml", null, null, null, Duration.ofMinutes(1), 1, 1, "d", "c", null);
		final var http = new ProfessionalWebHttp(HttpClient.newHttpClient(), Duration.ofSeconds(1));
		final var unconfigured = new ProfessionalWebClient(properties, new ProfessionalWebSession(properties, new ProfessionalWebSignIn(properties, http), http), http);

		assertThatThrownBy(() -> unconfigured.get("api2/x", Map.of())).hasMessageContaining("not configured");
	}

	@Test
	void sessionPastItsTtlIsReplaced() {
		final var properties = properties("user", "pw", Duration.ofMinutes(20));
		final var http = new ProfessionalWebHttp(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build(), Duration.ofSeconds(5));
		final var clock = new MutableClock(Instant.parse("2026-09-25T08:00:00Z"));
		final var timedSession = new ProfessionalWebSession(properties, new ProfessionalWebSignIn(properties, http), http, clock);

		timedSession.prepare();
		clock.now = clock.now.plus(Duration.ofMinutes(19));
		timedSession.prepare();
		wireMock.verify(1, postRequestedFor(urlPathEqualTo("/idp/login/post")));

		clock.now = clock.now.plus(Duration.ofMinutes(1));
		timedSession.prepare();
		wireMock.verify(2, postRequestedFor(urlPathEqualTo("/idp/login/post")));
	}

	@Test
	void wrongCredentialsAreReportedAsSuch() {
		wireMock.stubFor(post(urlPathEqualTo("/idp/login/post")).atPriority(1)
			.willReturn(ok(loginPage("/idp/login/retry"))));

		assertThatThrownBy(() -> client.get("api2/Thing/Get", Map.of())).hasMessageContaining("sign in a second time");
		assertThat(session.isEstablished()).isFalse();
	}

	@Test
	void waitingRoomIsReported() {
		wireMock.stubFor(post(urlPathEqualTo("/idp/login/post")).atPriority(1)
			.willReturn(ok("<form action=\"/idp/wait\"><input type=hidden name=poll value=1></form>")));
		wireMock.stubFor(post(urlPathEqualTo("/idp/wait"))
			.willReturn(ok("<form action=\"/idp/wait\"><input type=hidden name=poll value=2></form>")));

		assertThatThrownBy(() -> client.get("api2/Thing/Get", Map.of())).hasMessageContaining("waiting at /idp/wait");
	}

	@Test
	void pageWithoutPasswordIsReported() {
		wireMock.stubFor(get(urlPathEqualTo("/idp/login")).atPriority(1).willReturn(ok("<p>Ange engångskod</p>")));

		assertThatThrownBy(() -> client.get("api2/Thing/Get", Map.of())).hasMessageContaining("no password field");
	}

	@Test
	void noAccount() {
		final var properties = properties(null, null, Duration.ofMinutes(20));
		final var http = new ProfessionalWebHttp(HttpClient.newHttpClient(), Duration.ofSeconds(1));
		final var signIn = new ProfessionalWebSignIn(properties, http);

		assertThatThrownBy(() -> signIn.signIn(new ProfessionalWebCookies())).hasMessageContaining("No Lifecare account configured");
	}

	private ProfessionalWebProperties properties(final String username, final String password, final Duration ttl) {
		return new ProfessionalWebProperties(wireMock.baseUrl() + "/", "Domain", "Actor_Professional", "saml", "Sundsvall_Intra",
			username, password, ttl, 1, 5, "decision-template", "calculation-template", null);
	}

	private void stubSignIn() {
		wireMock.stubFor(get(urlPathEqualTo("/WE.Flow.Html"))
			.withQueryParam("domain", equalTo("Domain"))
			.willReturn(aResponse().withStatus(302).withHeader("Location", "/idp/login").withHeader("Set-Cookie", "ASP.NET_SessionId=s; path=/")));
		wireMock.stubFor(get(urlPathEqualTo("/idp/login")).willReturn(ok(loginPage("/idp/login/post"))));
		wireMock.stubFor(post(urlPathEqualTo("/idp/login/post")).willReturn(ok("""
			<form method="post" action="/IdentityPortalWeb/acs">
			  <textarea name="SAMLResponse">PHNhbWw+</textarea>
			</form>
			""")));
		wireMock.stubFor(post(urlPathEqualTo("/IdentityPortalWeb/acs")).willReturn(aResponse().withStatus(302)
			.withHeader("Location", "/WE.Flow.Html/")
			.withHeader("Set-Cookie", "LEGACY-TOKEN=tok; path=/; secure")));
		wireMock.stubFor(get(urlPathEqualTo("/WE.Flow.Html/")).willReturn(ok("<html>Lifecare</html>")));
	}

	private static String loginPage(final String action) {
		return """
			<form action="%s" method="post">
			  <input type="hidden" name="uid" value="">
			  <input type="hidden" name="otp" value="">
			  <input type="hidden" name="state" value="s1">
			</form>
			""".formatted(action);
	}

	private static final class MutableClock extends Clock {

		private Instant now;

		private MutableClock(final Instant now) {
			this.now = now;
		}

		@Override
		public ZoneOffset getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(final java.time.ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}
	}
}
