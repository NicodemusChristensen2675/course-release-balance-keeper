package education.devtools;

import java.util.Map;

public final class BuildContinuityServiceTest {
    public static void main(String[] args) {
        ReleaseOperation release = new ReleaseOperation("release-42", "build-108", "algebra-foundations");
        RecordingGateway low = new RecordingGateway(10.0);
        BuildContinuityDecision recharged = new BuildContinuityService(low, 10.0).onBuildEvent(release);
        check(recharged.state() == BuildContinuityDecision.State.RECHARGE_OBSERVED,
                "balance at the threshold should observe recharge");
        check(low.notices == 1, "the recharge should send exactly one notice");
        check("message-test-1".equals(recharged.messageId()), "the diagnostic should carry message_id");

        RecordingGateway healthy = new RecordingGateway(10.01);
        BuildContinuityDecision continued = new BuildContinuityService(healthy, 10.0).onBuildEvent(release);
        check(continued.state() == BuildContinuityDecision.State.BALANCE_HEALTHY,
                "balance above the threshold should remain healthy");
        check(healthy.notices == 0, "a healthy balance should not send a recharge notice");
        System.out.println("PASS: threshold decision and recharge notification");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class RecordingGateway implements BuildContinuityService.AccountGateway {
        private final double balance;
        private int notices;
        RecordingGateway(double balance) { this.balance = balance; }
        public Map<String, Object> balance() { return Map.of("balance", balance); }
        public String sendRechargeNotice(ReleaseOperation release) {
            notices++;
            return "message-test-" + notices;
        }
    }
}
