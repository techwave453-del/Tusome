# Tusome Test Plan and Feature Traceability

## Test layers

1. **Unit tests:** pure validation, role decisions, ID mapping, progress merge rules and curriculum parsing.
2. **Repository tests:** fake HTTP/transport responses, error mapping, cache fallback and refresh behavior.
3. **Database tests:** migrations from an empty supported schema, RLS, constraints, role policies and idempotency.
4. **Edge Function tests:** valid/invalid JWTs, role/school scopes, each action, malformed payloads and audit behavior.
5. **Android instrumentation/UI tests:** sign-in, navigation, profile refresh/save, Admin workflows and learner journeys.
6. **Manual device tests:** offline/online transitions, low-memory devices, accessibility, upgrade and session revocation.
7. **Release tests:** signed build, migration rehearsal, backup restoration and rollback plan.

## Feature -> minimum test evidence

| Feature | Minimum automated coverage | Manual/release coverage |
|---|---|---|
| Sign-in/sign-up | Valid/invalid credentials, confirmation-needed response, network failure | Fresh install, sign out/in, expired session |
| Session refresh | Refresh before expiry; expired refresh token; network unavailable | Reopen app offline and online |
| Profile | Canonical profile/roles overwrite cache after refresh; write persists if edit flow is added | Save, leave screen, reopen, sign in on second device |
| Super Admin | All granted permissions visible; no permission silently omitted | Open each admin screen/action using real staging account |
| Role assignment | Escalation blocked; global role protected; revoked role denied | Revoke role while another client session is active |
| School isolation | School A cannot read/write School B records | Exercise direct REST and Edge Function requests |
| Curriculum list | Grade mapping, publication filters, empty/error states | Add/publish content and verify all screens update |
| Curriculum detail | Stable ID mapping and nested outcomes parsing | Open a newly added database strand |
| Offline catalogue | Cache hit, stale marker, cache miss | Airplane mode across app restart |
| Progress | Save/read/merge and duplicate-safe sync | Offline edits, reconnect, retry and cross-device state |
| Assessments | Valid rubric level, submission ownership, duplicate prevention | Teacher marking and learner review |
| Admin audit | Audit event for each privileged action; no secrets in details | Inspect staging audit log |
| CBC governance | Draft invisible; reviewer approval; publication visibility | Review/publish content and verify source attribution |
| Upgrade | Migration/serialization compatibility | Install over previous supported version without data loss |

## Existing test baseline

The inspected `ExampleUnitTest.kt` only checks integer addition. The inspected instrumented test only verifies the package name. These are template smoke tests and do not establish business-feature coverage.

## Definition of done

- Tests have deterministic fixtures and do not depend on production data.
- Every critical security rule has positive and negative coverage.
- Every bug fix adds a regression test that fails before the fix.
- CI runs unit tests and debug compilation on every pull request.
- Staging database tests run migrations and RLS checks before production rollout.
- Test reports are attached to releases; unverified items remain explicitly marked.
