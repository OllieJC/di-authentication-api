package uk.gov.di.lambdas;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.di.entity.CheckUserExistsResponse;
import uk.gov.di.entity.Session;
import uk.gov.di.services.SessionService;
import uk.gov.di.services.UserService;
import uk.gov.di.services.ValidationService;
import uk.gov.di.validation.EmailValidation;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.di.Messages.ERROR_INVALID_SESSION_ID;
import static uk.gov.di.Messages.ERROR_MISSING_REQUEST_PARAMETERS;
import static uk.gov.di.matchers.APIGatewayProxyResponseEventMatcher.hasBody;

class CheckUserExistsHandlerTest {

    private final Context context = mock(Context.class);
    private final UserService userService = mock(UserService.class);
    private final ValidationService validationService = mock(ValidationService.class);
    private final SessionService sessionService = mock(SessionService.class);
    private CheckUserExistsHandler handler;
    private String sessionId;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        handler = new CheckUserExistsHandler(validationService, userService, sessionService);
        sessionId = UUID.randomUUID().toString();
    }

    @Test
    public void shouldReturn200IfUserExists() throws JsonProcessingException {
        usingValidSession();
        when(userService.userExists(eq("joe.bloggs@digital.cabinet-office.gov.uk")))
                .thenReturn(true);
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setBody("{ \"email\": \"joe.bloggs@digital.cabinet-office.gov.uk\" }");
        event.setHeaders(Map.of("Session-Id", sessionId));
        APIGatewayProxyResponseEvent result = handler.handleRequest(event, context);

        assertEquals(200, result.getStatusCode());

        CheckUserExistsResponse checkUserExistsResponse =
                objectMapper.readValue(result.getBody(), CheckUserExistsResponse.class);
        assertEquals(
                "joe.bloggs@digital.cabinet-office.gov.uk", checkUserExistsResponse.getEmail());
        assertEquals(true, checkUserExistsResponse.doesUserExist());
    }

    @Test
    public void shouldReturn200IfUserDoesNotExist() throws JsonProcessingException {
        usingValidSession();
        when(userService.userExists(eq("joe.bloggs@digital.cabinet-office.gov.uk")))
                .thenReturn(false);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setBody("{ \"email\": \"joe.bloggs@digital.cabinet-office.gov.uk\" }");
        event.setHeaders(Map.of("Session-Id", sessionId));
        APIGatewayProxyResponseEvent result = handler.handleRequest(event, context);

        assertEquals(200, result.getStatusCode());

        CheckUserExistsResponse checkUserExistsResponse =
                objectMapper.readValue(result.getBody(), CheckUserExistsResponse.class);
        assertEquals(
                "joe.bloggs@digital.cabinet-office.gov.uk", checkUserExistsResponse.getEmail());
        assertEquals(false, checkUserExistsResponse.doesUserExist());
    }

    @Test
    public void shouldReturn400IfRequestIsMissingEmail() {
        usingValidSession();

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setBody("{ }");
        event.setHeaders(Map.of("Session-Id", sessionId));
        APIGatewayProxyResponseEvent result = handler.handleRequest(event, context);

        assertEquals(400, result.getStatusCode());
        assertThat(result, hasBody(ERROR_MISSING_REQUEST_PARAMETERS));
    }

    @Test
    public void shouldReturn400IfRequestIsMissingSessionId() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setBody("{ \"email\": \"joe.bloggs@digital.cabinet-office.gov.uk\" }");

        APIGatewayProxyResponseEvent result = handler.handleRequest(event, context);

        assertEquals(400, result.getStatusCode());
        assertThat(result, hasBody(ERROR_INVALID_SESSION_ID));
    }

    @Test
    public void shouldReturn400IfEmailAddressIsInvalid() {
        usingValidSession();
        when(validationService.validateEmailAddress(eq("joe.bloggs")))
                .thenReturn(Set.of(EmailValidation.INCORRECT_FORMAT));

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setBody("{ \"email\": \"joe.bloggs\" }");
        event.setHeaders(Map.of("Session-Id", sessionId));
        APIGatewayProxyResponseEvent result = handler.handleRequest(event, context);

        assertEquals(400, result.getStatusCode());
        assertTrue(result.getBody().contains(EmailValidation.INCORRECT_FORMAT.toString()));
    }

    private void usingValidSession() {
        when(sessionService.getSessionFromRequestHeaders(anyMap()))
                .thenReturn(Optional.of(new Session("a-session-id")));
    }
}
