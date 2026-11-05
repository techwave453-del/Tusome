-- Assessment generation and editorial workflow.
-- The AI provider and all privileged database writes remain server-side.

begin;

insert into public.app_permissions(permission_key, description) values
  ('assessments.create', 'Generate or prepare CBC assessment papers for review'),
  ('assessments.review', 'Review and approve generated CBC assessment papers'),
  ('assessments.publish', 'Publish or withdraw approved CBC assessment papers')
on conflict (permission_key) do update set description = excluded.description;

insert into public.role_permissions(role_key, permission_key)
select 'teacher', 'assessments.create'
where exists (select 1 from public.app_roles where role_key = 'teacher')
on conflict do nothing;

insert into public.role_permissions(role_key, permission_key)
select 'curriculum_editor', permission_key
from public.app_permissions
where permission_key in ('assessments.create','assessments.review','assessments.publish')
on conflict do nothing;

insert into public.role_permissions(role_key, permission_key)
select 'platform_admin', permission_key
from public.app_permissions
where permission_key in ('assessments.create','assessments.review','assessments.publish')
on conflict do nothing;

alter table public.assessment_papers
  add column if not exists school_id uuid references public.schools(id) on delete cascade;

alter table public.assessment_papers
  drop constraint if exists assessment_papers_content_status_check;
alter table public.assessment_papers
  add constraint assessment_papers_content_status_check
  check (content_status in ('draft','in_review','approved','published','archived'));

create table if not exists public.assessment_generation_jobs (
  id uuid primary key default gen_random_uuid(),
  requested_by uuid references auth.users(id) on delete set null,
  school_id uuid references public.schools(id) on delete set null,
  paper_id uuid references public.assessment_papers(id) on delete set null,
  request_payload jsonb not null check (jsonb_typeof(request_payload) = 'object'),
  status text not null default 'queued'
    check (status in ('queued','running','completed','failed','cancelled')),
  provider text not null default 'openai',
  model text not null default '',
  error_message text not null default '' check (length(error_message) <= 3000),
  created_at timestamptz not null default now(),
  started_at timestamptz,
  completed_at timestamptz
);

create index if not exists assessment_generation_jobs_requester_idx
  on public.assessment_generation_jobs(requested_by, created_at desc);
create index if not exists assessment_generation_jobs_status_idx
  on public.assessment_generation_jobs(status, created_at);

alter table public.assessment_generation_jobs enable row level security;

drop policy if exists "Learners read published assessments" on public.assessment_papers;
create policy "Learners and authorised staff read assessments"
  on public.assessment_papers for select to authenticated
  using (
    (
      content_status = 'published'
      and (
        school_id is null
        or exists (
          select 1 from public.user_profiles up
          where up.user_id = (select auth.uid())
            and up.school_id = assessment_papers.school_id
        )
      )
    )
    or (created_by = (select auth.uid()) and (select private.has_permission('assessments.create', school_id)))
    or (select private.has_permission('assessments.review', null))
  );

-- All assessment writes go through the trusted Edge Function so generated JSON,
-- review state and publication metadata are validated together.
drop policy if exists "Curriculum editors create assessment drafts" on public.assessment_papers;
drop policy if exists "Curriculum reviewers update assessment drafts" on public.assessment_papers;

revoke insert, update, delete on public.assessment_papers from authenticated;
grant select on public.assessment_papers to authenticated;
revoke all on public.assessment_generation_jobs from public, anon, authenticated;
grant all on public.assessment_generation_jobs to service_role;

commit;
