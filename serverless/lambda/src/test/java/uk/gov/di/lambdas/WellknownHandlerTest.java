package uk.gov.di.lambdas;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.claims.ClaimType;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WellknownHandlerTest {
    private final Context CONTEXT = mock(Context.class);
    private final ClientContext clientContext = mock(ClientContext.class);
    private WellknownHandler handler;

    @BeforeEach
    public void setUp() {
        handler = new WellknownHandler();
        when(CONTEXT.getClientContext()).thenReturn(clientContext);
    }

    @Test
    public void shouldReturn200WhenRequestIsSuccessful() throws ParseException {
        when(CONTEXT.getClientContext().getEnvironment()).thenReturn(Map.of("BASE_URL", "http://localhost:8080/"));

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        APIGatewayProxyResponseEvent result = handler.handleRequest(event, CONTEXT);

        URI expectedRegistrationURI = URI.create("http://localhost:8080/connect/register");

        assertEquals(200, result.getStatusCode());
        assertEquals(List.of(GrantType.AUTHORIZATION_CODE), OIDCProviderMetadata.parse(result.getBody()).getGrantTypes());
        assertEquals(List.of(ClaimType.NORMAL), OIDCProviderMetadata.parse(result.getBody()).getClaimTypes());
        assertEquals(expectedRegistrationURI, OIDCProviderMetadata.parse(result.getBody()).getRegistrationEndpointURI());
    }

    @Test
    public void shouldReturn500WhenConfigIsMissing() {
        when(CONTEXT.getClientContext().getEnvironment()).thenReturn(Map.of());

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        APIGatewayProxyResponseEvent result = handler.handleRequest(event, CONTEXT);

        assertEquals(500, result.getStatusCode());
        assertEquals("Service not configured", result.getBody());
    }
}