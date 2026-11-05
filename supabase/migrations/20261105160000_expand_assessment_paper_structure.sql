-- Extend assessment papers to represent the exam structures in the supplied Grade 10 revision guide.
-- The same flexible format can be used for age-appropriate assessment designs at every CBC level.

alter table public.assessment_papers
  add column if not exists academic_year integer,
  add column if not exists term_number smallint check (term_number between 1 and 3),
  add column if not exists trial_number smallint check (trial_number between 1 and 20),
  add column if not exists paper_number text not null default '',
  add column if not exists pathway text not null default '',
  add column if not exists sections_payload jsonb not null default '[]'::jsonb
    check (jsonb_typeof(sections_payload) = 'array'),
  add column if not exists performance_level_grid jsonb not null default '[]'::jsonb
    check (jsonb_typeof(performance_level_grid) = 'array'),
  add column if not exists learner_detail_fields jsonb not null default '[]'::jsonb
    check (jsonb_typeof(learner_detail_fields) = 'array'),
  add column if not exists paper_metadata jsonb not null default '{}'::jsonb
    check (jsonb_typeof(paper_metadata) = 'object'),
  add column if not exists competency_tags jsonb not null default '[]'::jsonb
    check (jsonb_typeof(competency_tags) = 'array'),
  add column if not exists coverage_map jsonb not null default '[]'::jsonb
    check (jsonb_typeof(coverage_map) = 'array');

comment on column public.assessment_papers.sections_payload is
  'Ordered sections with instructions, section marks, and question IDs; supports theory, practical and performance tasks.';
comment on column public.assessment_papers.performance_level_grid is
  'Optional rubric/performance-band grid with criterion-level marks and locally approved CBC descriptors.';
comment on column public.assessment_papers.learner_detail_fields is
  'Learner-facing fields such as learner name, school, admission number and date; do not store unnecessary personal data.';
comment on column public.assessment_papers.paper_metadata is
  'Exam series, trial number, term, pathway, paper number, source attribution and examiner instructions.';
comment on column public.assessment_papers.coverage_map is
  'Curriculum coverage by strand, sub-strand and specific learning outcome for traceability.';
