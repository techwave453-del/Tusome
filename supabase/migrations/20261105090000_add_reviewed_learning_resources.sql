-- Structured, versioned learning resources kept separate from canonical CBC curriculum.
-- All writes are server-side through authenticated Edge Functions using service_role.
-- Learners can read only published global resources or resources shared with their school.

begin;

insert into public.app_permissions(permission_key, description) values
  ('learning_resources.create', 'Submit supplementary learning resources for review'),
  ('learning_resources.review', 'Review and approve supplementary learning resources'),
  ('learning_resources.publish', 'Publish or withdraw reviewed learning resources')
on conflict (permission_key) do update
set description = excluded.description;

insert into public.role_permissions(role_key, permission_key)
select 'teacher', 'learning_resources.create'
where exists (select 1 from public.app_roles where role_key = 'teacher')
on conflict do nothing;

insert into public.role_permissions(role_key, permission_key)
select 'curriculum_editor', permission_key
from public.app_permissions
where permission_key in (
  'learning_resources.create',
  'learning_resources.review',
  'learning_resources.publish'
)
on conflict do nothing;

insert into public.role_permissions(role_key, permission_key)
select 'platform_admin', permission_key
from public.app_permissions
where permission_key in (
  'learning_resources.create',
  'learning_resources.review',
  'learning_resources.publish'
)
on conflict do nothing;

create table if not exists public.learning_resources (
  id uuid primary key default gen_random_uuid(),
  grade_id text not null references public.curriculum_grades(id) on update cascade on delete restrict,
  learning_area_id text not null,
  strand_id uuid not null references public.curriculum_strands(id) on delete cascade,
  sub_strand_id uuid not null references public.curriculum_sub_strands(id) on delete cascade,
  school_id uuid references public.schools(id) on delete cascade,
  resource_type text not null check (resource_type in (
    'lesson_notes', 'worked_example', 'learner_activity', 'worksheet',
    'quiz', 'assessment', 'marking_scheme', 'revision_notes',
    'teacher_guide', 'project'
  )),
  title text not null check (length(trim(title)) between 2 and 180),
  summary text not null default '' check (length(summary) <= 1200),
  language_code text not null default 'en'
    check (language_code ~ '^[a-z]{2,3}(-[A-Za-z0-9]{2,8})?$'),
  status text not null default 'draft'
    check (status in ('draft', 'pending_review', 'approved', 'published', 'rejected', 'withdrawn')),
  source text not null default 'ai_generated'
    check (source in ('ai_generated', 'teacher_created', 'imported')),
  version integer not null default 1 check (version > 0),
  estimated_minutes integer check (estimated_minutes is null or estimated_minutes between 1 and 600),
  learning_outcomes text[] not null default '{}',
  content jsonb not null default '{"sections":[]}'::jsonb
    check (jsonb_typeof(content) = 'object' and jsonb_typeof(content->'sections') = 'array'),
  created_by uuid references auth.users(id) on delete set null,
  reviewed_by uuid references auth.users(id) on delete set null,
  published_by uuid references auth.users(id) on delete set null,
  review_feedback text not null default '' check (length(review_feedback) <= 5000),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  reviewed_at timestamptz,
  published_at timestamptz
);

create index if not exists learning_resources_substrand_status_idx
  on public.learning_resources(sub_strand_id, status, language_code);
create index if not exists learning_resources_grade_type_status_idx
  on public.learning_resources(grade_id, resource_type, status);
create index if not exists learning_resources_school_status_idx
  on public.learning_resources(school_id, status);
create index if not exists learning_resources_published_at_idx
  on public.learning_resources(published_at desc)
  where status = 'published';

drop trigger if exists learning_resources_set_updated_at on public.learning_resources;
create trigger learning_resources_set_updated_at
before update on public.learning_resources
for each row execute function private.set_updated_at();

create table if not exists public.learning_resource_versions (
  id uuid primary key default gen_random_uuid(),
  resource_id uuid not null references public.learning_resources(id) on delete cascade,
  version integer not null check (version > 0),
  content jsonb not null check (
    jsonb_typeof(content) = 'object' and jsonb_typeof(content->'sections') = 'array'
  ),
  title text not null,
  summary text not null default '',
  learning_outcomes text[] not null default '{}',
  changed_by uuid references auth.users(id) on delete set null,
  change_summary text not null default '',
  created_at timestamptz not null default now(),
  unique(resource_id, version)
);

create index if not exists learning_resource_versions_resource_idx
  on public.learning_resource_versions(resource_id, version desc);

create table if not exists public.learning_resource_reviews (
  id uuid primary key default gen_random_uuid(),
  resource_id uuid not null references public.learning_resources(id) on delete cascade,
  version integer not null check (version > 0),
  reviewer_id uuid references auth.users(id) on delete set null,
  decision text not null check (decision in ('approved', 'rejected', 'changes_requested')),
  feedback text not null default '' check (length(feedback) <= 5000),
  created_at timestamptz not null default now()
);

create index if not exists learning_resource_reviews_resource_idx
  on public.learning_resource_reviews(resource_id, created_at desc);

create table if not exists public.learning_resource_generation_jobs (
  id uuid primary key default gen_random_uuid(),
  requested_by uuid references auth.users(id) on delete set null,
  school_id uuid references public.schools(id) on delete set null,
  resource_id uuid references public.learning_resources(id) on delete set null,
  request_payload jsonb not null check (jsonb_typeof(request_payload) = 'object'),
  result_payload jsonb check (result_payload is null or jsonb_typeof(result_payload) = 'object'),
  status text not null default 'queued'
    check (status in ('queued', 'running', 'completed', 'failed', 'cancelled')),
  provider text not null default '',
  model text not null default '',
  error_message text not null default '' check (length(error_message) <= 5000),
  created_at timestamptz not null default now(),
  started_at timestamptz,
  completed_at timestamptz
);

create index if not exists learning_resource_generation_jobs_requester_idx
  on public.learning_resource_generation_jobs(requested_by, created_at desc);
create index if not exists learning_resource_generation_jobs_status_idx
  on public.learning_resource_generation_jobs(status, created_at);

alter table public.learning_resources enable row level security;
alter table public.learning_resource_versions enable row level security;
alter table public.learning_resource_reviews enable row level security;
alter table public.learning_resource_generation_jobs enable row level security;

drop policy if exists "Anyone reads published global learning resources" on public.learning_resources;
create policy "Anyone reads published global learning resources"
on public.learning_resources
for select to anon
using (status = 'published' and school_id is null);

drop policy if exists "Signed in learners read published shared learning resources" on public.learning_resources;
create policy "Signed in learners read published shared learning resources"
on public.learning_resources
for select to authenticated
using (
  status = 'published'
  and (
    school_id is null
    or exists (
      select 1
      from public.user_profiles up
      where up.user_id = (select auth.uid())
        and up.school_id = learning_resources.school_id
    )
  )
);

-- Drafts, reviews, generation jobs and version history are deliberately server-only.
revoke all on public.learning_resources, public.learning_resource_versions,
  public.learning_resource_reviews, public.learning_resource_generation_jobs
from public, anon, authenticated;

grant select on public.learning_resources to anon, authenticated;
grant all on public.learning_resources, public.learning_resource_versions,
  public.learning_resource_reviews, public.learning_resource_generation_jobs
to service_role;

commit;
