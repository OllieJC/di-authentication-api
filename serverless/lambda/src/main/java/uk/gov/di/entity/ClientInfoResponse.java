package uk.gov.di.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class ClientInfoResponse {

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("client_name")
    private String clientName;

    @JsonProperty("scopes")
    private List<String> scopes = new ArrayList<>();

    @JsonProperty("redirectUri")
    private String redirectUri;

    @JsonProperty("serviceType")
    private String serviceType;

    public ClientInfoResponse(
            @JsonProperty(required = true, value = "client_id") String clientId,
            @JsonProperty(required = true, value = "client_name") String clientName,
            @JsonProperty(required = true, value = "scopes") List<String> scopes,
            @JsonProperty(required = false, value = "redirectUri") String redirectUri,
            @JsonProperty(required = true, value = "serviceType") String serviceType) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.scopes = scopes;
        this.redirectUri = redirectUri;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getServiceType() {
        return serviceType;
    }
}