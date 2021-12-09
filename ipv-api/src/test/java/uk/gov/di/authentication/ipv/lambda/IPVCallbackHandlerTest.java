package uk.gov.di.authentication.ipv.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.id.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.di.authentication.ipv.services.AuthorisationResponseService;
import uk.gov.di.authentication.shared.services.ConfigurationService;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.di.authentication.sharedtest.matchers.APIGatewayProxyResponseEventMatcher.hasStatus;

class IPVCallbackHandlerTest {

    private final Context context = mock(Context.class);
    private final ConfigurationService configService = mock(ConfigurationService.class);
    private final AuthorisationResponseService responseService =
            mock(AuthorisationResponseService.class);
    private static final URI LOGIN_URL = URI.create("https://example.com");
    private static final AuthorizationCode AUTH_CODE = new AuthorizationCode();
    private static final State STATE = new State();
    private static final URI REDIRECT_URL = URI.create("https://redirect.com");
    private IPVCallbackHandler handler;

    @BeforeEach
    void setUp() {
        handler = new IPVCallbackHandler(configService);
        when(configService.getLoginURI()).thenReturn(LOGIN_URL);
    }

    @Test
    void shouldRedirectToLoginUriForSuccessfulResponse() {
        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("code", AUTH_CODE.getValue());
        responseHeaders.put("state", STATE.getValue());
        when(responseService.validateResponse(responseHeaders)).thenReturn(Optional.empty());

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setQueryStringParameters(responseHeaders);
        APIGatewayProxyResponseEvent response = makeHandlerRequest(event);

        assertThat(response, hasStatus(302));
        assertThat(response.getHeaders().get("Location"), equalTo(LOGIN_URL.toString()));
    }

    @Test
    void shouldRedirectToLoginUriWhenResponseContainsError() {
        ErrorObject errorObject =
                new ErrorObject(
                        "invalid_request_redirect_uri", "redirect_uri param must be provided");
        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("code", AUTH_CODE.getValue());
        responseHeaders.put("state", STATE.getValue());
        responseHeaders.put("error", errorObject.toString());

        when(responseService.validateResponse(responseHeaders))
                .thenReturn(Optional.of(new ErrorObject(errorObject.getCode())));

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setQueryStringParameters(responseHeaders);
        APIGatewayProxyResponseEvent response = makeHandlerRequest(event);

        assertThat(response, hasStatus(302));
        assertThat(response.getHeaders().get("Location"), equalTo(LOGIN_URL.toString()));
    }

    private APIGatewayProxyResponseEvent makeHandlerRequest(APIGatewayProxyRequestEvent event) {
        return handler.handleRequest(event, context);
    }
}