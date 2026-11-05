-- AI-generated sub-strand lesson packs are stored separately from authoritative curriculum rows.
-- Generated packs remain drafts until a curriculum editor reviews them.
create table if not exists public.ai_substrand_content (
  id uuid primary key default gen_random_uuid(),
  grade_id text not null,
  learning_area_id text not null,
  strand_id text not null,
  sub_strand_id text not null,
  generated_by uuid not null references auth.users(id) on delete restrict,
  model text not null,
  content_payload jsonb not null,
  source_snapshot jsonb not null,
  review_status text not null default 'pending_review'
    check (review_status in ('pending_review','approved','rejected')),
  created_at timestamptz not null default now(),
  reviewed_at timestamptz,
  reviewed_by uuid references auth.users(id) on delete set null
);
create index if not exists ai_substrand_content_lookup_idx
  on public.ai_substrand_content (sub_strand_id, created_at desc);
alter table public.ai_substrand_content enable row level security;
revoke all on public.ai_substrand_content from anon, authenticated;
grant select on public.ai_substrand_content to authenticated;
drop policy if exists "curriculum editors read generated sub-strand content" on public.ai_substrand_content;
create policy "curriculum editors read generated sub-strand content"
  on public.ai_substrand_content for select to authenticated
  using (
    exists (
      select 1 from public.user_role_assignments ura
      where ura.user_id = auth.uid()
        and ura.role_key in ('platform_admin','curriculum_editor')
    )
  );
