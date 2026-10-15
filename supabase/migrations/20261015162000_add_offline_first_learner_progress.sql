-- Offline-first learner activity progress, isolated by authenticated user.
create table if not exists public.learner_activity_progress (
  user_id uuid not null references auth.users(id) on delete cascade,
  sub_strand_id text not null check (length(trim(sub_strand_id)) between 1 and 200),
  is_completed boolean not null default false,
  learner_note text not null default '',
  evaluation_level text not null default 'ME'
    check (evaluation_level in ('EE', 'ME', 'AE', 'BE')),
  client_updated_at bigint not null default 0 check (client_updated_at >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (user_id, sub_strand_id),
  constraint learner_activity_progress_note_length check (length(learner_note) <= 10000)
);

create index if not exists learner_activity_progress_user_updated_idx
  on public.learner_activity_progress(user_id, client_updated_at desc);

alter table public.learner_activity_progress enable row level security;

drop policy if exists "Learners read own activity progress" on public.learner_activity_progress;
create policy "Learners read own activity progress"
  on public.learner_activity_progress
  for select to authenticated
  using (user_id = (select auth.uid()));

drop policy if exists "Learners insert own activity progress" on public.learner_activity_progress;
create policy "Learners insert own activity progress"
  on public.learner_activity_progress
  for insert to authenticated
  with check (user_id = (select auth.uid()));

drop policy if exists "Learners update own activity progress" on public.learner_activity_progress;
create policy "Learners update own activity progress"
  on public.learner_activity_progress
  for update to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));

grant select, insert, update on public.learner_activity_progress to authenticated;
