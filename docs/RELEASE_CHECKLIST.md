# Tusome Release and Migration Checklist

## Before merging

- [ ] Confirm branch name and working tree; preserve user changes.
- [ ] Run `.\gradlew.bat testDebugUnitTest assembleDebug --no-configuration-cache`.
- [ ] Review every new permission, migration and Edge Function action.
- [ ] Add regression tests for the defect being fixed.
- [ ] Run static analysis and dependency vulnerability checks.
- [ ] Verify app does not contain service-role keys, real child data or secrets.
- [ ] Confirm screen -> repository -> data source mapping is updated.

## Supabase staging

- [ ] Apply all migrations in chronological order to a clean staging project.
- [ ] Re-run migration process to verify idempotency where expected.
- [ ] Inspect RLS enabled status and policies for every sensitive table.
- [ ] Test anonymous, learner, teacher, school admin and platform admin scopes.
- [ ] Test direct REST calls as well as Edge Function calls.
- [ ] Verify privileged operations produce audit events.
- [ ] Confirm draft CBC content remains unpublished until approved.
- [ ] Test backup restoration and document recovery targets.

## Android release

- [ ] Verify release signing and versionCode/versionName increment.
- [ ] Test fresh install, upgrade install, sign-out and expired session.
- [ ] Test profile, role changes, admin lists, Home refresh, CBC search/detail, assessment and progress.
- [ ] Test offline launch, cached content, reconnect, failed sync and retry.
- [ ] Test small screens, enlarged fonts, TalkBack and supported Android versions.
- [ ] Verify sensitive session preferences are excluded from inappropriate Android backups.
- [ ] Verify crash reporting and operational monitoring.
- [ ] Keep a tested rollback plan for both app release and database migrations.

## After release

- [ ] Monitor crash rate, authentication failures, Edge Function errors and database errors.
- [ ] Verify real users see only authorized school and learner data.
- [ ] Confirm newly published CBC content appears in all expected screens.
- [ ] Review audit logs for unexpected role changes or cross-school access attempts.
- [ ] Record build SHA, migration versions, test results and release owner.
