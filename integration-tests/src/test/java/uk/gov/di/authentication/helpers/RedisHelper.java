package uk.gov.di.authentication.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.di.entity.Session;
import uk.gov.di.entity.SessionState;
import uk.gov.di.helpers.IdGenerator;
import uk.gov.di.services.CodeGeneratorService;
import uk.gov.di.services.CodeStorageService;
import uk.gov.di.services.RedisConnectionService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static uk.gov.di.entity.NotificationType.VERIFY_EMAIL;
import static uk.gov.di.entity.NotificationType.VERIFY_PHONE_NUMBER;

public class RedisHelper {

    private static final String REDIS_HOST =
            System.getenv().getOrDefault("REDIS_HOST", "localhost");
    private static final Optional<String> REDIS_PASSWORD =
            Optional.ofNullable(System.getenv("REDIS_PASSWORD"));

    public static String createSession(String sessionId, String clientSessionId)
            throws IOException {
        try (RedisConnectionService redis =
                new RedisConnectionService(REDIS_HOST, 6379, false, REDIS_PASSWORD)) {
            Session session = new Session(sessionId, clientSessionId);
            redis.saveWithExpiry(
                    session.getSessionId(), new ObjectMapper().writeValueAsString(session), 1800);
            return session.getSessionId();
        }
    }

    public static String createSession() throws IOException {
        return createSession(IdGenerator.generate(), IdGenerator.generate());
    }

    public static void addAuthRequestToSession(
            String clientSessionId, String sessionId, Map<String, List<String>> authRequest) {
        try (RedisConnectionService redis =
                new RedisConnectionService(REDIS_HOST, 6379, false, REDIS_PASSWORD)) {
            Session session =
                    new ObjectMapper().readValue(redis.getValue(sessionId), Session.class);
            session.addClientSessionAuthorisationRequest(clientSessionId, authRequest);
            redis.saveWithExpiry(
                    session.getSessionId(), new ObjectMapper().writeValueAsString(session), 1800);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addEmailToSession(String sessionId, String emailAddress) {
        try (RedisConnectionService redis =
                new RedisConnectionService(REDIS_HOST, 6379, false, REDIS_PASSWORD)) {
            Session session =
                    new ObjectMapper().readValue(redis.getValue(sessionId), Session.class);
            session.setEmailAddress(emailAddress);
            redis.saveWithExpiry(
                    session.getSessionId(), new ObjectMapper().writeValueAsString(session), 1800);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setSessionState(String sessionId, SessionState state) {
        try (RedisConnectionService redis =
                new RedisConnectionService(REDIS_HOST, 6379, false, REDIS_PASSWORD)) {
            Session session =
                    new ObjectMapper().readValue(redis.getValue(sessionId), Session.class);
            session.setState(state);
            redis.saveWithExpiry(
                    session.getSessionId(), new ObjectMapper().writeValueAsString(session), 1800);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String generateAndSaveEmailCode(String email, long codeExpiryTime) {
        try (RedisConnectionService redis =
                new RedisConnectionService(REDIS_HOST, 6379, false, REDIS_PASSWORD)) {

            var code = new CodeGeneratorService().sixDigitCode();
            new CodeStorageService(redis).saveOtpCode(email, code, codeExpiryTime, VERIFY_EMAIL);

            return code;
        }
    }

    public static String generateAndSavePhoneNumberCode(String email, long codeExpiryTime) {
        try (RedisConnectionService redis =
                new RedisConnectionService(REDIS_HOST, 6379, false, REDIS_PASSWORD)) {

            var code = new CodeGeneratorService().sixDigitCode();
            new CodeStorageService(redis)
                    .saveOtpCode(email, code, codeExpiryTime, VERIFY_PHONE_NUMBER);

            return code;
        }
    }

    public static void blockPhoneCode(String email, String sessionId) {
        try (RedisConnectionService redis =
                new RedisConnectionService(REDIS_HOST, 6379, false, REDIS_PASSWORD)) {

            var code = new CodeGeneratorService().sixDigitCode();
            new CodeStorageService(redis).saveCodeBlockedForSession(email, sessionId, 10);
            ;
        }
    }
}