# Tusome Security, Authorization and Privacy Audit

**Branch:** `feature/superadmin-profile-cbc-expansion-20261016`  
**Status:** Security design baseline, not a penetration-test certificate. Live Supabase policies and deployed Edge Function configuration must be checked separately.

## Non-negotiable controls

1. Never embed a Supabase service-role key in Android. Client publishable keys are not secrets; RLS and server-side authorization must protect data.
2. Treat role visibility in Android as UX only. Every privileged Edge Function action and direct database operation must enforce authorization server-side.
3. Enforce school tenancy in database policies, not only in Android filters.
4. Protect global roles from self-assignment, self-escalation and school-admin assignment.
5. Use least privilege for Edge Function service-role operations; validate each action's payload and target scope before any privileged write.
6. Audit privileged role, school, curriculum publication and learner-data changes. Never log passwords, bearer tokens, refresh tokens or unnecessary child data.
7. Do not use real child records in sample data or screenshots. Minimize collected data and document retention, export, correction and deletion rules.
8. Keep unpublished CBC content unavailable to learners until the review and publication workflow approves it.

## Role policy target

| Operation | Platform admin | School admin | Teacher | Learner |
|---|---|---|---|---|
| Create/deactivate schools | Global | Denied | Denied | Denied |
| Assign platform/global roles | Authorized global roles only | Denied | Denied | Denied |
| Manage school users | All schools, audited | Own school only | Denied | Denied |
| View learner records | Authorized, audited | Own school | Assigned classes only | Own records |
| Record teacher assessment | As explicitly authorized | As explicitly authorized | Assigned learners only | Denied |
| Submit self-assessment | Allowed for testing/support only if explicitly scoped | Same | Own account only if also learner | Own records only |
| Edit CBC draft | Only if permission granted | Only if permission granted | Only if permission granted | Denied |
| Approve/publish CBC | Explicit publication permission | Explicit permission only | Explicit permission only | Denied |
| View global audit logs | Authorized platform role | No global access | Denied | Denied |

A platform administrator may have all application permissions as intended, but each permission must be checked by the backend. Role names and cached session fields must never be trusted as the sole authorization decision.

## Edge Function review: `manage-school-users`

Source inspection shows the function:
- Requires POST and a bearer token.
- Validates the token with Supabase Auth.
- Loads role assignments and the caller profile.
- Distinguishes a global platform administrator from a school administrator scoped to their own school.
- Uses a service-role client for privileged database operations.
- Attempts to write administrative audit events.

Required verification:
- Review every action branch, not just the initial role checks.
- Ensure each action checks target school and target user scope before service-role reads/writes.
- Ensure a school admin cannot assign protected roles or move users across schools.
- Ensure role changes cannot remove the last recovery-capable platform admin accidentally.
- Test malformed JSON, unknown actions, invalid IDs, duplicate requests, partial failure and concurrent role changes.
- Verify every privileged action emits a successful audit event or fails safely if audit logging is mandatory.
- Restrict CORS origins for browser callers where practical; CORS is not authorization.

## Data privacy and child safeguarding

Before production, document:
- Exact personal data collected and purpose for each field.
- Lawful basis, notices and guardian/child-facing explanations appropriate to the service.
- Data minimization, retention, correction, export and deletion processes.
- Who can view learner identity, attendance, assessment and guardian contact data.
- Incident response, breach escalation, backups and access review.
- Applicable Kenyan data protection obligations, validated with qualified local advice.

## Required negative security tests

- Anonymous caller cannot read protected learner or school data.
- Learner cannot invoke any admin action or modify role assignments.
- School administrator cannot read/update another school's records.
- Teacher cannot access unassigned learners.
- User cannot elevate their own role via direct REST requests or profile metadata.
- Unpublished curriculum is not returned to learner clients.
- Revoked roles stop granting access even if an old client session remains cached.
- Expired/revoked JWTs fail safely.
