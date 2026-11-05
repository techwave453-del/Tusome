# Tusome CBC AI learning resources

## Why the AI-Learnings project is not copied as a separate app

The supplied AI-Learnings export is a Replit-hosted React/Express study application. Its data model is generic `fields -> topics`, it uses Replit authentication and a separate Drizzle/Postgres database, and its OpenAI credentials are supplied through Replit integration environment variables.

The reusable parts are its server-side OpenAI chat-completion pattern, JSON-only generation, outline workflow and assignment-feedback approach. Its `generateQuizForTopic` function is currently a stub. Tusome therefore keeps its existing Supabase authentication and canonical CBC hierarchy, and ports the useful generation pattern into a CBC-specific Supabase Edge Function.

## Data ownership and workflow

- `curriculum_grades`, `learning_areas`, `curriculum_strands`, `curriculum_sub_strands`, and their published learning outcomes remain the authoritative CBC source.
- `learning_resources` stores supplementary notes, examples, activities, worksheets, quizzes, assessments, revision notes, projects, teacher guides and marking schemes.
- `learning_resource_versions` preserves each saved version.
- `learning_resource_reviews` records editor decisions and feedback.
- `learning_resource_generation_jobs` records generation status and safe result metadata.

Generated material is saved as `draft`. A teacher can revise and submit it. A curriculum editor or platform administrator reviews it and may approve, reject, or request changes. Publishing is a separate action. Learners can read only published learner-facing resource types; teacher guides and marking schemes are excluded from learner RLS policies.

## Edge Functions

### `generate-learning-resource`

Accepts a signed-in teacher/editor request containing:

- `grade_id`
- `learning_area_id`
- `strand_id`
- `sub_strand_id`
- `resource_type`
- `learning_outcomes` (optional subset; every item must exactly match a published outcome)
- `language_code`
- `estimated_minutes`
- `learner_age_range`
- `difficulty`
- `local_context`
- `additional_instructions`

The function validates the role and exact curriculum path, loads official outcomes, inquiry questions and learning activities, applies a CBC-focused prompt, validates structured JSON, and stores a version-1 draft. The model never receives authority to create or rename official curriculum nodes.

### `manage-learning-resources`

Provides authenticated actions for draft creation/editing, listing the caller's resources, reading an authorised resource, submitting for review, listing the review queue, recording a review decision, publishing and withdrawing.

All database writes use the Edge Function service role. The Android app contains no OpenAI key and has read-only learner access to published resources.

## Required server configuration

Set these as Supabase Edge Function secrets; do not add them to Android resources, source control or the AI-Learnings export:

- `OPENAI_API_KEY` — required OpenAI API key.
- `OPENAI_MODEL` — optional model override; defaults to `gpt-5.1`.

The existing AI-Learnings `AI_INTEGRATIONS_OPENAI_API_KEY` and `AI_INTEGRATIONS_OPENAI_BASE_URL` are Replit-specific settings and are not assumed to exist in Tusome's Supabase project.

## Deployment checklist

The Tusome Supabase project was inactive when this integration was prepared. No database migration or Edge Function has been deployed from this branch.

After restoring the Tusome project and checking the live schema/migration history:

1. Review and apply `supabase/migrations/20261105090000_add_reviewed_learning_resources.sql`.
2. Set `OPENAI_API_KEY` (and optionally `OPENAI_MODEL`) in Supabase Edge Function secrets.
3. Deploy `manage-learning-resources` and `generate-learning-resource` with JWT verification enabled.
4. Build and test the Android app using the teacher and curriculum-editor roles.
5. Verify that generated drafts are invisible to learners, teacher-only resource types remain restricted, and only published learner resources appear under their matching sub-strands.

The migration and function source are committed to the feature branch, but deployment must wait until the Tusome project is active and its existing schema has been verified.
