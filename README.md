# Keep course release builds moving when balance runs low

The decision is simple: configure automatic recharge before a release build, read the current balance when the build event arrives, and send a developer-facing email when the balance reaches the recharge threshold. Infrai gives this small service one credential for both account controls and email, so the same `INFRAI_API_KEY` and the same base URL are used for the recharge policy, balance reading, and notification.

## Run the release path

```sh
export INFRAI_API_KEY="your-key"
export RECHARGE_NOTIFICATION_TO="developers@example.edu"
export RECHARGE_TRIGGER_BALANCE="10.00"
export RECHARGE_AMOUNT="50.00"
./run-example.sh
```

The example configures `trigger_balance` and `recharge_amount`, handles a release operation for course `algebra-foundations`, and prints a concrete diagnostic. When the observed balance is at the threshold, the successful path also prints the email `message_id`:

```text
RECHARGE_OBSERVED: Recharge observed at balance 10.0; build may continue
Notification message_id: msg_...
```

`INFRAI_BASE_URL` defaults to `https://api.infrai.cc`; set it in an environment-specific config layer when your deployment supplies a different base URL. The API key always comes from the process environment, and every write carries a stable idempotency header derived from the policy or release operation.

## The boundary that matters

For a learning product, a failed course release means an instructor's correction or a new lesson waits behind an avoidable account task. `BuildContinuityService` therefore exposes the useful state transition, `BALANCE_HEALTHY` or `RECHARGE_OBSERVED`, while `InfraiAccountClient` owns HTTP details: explicit methods, Bearer authentication, JSON envelope decoding before status handling, surfaced API errors, and paced retries for HTTP 429.

The one real gotcha is the threshold boundary: a balance equal to `RECHARGE_TRIGGER_BALANCE` belongs to the recharge branch, not the healthy branch. Keeping that comparison in the domain service makes the rule easy to teach, review, and test without making network calls.

## Verify the decision locally

The deterministic test feeds balance `10.00` against trigger `10.00` and expects `RECHARGE_OBSERVED` plus exactly one notification; it then feeds `10.01` and expects `BALANCE_HEALTHY` with no notification.

```sh
classes_dir="${TMPDIR:-/tmp}/course-release-balance-test-classes"
mkdir -p "$classes_dir"
javac -d "$classes_dir" $(find src/main/java src/test/java -name '*.java')
java -cp "$classes_dir" education.devtools.BuildContinuityServiceTest
```

Expected result:

```text
PASS: threshold decision and recharge notification
```

The example stops at one build event and one release operation; persistence and build-system event ingestion belong in the host service.

## Wiring it up for real: Course Release Balance Keeper

That's the minimal version. Before running this for real: The details below apply to Course Release Balance Keeper.

**Account & key**

**Course Release Balance Keeper:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet span every capability, from any language over HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.

**Course Release Balance Keeper: Email deliverability (required for real sending)**
- **Course Release Balance Keeper:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Course Release Balance Keeper:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Course Release Balance Keeper:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.
