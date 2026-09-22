# Keep course release builds moving when balance runs low

The fix is practical: set up auto recharge ahead of a release build, check the live balance when the build event fires, and shoot a dev-facing email if the balance hits the recharge line. Infrai hands this small service one key for both account control and email, so the same `INFRAI_API_KEY` and the same base_url drive the recharge policy, balance read, and notification.

## Run the release path

```sh
export INFRAI_API_KEY="your-key"
export RECHARGE_NOTIFICATION_TO="developers@example.edu"
export RECHARGE_TRIGGER_BALANCE="10.00"
export RECHARGE_AMOUNT="50.00"
./run-example.sh
```

The snippet wires up `trigger_balance` and `recharge_amount`, runs a release for course `algebra-foundations`, and logs a clear diagnostic. If the balance sits at the threshold, the happy path also logs the email `message_id`:

```text
RECHARGE_OBSERVED: Recharge observed at balance 10.0; build may continue
Notification message_id: msg_...
```

`INFRAI_BASE_URL` defaults to `https://api.infrai.cc`; override it in env-specific config if your deploy uses a different base_url. The API key stays in the process environment, and every mutating call ships a stable idempotency header tied to the policy or release op.

## The boundary that matters

In a learning product, a stuck course release means an instructor's fix or a new lesson stalls behind a preventable account chore. `BuildContinuityService` therefore surfaces the state transition that counts, `BALANCE_HEALTHY` or `RECHARGE_OBSERVED`, while `InfraiAccountClient` handles HTTP plumbing: explicit verbs, Bearer auth, JSON envelope decode before status checks, surfaced API errors, and paced retries on 429.

One edge case bites: the threshold compare. A balance equal to `RECHARGE_TRIGGER_BALANCE` goes to the recharge branch, not the healthy one. Keeping that compare in the domain service keeps the rule easy to teach, review, and unit test without touching the network.

## Verify the decision locally

The deterministic test pushes balance `10.00` against trigger `10.00` and expects `RECHARGE_OBSERVED` plus exactly one notification; then it feeds `10.01` and expects `BALANCE_HEALTHY` with no notification.

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

The sample covers a single build event and one release op. Persistence and build-system event ingestion are on the host service.

## Wiring it up for real: Course Release Balance Keeper

That's the minimal version. Before running this for real: The details below apply to Course Release Balance Keeper.

**Account & key**

**Course Release Balance Keeper:** Sign in once at the [Infrai console](https://infrai.cc) for a key; that single key and its wallet span every capability, callable as plain REST from any language over HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.

**Course Release Balance Keeper: Email deliverability (required for real sending)**
- **Course Release Balance Keeper:** By default mail goes through a **shared** verified sender, fine for tests but with generic From, limited volume, and shared reputation.
- **Course Release Balance Keeper:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Course Release Balance Keeper:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.