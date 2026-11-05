-- Keep answer keys out of learner assessment downloads.
-- Learners read the safe view; the marking guide is delivered only after a submitted attempt is verified.

begin;

create or replace view public.published_assessment_papers
with (security_invoker = true)
as
select
  id, paper_code, grade_id, learning_area_id, strand_id, sub_strand_id,
  assessment_type, title, description, instructions, duration_minutes, total_marks,
  question_payload, sections_payload, performance_level_grid, learner_detail_fields,
  paper_metadata, competency_tags, coverage_map, source_reference,
  term_number, trial_number, paper_number, pathway, academic_year,
  version, content_status, published_at, created_at, updated_at, school_id
from public.assessment_papers
where content_status = 'published';

revoke select on public.assessment_papers from authenticated;
grant select on public.published_assessment_papers to authenticated;

commit;
