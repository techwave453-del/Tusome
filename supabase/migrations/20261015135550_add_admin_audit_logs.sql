-- Administrative audit trail for school and role management.
-- Access is server-only through the manage-school-users Edge Function.
create table if not exists public.admin_audit_logs (
  id bigint generated always as identity primary key,
  actor_id uuid references auth.users(id) on delete set null,
  action_key text not null check (length(action_key) between 2 and 80),
  target_user_id uuid references auth.users(id) on delete set null,
  school_id uuid references public.schools(id) on delete set null,
  details jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create index if not exists admin_audit_logs_created_at_idx
  on public.admin_audit_logs(created_at desc);

create index if not exists admin_audit_logs_school_created_idx
  on public.admin_audit_logs(school_id, created_at desc);

alter table public.admin_audit_logs enable row level security;
revoke all on public.admin_audit_logs from public, anon, authenticated;
grant all on public.admin_audit_logs to service_role;
