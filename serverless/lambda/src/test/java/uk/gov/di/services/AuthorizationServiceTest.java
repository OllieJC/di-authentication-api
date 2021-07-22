package uk.gov.di.services;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.di.entity.ClientRegistry;
import uk.gov.di.exceptions.ClientNotFoundException;

import java.net.URI;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationServiceTest {

    private static final URI REDIRECT_URI = URI.create("http://localhost/redirect");
    private AuthorizationService authorizationService;
    private final DynamoClientService dynamoClientService = mock(DynamoClientService.class);

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService(dynamoClientService);
    }

    @Test
    void shouldThrowClientNotFoundExceptionWhenClientDoesNotExist() {
        ClientID clientID = new ClientID();
        when(dynamoClientService.getClient(clientID.toString())).thenReturn(Optional.empty());

        ClientNotFoundException exception =
                assertThrows(
                        ClientNotFoundException.class,
                        () -> authorizationService.isClientRedirectUriValid(clientID, REDIRECT_URI),
                        "Expected to throw exception");

        assertThat(
                exception.getMessage(),
                equalTo(format("No Client found for ClientID: %s", clientID)));
    }

    @Test
    void shouldReturnFalseIfClientUriIsInvalid() throws ClientNotFoundException {
        ClientID clientID = new ClientID();
        ClientRegistry clientRegistry =
                generateClientRegistry("http://localhost//", clientID.toString());
        when(dynamoClientService.getClient(clientID.toString()))
                .thenReturn(Optional.of(clientRegistry));
        assertFalse(authorizationService.isClientRedirectUriValid(clientID, REDIRECT_URI));
    }

    @Test
    void shouldReturnTrueIfRedirectUriIsValid() throws ClientNotFoundException {
        ClientID clientID = new ClientID();
        ClientRegistry clientRegistry =
                generateClientRegistry(REDIRECT_URI.toString(), clientID.toString());
        when(dynamoClientService.getClient(clientID.toString()))
                .thenReturn(Optional.of(clientRegistry));
        assertTrue(authorizationService.isClientRedirectUriValid(clientID, REDIRECT_URI));
    }

    @Test
    void shouldGenerateSuccessfulAuthResponse() {
        ClientID clientID = new ClientID();
        AuthorizationCode authCode = new AuthorizationCode();
        State state = new State();
        ResponseType responseType = new ResponseType(ResponseType.Value.CODE);
        AuthorizationRequest authorizationRequest =
                new AuthorizationRequest.Builder(responseType, clientID)
                        .redirectionURI(REDIRECT_URI)
                        .state(state)
                        .build();

        AuthenticationSuccessResponse authSuccessResponse =
                authorizationService.generateSuccessfulAuthResponse(authorizationRequest, authCode);
        assertThat(authSuccessResponse.getState(), equalTo(state));
        assertThat(authSuccessResponse.getAuthorizationCode(), equalTo(authCode));
        assertThat(authSuccessResponse.getRedirectionURI(), equalTo(REDIRECT_URI));
    }

    private ClientRegistry generateClientRegistry(String redirectURI, String clientID) {
        return new ClientRegistry()
                .setRedirectUrls(singletonList(redirectURI))
                .setClientID(clientID)
                .setContacts(singletonList("joe.bloggs@digital.cabinet-office.gov.uk"))
                .setPublicKey(null)
                .setScopes(singletonList("openid"));
    }
}