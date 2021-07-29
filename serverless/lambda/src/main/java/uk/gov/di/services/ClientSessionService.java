package uk.gov.di.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.entity.ClientSession;
import uk.gov.di.helpers.IdGenerator;
import uk.gov.di.helpers.ObjectMapperFactory;

public class ClientSessionService {

    private static final Logger LOG = LoggerFactory.getLogger(ClientSessionService.class);
    public static final String CLIENT_SESSION_PREFIX = "client-session-";

    private final RedisConnectionService redisConnectionService;
    private final ConfigurationService configurationService;
    private final ObjectMapper objectMapper;

    public ClientSessionService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
        this.redisConnectionService =
                new RedisConnectionService(
                        configurationService.getRedisHost(),
                        configurationService.getRedisPort(),
                        configurationService.getUseRedisTLS(),
                        configurationService.getRedisPassword());
        objectMapper = ObjectMapperFactory.getInstance();
    }

    public String generateClientSession(ClientSession clientSession) {
        String id = IdGenerator.generate();
        try {
            redisConnectionService.saveWithExpiry(
                    CLIENT_SESSION_PREFIX.concat(id),
                    objectMapper.writeValueAsString(clientSession),
                    configurationService.getSessionExpiry());
        } catch (JsonProcessingException e) {
            LOG.error("Error saving client session to Redis", e);
            throw new RuntimeException(e);
        }
        return id;
    }

    public ClientSession getClientSession(String clientSessionId) {
        try {
            String result =
                    redisConnectionService.getValue(CLIENT_SESSION_PREFIX.concat(clientSessionId));
            return objectMapper.readValue(result, ClientSession.class);
        } catch (JsonProcessingException e) {
            LOG.error("Error getting client session from Redis", e);
            throw new RuntimeException(e);
        }
    }
}
