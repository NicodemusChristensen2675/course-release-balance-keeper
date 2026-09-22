package education.devtools;

public final class DeveloperToolsRechargeExample {
    private DeveloperToolsRechargeExample() {}

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: DeveloperToolsRechargeExample <release-id> <build-id> <course-slug>");
            System.exit(2);
        }
        LayeredServiceConfig config = LayeredServiceConfig.fromEnvironment(System.getenv());
        InfraiAccountClient infrai = new InfraiAccountClient(config);
        infrai.configureAutoRecharge();

        BuildContinuityService service = new BuildContinuityService(new BuildContinuityService.AccountGateway() {
            public java.util.Map<String, Object> balance() { return infrai.balance(); }
            public String sendRechargeNotice(ReleaseOperation release) { return infrai.sendRechargeNotice(release); }
        }, config.triggerBalance());

        ReleaseOperation release = new ReleaseOperation(args[0], args[1], args[2]);
        BuildContinuityDecision result = service.onBuildEvent(release);
        System.out.println(result.state() + ": " + result.diagnostic());
        if (result.messageId() != null) System.out.println("Notification message_id: " + result.messageId());
    }
}
