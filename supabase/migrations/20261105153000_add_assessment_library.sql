-- Central assessment library and learner attempt sync.
-- Papers are authored/reviewed by curriculum editors; generated content must remain draft until reviewed.

create table if not exists public.assessment_papers (
  id uuid primary key default gen_random_uuid(),
  paper_code text not null unique check (length(trim(paper_code)) between 2 and 120),
  grade_id text not null references public.curriculum_grades(id) on update cascade on delete restrict,
  learning_area_id text not null references public.learning_areas(id) on update cascade on delete restrict,
  strand_id uuid references public.curriculum_strands(id) on delete set null,
  sub_strand_id uuid references public.curriculum_sub_strands(id) on delete set null,
  assessment_type text not null default 'practice'
    check (assessment_type in ('diagnostic','formative','summative','sba','practice')),
  title text not null check (length(trim(title)) between 3 and 200),
  description text not null default '',
  instructions text not null default '',
  duration_minutes integer not null default 30 check (duration_minutes between 1 and 600),
  total_marks numeric(8,2) not null default 0 check (total_marks >= 0),
  question_payload jsonb not null default '[]'::jsonb
    check (jsonb_typeof(question_payload) = 'array'),
  marking_payload jsonb not null default '[]'::jsonb
    check (jsonb_typeof(marking_payload) = 'array'),
  source_reference text not null default '',
  generation_method text not null default 'editor_authored'
    check (generation_method in ('editor_authored','ai_generated','imported')),
  content_status text not null default 'draft'
    check (content_status in ('draft','in_review','published','archived')),
  version integer not null default 1 check (version > 0),
  created_by uuid references auth.users(id) on delete set null default auth.uid(),
  reviewed_by uuid references auth.users(id) on delete set null,
  published_by uuid references auth.users(id) on delete set null,
  reviewed_at timestamptz,
  published_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint assessment_papers_publish_review_check
    check (content_status <> 'published' or (reviewed_by is not null and reviewed_at is not null))
);

create index if not exists assessment_papers_grade_area_status_idx
  on public.assessment_papers(grade_id, learning_area_id, content_status, updated_at desc);
create index if not exists assessment_papers_strand_idx
  on public.assessment_papers(strand_id) where strand_id is not null;
create index if not exists assessment_papers_sub_strand_idx
  on public.assessment_papers(sub_strand_id) where sub_strand_id is not null;

create table if not exists public.learner_assessment_attempts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  paper_id uuid not null references public.assessment_papers(id) on delete restrict,
  paper_version integer not null check (paper_version > 0),
  answers jsonb not null default '{}'::jsonb
    check (jsonb_typeof(answers) = 'object'),
  started_at timestamptz not null default now(),
  submitted_at timestamptz,
  score numeric(8,2) check (score is null or score >= 0),
  teacher_feedback jsonb not null default '[]'::jsonb
    check (jsonb_typeof(teacher_feedback) = 'array'),
  client_updated_at bigint not null default 0 check (client_updated_at >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (user_id, id)
);

create index if not exists learner_assessment_attempts_user_updated_idx
  on public.learner_assessment_attempts(user_id, client_updated_at desc);
create index if not exists learner_assessment_attempts_paper_idx
  on public.learner_assessment_attempts(paper_id, submitted_at desc);

create or replace function private.touch_assessment_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists assessment_papers_touch_updated_at on public.assessment_papers;
create trigger assessment_papers_touch_updated_at
before update on public.assessment_papers
for each row execute function private.touch_assessment_updated_at();

drop trigger if exists learner_assessment_attempts_touch_updated_at on public.learner_assessment_attempts;
create trigger learner_assessment_attempts_touch_updated_at
before update on public.learner_assessment_attempts
for each row execute function private.touch_assessment_updated_at();

alter table public.assessment_papers enable row level security;
alter table public.learner_assessment_attempts enable row level security;

drop policy if exists "Learners read published assessments" on public.assessment_papers;
create policy "Learners read published assessments"
  on public.assessment_papers for select to authenticated
  using (
    content_status = 'published'
    or (select private.has_permission('curriculum.create', null))
    or (select private.has_permission('curriculum.review', null))
  );

drop policy if exists "Curriculum editors create assessment drafts" on public.assessment_papers;
create policy "Curriculum editors create assessment drafts"
  on public.assessment_papers for insert to authenticated
  with check (
    created_by = (select auth.uid())
    and content_status in ('draft','in_review')
    and (select private.has_permission('curriculum.create', null))
  );

drop policy if exists "Curriculum reviewers update assessment drafts" on public.assessment_papers;
create policy "Curriculum reviewers update assessment drafts"
  on public.assessment_papers for update to authenticated
  using (
    (select private.has_permission('curriculum.review', null))
    or (select private.has_permission('curriculum.publish', null))
  )
  with check (
    (select private.has_permission('curriculum.review', null))
    or (
      (select private.has_permission('curriculum.publish', null))
      and content_status = 'published'
      and reviewed_by is not null
      and reviewed_at is not null
      and published_by = (select auth.uid())
    )
  );

drop policy if exists "Learners read own assessment attempts" on public.learner_assessment_attempts;
create policy "Learners read own assessment attempts"
  on public.learner_assessment_attempts for select to authenticated
  using (user_id = (select auth.uid()));

drop policy if exists "Learners create own assessment attempts" on public.learner_assessment_attempts;
create policy "Learners create own assessment attempts"
  on public.learner_assessment_attempts for insert to authenticated
  with check (
    user_id = (select auth.uid())
    and exists (
      select 1 from public.assessment_papers p
      where p.id = paper_id and p.content_status = 'published'
    )
  );

drop policy if exists "Learners update own assessment attempts" on public.learner_assessment_attempts;
create policy "Learners update own assessment attempts"
  on public.learner_assessment_attempts for update to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));

revoke all on public.assessment_papers, public.learner_assessment_attempts from anon;
revoke all on public.assessment_papers, public.learner_assessment_attempts from authenticated;
grant select on public.assessment_papers to authenticated;
grant insert, update on public.assessment_papers to authenticated;
grant select, insert, update on public.learner_assessment_attempts to authenticated;
