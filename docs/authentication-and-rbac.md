# Tusome multi-user authentication and RBAC

## Current implementation

Tusome uses the dedicated Supabase project `wkwnoflielrotknvoflz`. Android authentication is handled through Supabase Auth REST endpoints. The app has a sign-in / registration entry screen, persists the session encrypted with an AES-GCM key held in Android Keystore, tracks access-token expiry, and attempts refresh-token rotation in the background at startup. A transient network failure preserves the encrypted session for offline access. The Profile screen displays the signed-in account and assigned role(s).

New registrations receive the `learner` role only. The app does not offer a role picker and never trusts a role sent in user metadata. Elevated roles must be assigned through a trusted administrator workflow; that workflow must not use a service-role key in the Android APK. The deployed `manage-school-users` Edge Function supports platform-admin school creation and secure user invitations. The first platform administrator must be bootstrapped manually by a trusted operator as described in [first-platform-admin-bootstrap.md](first-platform-admin-bootstrap.md).

## Roles

| Role | Scope | Intended responsibilities |
| --- | --- | --- |
| `learner` | Global identity, self-only permissions | Read published curriculum, maintain own learning progress, view available assessments |
| `teacher` | School | Support learners and record assessments for the assigned school |
| `school_admin` | School | Manage users and learning operations within the assigned school |
| `curriculum_editor` | Global | Create and review curriculum drafts and publish approved content |
| `platform_admin` | Global | Platform configuration and role governance |

Permissions are normalized in `app_permissions` and `role_permissions`. Assignments are stored in `user_role_assignments`, optionally scoped to a school. School association and grade are stored in `user_profiles`.

## Database security

- RLS is enabled on `schools`, `app_roles`, `app_permissions`, `role_permissions`, `user_profiles`, and `user_role_assignments`.
- A database trigger creates the user's profile and assigns only `learner` when Supabase Auth creates an account.
- Users can read their own profile and update only `display_name` and `grade_id`; school membership and role assignments are not client-editable.
- Users can read their own role assignments. School-level access is filtered by school scope; global governance requires the platform role.
- Role/permission lookup helpers live in the non-exposed `private` schema, use a fixed empty search path, and are executable by authenticated users only.
- The client contains only the Supabase publishable key. Never put a service-role or secret key in the APK.

## Important remaining work

This is the first secure foundation, not the completed school-management system. Next implementation steps:

1. Apply and test refreshed tokens consistently before every private API operation, and add an explicit re-authentication flow when refresh tokens are revoked or expired. The current startup refresh is best-effort so offline access is not blocked.
2. Extend the deployed `manage-school-users` Edge Function with role revocation, school membership changes, and audit logging. Never allow a client to insert its own role assignments.
3. Replace the remaining demo learner progress and assessment data with user-owned, RLS-protected tables and offline-first local storage.
4. Add role-aware navigation and screens: learner, teacher, school administrator, curriculum editor, and platform administrator.
5. Add school onboarding, account recovery, email-confirmation states, and authorization tests for cross-user and cross-school access.

## Current limits

- Registration currently creates a learner account; it does not yet assign a school or grade.
- The Profile screen reports the role and school-link status but does not yet provide a school administrator console.
- Curriculum catalogue reads remain published-content reads and do not require a signed-in role.
- Do not claim that all learner progress is cloud-synced or isolated by user until the progress repository and database policies are migrated.
