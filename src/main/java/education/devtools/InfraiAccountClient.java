package education.devtools;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InfraiAccountClient {
    private final HttpClient http;
    private final LayeredServiceConfig config;

    public InfraiAccountClient(LayeredServiceConfig config) {
        this(HttpClient.newBuilder().connectTimeout(config.requestTimeout()).build(), config);
    }

    InfraiAccountClient(HttpClient http, LayeredServiceConfig config) {
        this.http = http;
        this.config = config;
    }

    public Map<String, Object> configureAutoRecharge() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("trigger_balance", config.triggerBalance());
        body.put("recharge_amount", config.rechargeAmount());
        return request("PUT", "/v1/account/autorecharge/configure", body, "autorecharge-policy");
    }

    public Map<String, Object> balance() {
        return request("GET", "/v1/account/balance", null, null);
    }

    public String sendRechargeNotice(ReleaseOperation release) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("to", config.notificationRecipient());
        body.put("subject", "Build balance recharged for " + release.courseSlug());
        body.put("body", "Release " + release.releaseId() + " can continue. Recharge amount: "
                + config.rechargeAmount() + ". Build: " + release.buildId());
        Map<String, Object> data = request("POST", "/v1/email/send", body, "recharge-" + release.releaseId());
        Object messageId = data.get("message_id");
        if (!(messageId instanceof String id) || id.isBlank()) {
            throw new IllegalStateException("email.send response did not include message_id");
        }
        return id;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> request(String method, String path, Map<String, Object> body, String idempotencyKey) {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(config.baseUrl().resolve(path))
                    .timeout(config.requestTimeout())
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Accept", "application/json");
            if (idempotencyKey != null) builder.header("Idempotency-Key", idempotencyKey);
            if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
            else builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(JsonDocument.write(body)));

            HttpResponse<String> response;
            try {
                response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            } catch (IOException error) {
                throw new IllegalStateException("Could not reach Infrai", error);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Infrai request interrupted", error);
            }

            Object decoded = JsonDocument.parse(response.body());
            if (!(decoded instanceof Map<?, ?> rawEnvelope)) {
                throw new IllegalStateException("Infrai returned a non-object envelope");
            }
            Map<String, Object> envelope = (Map<String, Object>) rawEnvelope;
            if (response.statusCode() == 429 && attempt < 3) {
                pause(retryDelay(response, attempt));
                continue;
            }
            if (!Boolean.TRUE.equals(envelope.get("ok"))) {
                Object rawError = envelope.get("error");
                Map<String, Object> error = rawError instanceof Map<?, ?> map
                        ? (Map<String, Object>) map : Map.of();
                String code = String.valueOf(error.getOrDefault("code", "INFRAI_REQUEST_REJECTED"));
                String message = String.valueOf(error.getOrDefault("message", "Infrai rejected the request"));
                throw new InfraiException(code, message, response.statusCode());
            }
            Object data = envelope.get("data");
            return data instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
        }
        throw new IllegalStateException("Retry attempts exhausted");
    }

    private static Duration retryDelay(HttpResponse<?> response, int attempt) {
        String value = response.headers().firstValue("Retry-After").orElse("");
        try { return Duration.ofSeconds(Math.max(1, Long.parseLong(value))); }
        catch (NumberFormatException ignored) { return Duration.ofMillis(250L * (1L << attempt)); }
    }

    private static void pause(Duration delay) {
        try { Thread.sleep(delay.toMillis()); }
        catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", error);
        }
    }
}
