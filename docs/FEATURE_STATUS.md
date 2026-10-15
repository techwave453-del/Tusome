# Tusome Feature Status and Traceability

**Branch:** `feature/superadmin-profile-cbc-expansion-20261016`  
**Evidence level:** File-level source inspection. “Implemented” means source exists, not that the live feature has passed an end-to-end test.

## Screen -> repository -> data source

| Screen / component | Repository or service | Data source | Status / gap |
|---|---|---|---|
| `AuthActivity` | `SupabaseAuthRepository` | Supabase Auth; encrypted local session | Sign-in/sign-up path exists; expiry, recovery and end-to-end tests needed |
| `MainActivity` | `SupabaseAuthRepository` | Cached session role list | Role-gated navigation exists; server must independently authorize operations |
| `AdminActivity` | `SupabaseAuthRepository.manageSchoolUsers` | `manage-school-users` Edge Function; admin tables | School/user management UI exists; verify each action and post-write refresh |
| `HomeFragment` | `SupabaseCurriculumRepository`, `CbcRepository`, `SupabaseAuthRepository` | Live published CBC plus bundled Grade 4 fallback | Featured activity now resolves from the selected grade's published catalogue; fallback data is not a complete offline cache |
| `ProfileFragment` | `SupabaseAuthRepository` | Encrypted session and Supabase profile/role refresh | Read/refresh exists; no edit/save form in inspected fragment |
| `StrandsFragment` | `CbcRepository` | Bundled Kotlin sample catalogue | Not unified with live Supabase curriculum |
| `StrandDetailActivity` | `CbcRepository`, `ActivityProgressManager` | Bundled strand data and local preferences | Live strand IDs may not resolve in bundled catalogue |
| `AssessmentsFragment` | `CbcRepository`, `ActivityProgressManager` | Bundled rubrics and local preferences | No demonstrated remote submission or cross-device sync |
| `CompetenciesFragment` | `CbcRepository` | Bundled competencies | Database-driven updates not established |
| `RubricAdapter` | Adapter/model | `AssessmentRubric` enriched from bundled sub-strand data | Displays actual sub-strand title, inquiry questions and suggested experiences when available; untouched evaluations are labelled “Not evaluated” |

## Data-source inventory

- Supabase Auth: account identity and tokens.
- `user_profiles`: display name, grade and school profile fields are read during session refresh.
- `user_role_assignments` and permission tables: role/permission assignments; migration seeds platform-admin permissions.
- `schools`, invitations/users and `admin_audit_logs`: school administration Edge Function.
- Published curriculum: `grade_learning_areas`, `learning_areas`, `curriculum_strands`, `curriculum_sub_strands`, `learning_outcomes`, plus related curriculum tables.
- Local session cache: encrypted SharedPreferences in `SupabaseAuthRepository`.
- Local progress: SharedPreferences in `ActivityProgressManager`.
- Bundled content: `CbcRepository`, including sample Grade 4 learning areas, strands, competencies, rubrics and a sample learner profile.

## Critical known gaps

- Profile edit/save workflow is absent from the inspected Profile screen.
- Live curriculum and bundled curriculum are separate sources.
- Local progress is not account-synchronized.
- A sample learner profile includes identifying-looking example values; remove from production paths or clearly isolate as demo fixtures.
- Home count errors must not be silently represented as zero.
- Added `CurriculumSelectionTest` for preferred/fallback/empty featured-area selection. Other important workflows still need coverage.
- Added `.github/workflows/android-ci.yml` to run unit tests and assemble a debug APK on branch pushes and pull requests; CI has not yet reported a result for these changes.
- Migration application and live RLS behavior have not been verified by this source inspection.

## Status labels

- **Implemented in source:** relevant code path exists.
- **Partial:** UI or one side of the workflow exists, but the end-to-end contract is incomplete.
- **Unverified:** requires live DB, device, or deployment test.
- **Blocked:** dependent on schema, policy, or product decision not yet verified.

Update this file whenever a feature is added, fixed or its test status changes.
