-- Tusome RBAC foundation. Applied to Supabase project wkwnoflielrotknvoflz.
-- Keep this migration in source control; elevated role assignments are server-managed only.

create schema if not exists private;
revoke all on schema private from public, anon;
grant usage on schema private to authenticated;

create table if not exists public.schools (
  id uuid primary key default gen_random_uuid(),
  name text not null check (length(trim(name)) between 2 and 160),
  county text not null default '',
  is_active boolean not null default true,
  created_at timestamptz not null default now()
);

create table if not exists public.app_roles (
  role_key text primary key,
  role_name text not null,
  scope text not null check (scope in ('global', 'school')),
  description text not null default ''
);

create table if not exists public.app_permissions (
  permission_key text primary key,
  description text not null
);

create table if not exists public.role_permissions (
  role_key text not null references public.app_roles(role_key) on delete cascade,
  permission_key text not null references public.app_permissions(permission_key) on delete cascade,
  primary key (role_key, permission_key)
);

create table if not exists public.user_profiles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  display_name text not null default '',
  grade_id text references public.curriculum_grades(id) on update cascade on delete set null,
  school_id uuid references public.schools(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint user_profiles_display_name_length check (length(display_name) <= 120)
);

create table if not exists public.user_role_assignments (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  role_key text not null references public.app_roles(role_key),
  school_id uuid references public.schools(id) on delete cascade,
  assigned_by uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now()
);

create unique index if not exists user_role_assignments_global_unique
  on public.user_role_assignments(user_id, role_key)
  where school_id is null;
create unique index if not exists user_role_assignments_school_unique
  on public.user_role_assignments(user_id, role_key, school_id)
  where school_id is not null;
create index if not exists user_role_assignments_school_user_idx
  on public.user_role_assignments(school_id, user_id);
create index if not exists user_profiles_school_idx on public.user_profiles(school_id);

insert into public.app_roles(role_key, role_name, scope, description) values
 ('learner', 'Learner', 'global', 'Learns, completes activities and views own progress'),
 ('teacher', 'Teacher', 'school', 'Supports assigned learners and records assessments'),
 ('school_admin', 'School Administrator', 'school', 'Manages users and learning operations within one school'),
 ('curriculum_editor', 'Curriculum Editor', 'global', 'Creates and reviews curriculum content'),
 ('platform_admin', 'Platform Administrator', 'global', 'Manages platform configuration and global access')
on conflict (role_key) do update set role_name = excluded.role_name, scope = excluded.scope, description = excluded.description;

insert into public.app_permissions(permission_key, description) values
 ('profile.read.self', 'Read own profile'),
 ('profile.update.self', 'Update own permitted profile fields'),
 ('progress.read.self', 'Read own learning progress'),
 ('progress.write.self', 'Write own learning progress'),
 ('assessments.read', 'Read available assessments'),
 ('assessments.record', 'Record learner assessments'),
 ('users.read.school', 'Read user profiles within assigned school'),
 ('users.manage.school', 'Manage user memberships within assigned school'),
 ('curriculum.read', 'Read published curriculum'),
 ('curriculum.create', 'Create curriculum drafts'),
 ('curriculum.review', 'Review curriculum drafts'),
 ('curriculum.publish', 'Publish curriculum content'),
 ('platform.configure', 'Manage platform-wide configuration'),
 ('roles.manage', 'Manage role assignments')
on conflict (permission_key) do update set description = excluded.description;

insert into public.role_permissions(role_key, permission_key)
select 'learner', permission_key from public.app_permissions
where permission_key in ('profile.read.self','profile.update.self','progress.read.self','progress.write.self','assessments.read','curriculum.read')
on conflict do nothing;

insert into public.role_permissions(role_key, permission_key)
select 'teacher', permission_key from public.app_permissions
where permission_key in ('profile.read.self','profile.update.self','progress.read.self','progress.write.self','assessments.read','assessments.record','users.read.school','curriculum.read')
on conflict do nothing;

insert into public.role_permissions(role_key, permission_key)
select 'school_admin', permission_key from public.app_permissions
where permission_key in ('profile.read.self','profile.update.self','progress.read.self','progress.write.self','assessments.read','assessments.record','users.read.school','users.manage.school','curriculum.read')
on conflict do nothing;

