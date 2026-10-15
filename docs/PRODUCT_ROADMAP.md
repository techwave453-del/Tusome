# Tusome Product Roadmap

This roadmap prioritizes correctness, security and data consistency before expanding feature count.

## Phase 0 — Establish a reliable baseline

- [ ] Record exact branch, commit SHA and working-tree state.
- [ ] Inventory all activities, fragments, dialogs, navigation destinations, adapters, repositories, models, migrations and Edge Functions.
- [ ] Build the exact branch and resolve compiler errors one at a time.
- [ ] Create screen/repository/data-source and feature/test traceability.
- [ ] Remove real-looking learner personal data from production UI paths.

## Phase 1 — Security and identity

- [ ] Complete operation-by-operation authorization review of every Edge Function action.
- [ ] Verify RLS and school tenancy for every sensitive table.
- [ ] Define authoritative profile fields and role-assignment lifecycle.
- [ ] Implement a profile save workflow only after confirming schema, validation and write policy.
- [ ] Re-fetch canonical profile, roles and school after successful writes.
- [ ] Ensure Super Admin can reach every authorized feature while server-side rules remain authoritative.
- [ ] Add negative tests for escalation, cross-school access and revoked roles.

## Phase 2 — Unified curriculum

- [ ] Establish a shared curriculum repository contract for Home, Strands, Strand Detail, Assessments and Competencies.
- [ ] Standardize stable IDs across database and offline content.
- [ ] Replace generic demo labels with actual curriculum titles.
- [ ] Make count errors observable; do not translate failed requests into zero.
- [ ] Add local cache for all supported grades and published content.
- [ ] Keep new CBC additions in draft/review until verified against authoritative KICD curriculum designs.
- [ ] Record source, grade, revision, reviewer and publication date for curriculum content.

## Phase 3 — Learner progress and assessment

- [ ] Separate rubric definitions, learner self-evaluations, submissions and teacher-marked results.
- [ ] Persist progress under authenticated user and school ownership.
- [ ] Implement offline write queue, idempotency keys, retry and conflict resolution.
- [ ] Add progress history and account/device synchronization.
- [ ] Verify teacher access is restricted to assigned learners/classes.

## Phase 4 — Operational readiness

- [ ] Add unit, repository, Edge Function, RLS and UI regression tests.
- [ ] Add CI build/test workflow and release artifacts.
- [ ] Document backups, restore drills, monitoring and incident response.
- [ ] Complete privacy notices, retention/deletion and child-safeguarding procedures.
- [ ] Test accessibility, low-data use, low-end devices and language requirements.
- [ ] Rehearse staged deployment and rollback.

## Phase 5 — Product expansion

Only after the foundation is stable, expand parent/guardian features, teacher classroom tools, curriculum review workflows, analytics, content import/export and personalized learning. Every feature needs an owner, source-of-truth definition, authorization rule, offline behavior and regression test before release.
