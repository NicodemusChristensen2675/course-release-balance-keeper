package education.devtools;

public record BuildContinuityDecision(State state, String diagnostic, String messageId) {
    public enum State { BALANCE_HEALTHY, RECHARGE_OBSERVED }

    public static BuildContinuityDecision healthy(double balance) {
        return new BuildContinuityDecision(State.BALANCE_HEALTHY,
                "Build may continue; available balance is " + balance, null);
    }

    public static BuildContinuityDecision recharged(double balance, String messageId) {
        return new BuildContinuityDecision(State.RECHARGE_OBSERVED,
                "Recharge observed at balance " + balance + "; build may continue", messageId);
    }
}
