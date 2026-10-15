-- Default to one role per user. A platform administrator may explicitly
-- assign additional roles; the platform administrator role itself is protected
-- by the role-management Edge Function.
create or replace function private.enforce_role_assignment_policy()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  existing_role_count integer;
  target_is_platform_admin boolean;
  assigner_is_platform_admin boolean;
begin
  select count(*)
    into existing_role_count
    from public.user_role_assignments ura
   where ura.user_id = new.user_id
     and ura.id <> new.id;

  if existing_role_count = 0 then
    return new;
  end if;

  select exists (
    select 1
      from public.user_role_assignments ura
     where ura.user_id = new.user_id
       and ura.role_key = 'platform_admin'
       and ura.school_id is null
  ) into target_is_platform_admin;

  select exists (
    select 1
      from public.user_role_assignments ura
     where ura.user_id = new.assigned_by
       and ura.role_key = 'platform_admin'
       and ura.school_id is null
  ) into assigner_is_platform_admin;

  if not target_is_platform_admin and not assigner_is_platform_admin then
    raise exception 'A user can have only one role unless an additional role is explicitly assigned by a platform administrator.'
      using errcode = '42501';
  end if;

  return new;
end;
$$;

drop trigger if exists enforce_role_assignment_policy on public.user_role_assignments;
create trigger enforce_role_assignment_policy
before insert or update of user_id, role_key, school_id, assigned_by
on public.user_role_assignments
for each row execute function private.enforce_role_assignment_policy();

revoke all on function private.enforce_role_assignment_policy() from public, anon, authenticated;
