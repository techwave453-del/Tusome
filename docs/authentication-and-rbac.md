# Tusome multi-user authentication and RBAC

## Current implementation

Tusome uses the dedicated Supabase project `wkwnoflielrotknvoflz`. Android authentication is handled through Supabase Auth REST endpoints. The app persists the session encrypted with an AES-GCM key held in Android Keystore, tracks access-token expiry, and attempts refresh-token rotation. A transient network failure preserves the encrypted session so offline-capable screens remain accessible. The Profile screen displays the signed-in account and assigned roles.

The School administration screen is available to platform and school administrators. It supports school creation, school selection, user invitations, a role-aware user directory, eligible role revocation, and a recent administration audit log. All privileged operations are checked again by the `manage-school-users` Edge Function using the caller's verified Supabase user and database role assignments.

## Roles

| Role | Scope | Intended responsibilities |
| --- | --- | --- |
| `learner` | Global identity, self-only permissions | Read published curriculum, maintain own learning progress, view available assessments |
| `teacher` | School | Support learners and record assessments for the assigned school |
| `school_admin` | School | Manage users and learning operations within the assigned school |
| `curriculum_editor` | Global | Create and review curriculum drafts and publish approved content |
| `platform_admin` | Global | Platform configuration and role governance |

Permissions are normalized in `app_permissions` and `role_permissions`. Assignments are stored in `user_role_assignments`, optionally scoped to a school. School association and grade are stored in `user_profiles`.

## Administration safeguards

- Platform administrators can list/select schools, list users, invite users, and revoke eligible role assignments. The app does not expose a control for revoking `learner` or `platform_admin` roles.
- School administrators can only view users assigned to their school, invite learners and teachers to that school, and revoke teacher assignments within that school.
- The Edge Function rejects self-revocation and validates the requested role and school scope server-side.
- Role assignments cannot be changed directly from the Android client. The service-role key remains server-side.
- The `admin_audit_logs` table has RLS enabled and no grants to `anon` or `authenticated`; the Edge Function writes and reads audit records with the server-side service role.
- The user directory is capped to the latest 200 profile records per request. The audit view shows the latest 50 records.

## Database security

- RLS is enabled on `schools`, `app_roles`, `app_permissions`, `role_permissions`, `user_profiles`, `user_role_assignments`, and `admin_audit_logs`.
- A database trigger creates the user's profile and assigns only `learner` when Supabase Auth creates an account.
- Users can read their own profile and update only `display_name` and `grade_id`; school membership and role assignments are not client-editable.
- Users can read their own role assignments. School-level access is filtered by school scope; global governance requires the platform role.
- Role/permission lookup helpers live in the non-exposed `private` schema, use a fixed empty search path, and are executable by authenticated users only.
- The client contains only the Supabase publishable key. Never put a service-role or secret key in the APK.

## Remaining work

1. Replace remaining demo learner progress and assessment data with user-owned, RLS-protected tables and offline-first local storage.
2. Implement sync conflict handling and visible pending-sync status for offline learning activity.
3. Add role-aware teacher, curriculum editor, and platform administrator workflows beyond the school-management screen.
4. Add account recovery, email-confirmation states, and automated authorization tests for cross-user and cross-school access.

## Current limits

- Registration currently creates a learner account; it does not yet assign a school or grade.
- User listing returns up to 200 recent profiles per request, and the audit view returns up to 50 recent records.
- Audit logging is best-effort in the Edge Function; a failed audit insert is logged to function logs and does not roll back an already completed operation.
- Curriculum catalogue reads remain published-content reads and do not require a signed-in role.
- Do not claim that all learner progress is cloud-synced or isolated by user until the progress repository and database policies are migrated and tested.
