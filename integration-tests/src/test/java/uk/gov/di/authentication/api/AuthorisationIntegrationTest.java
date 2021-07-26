package uk.gov.di.authentication.api;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.di.authentication.helpers.DynamoHelper;
import uk.gov.di.authentication.helpers.RedisHelper;
import uk.gov.di.entity.SessionState;
import uk.gov.di.services.ConfigurationService;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

import static com.nimbusds.openid.connect.sdk.Prompt.Type.NONE;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AuthorisationIntegrationTest extends IntegrationTestEndpoints {

    private static final String AUTHORIZE_ENDPOINT = "/authorize";

    private static final String CLIENT_ID = "test-client";
    private static final String INVALID_CLIENT_ID = "invalid-test-client";
    private static final KeyPair KEY_PAIR = generateRsaKeyPair();

    private static final ConfigurationService configurationService = new ConfigurationService();

    @BeforeAll
    public static void setup() {
        DynamoHelper.registerClient(
                CLIENT_ID,
                "test-client",
                singletonList("localhost"),
                singletonList("joe.bloggs@digital.cabinet-office.gov.uk"),
                singletonList("openid"),
                Base64.getMimeEncoder().encodeToString(KEY_PAIR.getPublic().getEncoded()),
                singletonList("http://localhost/post-redirect-logout"));
    }

    @Test
    public void shouldReturnUnmetAuthenticationRequirementsErrorWhenUsingInvalidClient() {
        Response response =
                doAuthorisationRequest(
                        Optional.of(INVALID_CLIENT_ID), Optional.empty(), Optional.empty());
        assertEquals(302, response.getStatus());
        assertThat(
                getHeaderValueByParamName(response, "Location"),
                containsString("error=unmet_authentication_requirements"));
    }

    @Test
    public void shouldRedirectToLoginWhenNoCookie() {
        Response response =
                doAuthorisationRequest(Optional.of(CLIENT_ID), Optional.empty(), Optional.empty());

        assertEquals(302, response.getStatus());
        assertThat(
                getHeaderValueByParamName(response, "Location"),
                startsWith(configurationService.getLoginURI().toString()));
        assertNotNull(response.getCookies().get("gs"));
    }

    @Test
    public void shouldRedirectToLoginWhenBadCookie() {
        Response response =
                doAuthorisationRequest(
                        Optional.of(CLIENT_ID),
                        Optional.of(new Cookie("gs", "this is bad")),
                        Optional.empty());

        assertEquals(302, response.getStatus());
        assertThat(
                getHeaderValueByParamName(response, "Location"),
                startsWith(configurationService.getLoginURI().toString()));
        assertNotNull(response.getCookies().get("gs"));
    }

    @Test
    public void shouldRedirectToLoginWhenCookieHasUnknownSessionId() {
        Response response =
                doAuthorisationRequest(
                        Optional.of(CLIENT_ID),
                        Optional.of(new Cookie("gs", "123.456")),
                        Optional.empty());

        assertEquals(302, response.getStatus());
        assertThat(
                getHeaderValueByParamName(response, "Location"),
                startsWith(configurationService.getLoginURI().toString()));
        assertNotNull(response.getCookies().get("gs"));
    }

    @Test
    public void shouldRedirectToLoginWhenSessionFromCookieIsNotAuthenticated() throws IOException {
        String sessionId = RedisHelper.createSession();
        RedisHelper.setSessionState(sessionId, SessionState.AUTHENTICATION_REQUIRED);

        Response response =
                doAuthorisationRequest(
                        Optional.of(CLIENT_ID),
                        Optional.of(new Cookie("gs", format("%s.456", sessionId))),
                        Optional.empty());

        assertEquals(302, response.getStatus());
        assertThat(
                getHeaderValueByParamName(response, "Location"),
                startsWith(configurationService.getLoginURI().toString()));
        assertNotNull(response.getCookies().get("gs"));
        assertThat(response.getCookies().get("gs").getValue(), not(startsWith(sessionId)));
    }

    @Test
    public void shouldIssueAuthorisationCodeWhenSessionFromCookieIsAuthenticated()
            throws IOException {
        String sessionId = RedisHelper.createSession();
        RedisHelper.setSessionState(sessionId, SessionState.AUTHENTICATED);

        Response response =
                doAuthorisationRequest(
                        Optional.of(CLIENT_ID),
                        Optional.of(new Cookie("gs", format("%s.456", sessionId))),
                        Optional.empty());

        assertEquals(302, response.getStatus());
        // TODO: Update assertions to reflect code issuance, once we've written that code
        assertNotNull(response.getCookies().get("gs"));
        assertThat(response.getCookies().get("gs").getValue(), not(startsWith(sessionId)));
    }

    @Test
    public void shouldReturnLoginRequiredErrorWhenPromptNoneAndUserUnauthenticated() {
        Response response =
                doAuthorisationRequest(
                        Optional.of(CLIENT_ID), Optional.empty(), Optional.of(NONE.toString()));
        assertEquals(302, response.getStatus());
        assertThat(
                getHeaderValueByParamName(response, "Location"),
                containsString("error=login_required"));
    }

    @Test
    public void shouldNotPromptForLoginWhenPromptNoneAndUserAuthenticated() throws Exception {
        String sessionId = RedisHelper.createSession();
        RedisHelper.setSessionState(sessionId, SessionState.AUTHENTICATED);

        Response response =
                doAuthorisationRequest(
                        Optional.of(CLIENT_ID),
                        Optional.of(new Cookie("gs", format("%s.456", sessionId))),
                        Optional.of(NONE.toString()));

        assertEquals(302, response.getStatus());
        assertNotNull(response.getCookies().get("gs"));
        assertThat(response.getCookies().get("gs").getValue(), not(startsWith(sessionId)));
        assertThat(
                getHeaderValueByParamName(response, "Location"),
                not(startsWith(configurationService.getLoginURI().toString() + "?id=")));
    }

    private static KeyPair generateRsaKeyPair() {
        KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException();
        }
        kpg.initialize(2048);
        return kpg.generateKeyPair();
    }

    private Response doAuthorisationRequest(
            Optional<String> clientId, Optional<Cookie> cookie, Optional<String> prompt) {
        Client client = ClientBuilder.newClient();

        WebTarget webTarget =
                client.target(ROOT_RESOURCE_URL + AUTHORIZE_ENDPOINT)
                        .queryParam("response_type", "code")
                        .queryParam("redirect_uri", "localhost")
                        .queryParam("state", "8VAVNSxHO1HwiNDhwchQKdd7eOUK3ltKfQzwPDxu9LU")
                        .queryParam("client_id", clientId.orElse("test-client"))
                        .queryParam("scope", "openid")
                        .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE);
        if (prompt.isPresent()) {
            webTarget = webTarget.queryParam("prompt", prompt.get());
        }

        Invocation.Builder builder = webTarget.request();
        cookie.ifPresent(builder::cookie);
        return builder.get();
    }

    private String getHeaderValueByParamName(Response response, String paramName) {
        return response.getHeaders().get(paramName).get(0).toString();
    }
}
