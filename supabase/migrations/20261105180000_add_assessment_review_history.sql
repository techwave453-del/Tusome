-- Human review history for AI-generated assessment papers.
begin;

create table if not exists public.assessment_paper_reviews (
  id uuid primary key default gen_random_uuid(),
  paper_id uuid not null references public.assessment_papers(id) on delete cascade,
  paper_version integer not null check (paper_version > 0),
  reviewer_id uuid references auth.users(id) on delete set null,
  decision text not null check (decision in ('approved','changes_requested','rejected','published','withdrawn')),
  feedback text not null default '' check (length(feedback) <= 5000),
  created_at timestamptz not null default now()
);

create index if not exists assessment_paper_reviews_paper_idx
  on public.assessment_paper_reviews(paper_id, created_at desc);

alter table public.assessment_paper_reviews enable row level security;
revoke all on public.assessment_paper_reviews from public, anon, authenticated;
grant all on public.assessment_paper_reviews to service_role;

commit;