insert into public.role_permissions(role_key, permission_key)
select 'curriculum_editor', permission_key from public.app_permissions
where permission_key in ('curriculum.read','curriculum.create','curriculum.review','curriculum.publish')
on conflict do nothing;

insert into public.role_permissions(role_key, permission_key)
select 'platform_admin', permission_key from public.app_permissions
on conflict do nothing;

create or replace function private.has_permission(_permission_key text, _school_id uuid default null)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.user_role_assignments ura
    join public.app_roles ar on ar.role_key = ura.role_key
    join public.role_permissions rp on rp.role_key = ura.role_key
    where ura.user_id = (select auth.uid())
      and rp.permission_key = _permission_key
      and (
        (ar.scope = 'global' and ura.school_id is null)
        or
        (ar.scope = 'school' and _school_id is not null and ura.school_id = _school_id)
      )
  );
$$;

create or replace function private.has_role(_role_key text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1 from public.user_role_assignments ura
    where ura.user_id = (select auth.uid())
      and ura.role_key = _role_key
      and ura.school_id is null
  );
$$;

revoke all on function private.has_permission(text, uuid) from public, anon;
revoke all on function private.has_role(text) from public, anon;
grant execute on function private.has_permission(text, uuid) to authenticated;
grant execute on function private.has_role(text) to authenticated;

create or replace function private.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists user_profiles_set_updated_at on public.user_profiles;
create trigger user_profiles_set_updated_at
before update on public.user_profiles
for each row execute function private.set_updated_at();

create or replace function private.bootstrap_auth_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.user_profiles(user_id, display_name)
  values (
    new.id,
    coalesce(nullif(trim(new.raw_user_meta_data ->> 'display_name'), ''), '')
  )
  on conflict (user_id) do nothing;

  insert into public.user_role_assignments(user_id, role_key, school_id)
  values (new.id, 'learner', null)
  on conflict do nothing;

  return new;
end;
$$;

drop trigger if exists on_auth_user_created_tusome on auth.users;
create trigger on_auth_user_created_tusome
after insert on auth.users
for each row execute function private.bootstrap_auth_user();

alter table public.schools enable row level security;
alter table public.app_roles enable row level security;
alter table public.app_permissions enable row level security;
alter table public.role_permissions enable row level security;
alter table public.user_profiles enable row level security;
alter table public.user_role_assignments enable row level security;

drop policy if exists "Authenticated users read roles" on public.app_roles;
create policy "Authenticated users read roles" on public.app_roles
for select to authenticated using (true);

drop policy if exists "Authenticated users read permissions" on public.app_permissions;
create policy "Authenticated users read permissions" on public.app_permissions
for select to authenticated using (true);

drop policy if exists "Authenticated users read role permissions" on public.role_permissions;
create policy "Authenticated users read role permissions" on public.role_permissions
for select to authenticated using (true);

drop policy if exists "Users read own profile or school-authorized profiles" on public.user_profiles;
create policy "Users read own profile or school-authorized profiles" on public.user_profiles
for select to authenticated
using (
  user_id = (select auth.uid())
  or private.has_permission('users.read.school', school_id)
);

drop policy if exists "Users update own profile" on public.user_profiles;
create policy "Users update own profile" on public.user_profiles
for update to authenticated
using (user_id = (select auth.uid()))
with check (user_id = (select auth.uid()));

drop policy if exists "Users read own role assignments" on public.user_role_assignments;
create policy "Users read own role assignments" on public.user_role_assignments
for select to authenticated
using (
  user_id = (select auth.uid())
  or private.has_permission('users.manage.school', school_id)
  or private.has_permission('roles.manage', null)
);

drop policy if exists "School members read their school" on public.schools;
create policy "School members read their school" on public.schools
for select to authenticated
using (
  exists (
    select 1 from public.user_profiles up
    where up.user_id = (select auth.uid())
      and up.school_id = schools.id
  )
  or private.has_permission('platform.configure', null)
);

revoke all on public.schools, public.app_roles, public.app_permissions,
  public.role_permissions, public.user_profiles, public.user_role_assignments
from anon, authenticated;

grant select on public.app_roles, public.app_permissions, public.role_permissions to authenticated;
grant select on public.schools to authenticated;
grant select on public.user_profiles to authenticated;
grant update(display_name, grade_id) on public.user_profiles to authenticated;
grant select on public.user_role_assignments to authenticated;
