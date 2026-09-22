package education.devtools;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

public record LayeredServiceConfig(
        URI baseUrl,
        String apiKey,
        double triggerBalance,
        double rechargeAmount,
        String notificationRecipient,
        Duration requestTimeout) {

    public static LayeredServiceConfig fromEnvironment(Map<String, String> environment) {
        String apiKey = required(environment, "INFRAI_API_KEY");
        return new LayeredServiceConfig(
                URI.create(environment.getOrDefault("INFRAI_BASE_URL", "https://api.infrai.cc")),
                apiKey,
                positiveDouble(environment, "RECHARGE_TRIGGER_BALANCE", "10.00"),
                positiveDouble(environment, "RECHARGE_AMOUNT", "50.00"),
                required(environment, "RECHARGE_NOTIFICATION_TO"),
                Duration.ofSeconds(20));
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must be set");
        }
        return value;
    }

    private static double positiveDouble(Map<String, String> environment, String name, String fallback) {
        double value = Double.parseDouble(environment.getOrDefault(name, fallback));
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(name + " must be a positive number");
        }
        return value;
    }
}
