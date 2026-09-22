package education.devtools;

import java.util.Map;

public final class BuildContinuityService {
    interface AccountGateway {
        Map<String, Object> balance();
        String sendRechargeNotice(ReleaseOperation release);
    }

    private final AccountGateway gateway;
    private final double triggerBalance;

    public BuildContinuityService(AccountGateway gateway, double triggerBalance) {
        this.gateway = gateway;
        this.triggerBalance = triggerBalance;
    }

    public BuildContinuityDecision onBuildEvent(ReleaseOperation release) {
        double balance = numericBalance(gateway.balance());
        if (balance > triggerBalance) return BuildContinuityDecision.healthy(balance);
        String messageId = gateway.sendRechargeNotice(release);
        return BuildContinuityDecision.recharged(balance, messageId);
    }

    private static double numericBalance(Map<String, Object> data) {
        Object value = data.get("balance");
        if (!(value instanceof Number number)) {
            throw new IllegalStateException("account.balance response did not include a numeric balance");
        }
        return number.doubleValue();
    }
}
