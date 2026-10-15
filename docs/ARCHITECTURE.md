# Tusome Architecture and Data Ownership Audit

**Audit scope:** `feature/superadmin-profile-cbc-expansion-20261016`  
**Status:** Source-based audit; runtime, deployed database and device behavior are not certified by this document.

## Current application layers

- Android entry/authentication: `ui/AuthActivity.kt`, `data/repository/SupabaseAuthRepository.kt`.
- Main navigation: `ui/MainActivity.kt`; role-gated administration entry points into `ui/AdminActivity.kt`.
- Profile: `ui/fragments/ProfileFragment.kt`; reads the encrypted local session and refreshes user/profile/role data through `SupabaseAuthRepository`.
- Home: `ui/fragments/HomeFragment.kt`; published learning-area catalogue and overview are fetched through `SupabaseCurriculumRepository`; bundled `CbcRepository` is used as a Grade 4 fallback.
- Strand catalogue/detail: `ui/fragments/StrandsFragment.kt` and `ui/StrandDetailActivity.kt` use bundled `CbcRepository` data.
- Assessment: `ui/fragments/AssessmentsFragment.kt` uses bundled `CbcRepository` rubrics and local `ActivityProgressManager` preferences.
- Competencies: `ui/fragments/CompetenciesFragment.kt` uses bundled `CbcRepository` data.
- School/user administration: `ui/AdminActivity.kt` calls `SupabaseAuthRepository.manageSchoolUsers`, which calls the `manage-school-users` Supabase Edge Function.
- Local learner progress: `data/local/ActivityProgressManager.kt` stores completion, notes and self-evaluation in SharedPreferences.

## Data ownership and source-of-truth rules

| Domain | Intended authoritative source | Current source(s) | Required direction |
|---|---|---|---|
| Authentication | Supabase Auth | Supabase Auth plus encrypted session cache | Keep Auth authoritative; treat cache as an offline snapshot |
| Profile and school/grade assignments | `user_profiles` plus role assignment tables | Server reads plus cached `AuthSession` | Define field ownership and always re-fetch canonical state after writes |
| Roles and permissions | `user_role_assignments`, permission tables, server authorization | Cached roles also control navigation visibility | UI is presentation only; server/RLS must enforce every operation |
| Published CBC | Supabase curriculum tables | Supabase on Home; bundled catalogue elsewhere | Create one repository contract for all screens and cache published records |
| Draft CBC | Supabase draft/review records | Migration-defined; live admin workflow not fully verified | Draft -> review -> approval -> publication with audit and source attribution |
| Learner progress | Authenticated learner-scoped records in database | Local SharedPreferences | Add durable remote persistence and queued offline sync |
| Assessments | Assessment/submission tables with teacher/learner ownership | Bundled rubrics and local self-evaluation | Separate rubric definitions, learner submissions and teacher-marked results |
| Administration audit | `admin_audit_logs` | Edge Function writes audit events | Make audit writes reliable and test action coverage |

## Required repository contract

Every screen should depend on a domain repository, not directly on static demo data or transport details. Repository methods should return typed success/failure states, preserve useful cached data during network failure, and expose freshness/sync status. Avoid treating request failures as empty datasets.

## Known consistency defects

1. Home's featured action previously used the literal `agriculture_nutrition`; the handler now resolves an available published learning area for the selected grade.
2. Home reads live curriculum, while Strands, Strand Detail, Assessments and Competencies still use bundled data.
3. `ActivityProgressManager` is device-local and does not provide cross-device sync.
4. `CbcRepository.learnerProfile` contains example personal data; production UI must not present it as a real signed-in learner.
5. Some curriculum count-fetch errors are converted to empty results, which can make failed counts look like zero.
6. Profile currently displays refreshed data but the inspected fragment does not provide a profile-edit/save flow.

## Definition of done for architecture alignment

- All learning screens read the same published curriculum IDs and repository interface.
- Every learner record is scoped to the authenticated user and school on the server.
- Writes return canonical saved records and update local state only after success (or explicitly mark pending offline writes).
- All local cached data has owner, version, freshness and synchronization semantics.
- No sample personal data is used as a real learner account.
