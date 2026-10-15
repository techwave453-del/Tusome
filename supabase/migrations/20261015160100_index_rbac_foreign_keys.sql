create index if not exists role_permissions_permission_key_idx on public.role_permissions(permission_key);
create index if not exists user_profiles_grade_id_idx on public.user_profiles(grade_id);
create index if not exists user_role_assignments_assigned_by_idx on public.user_role_assignments(assigned_by);
create index if not exists user_role_assignments_role_key_idx on public.user_role_assignments(role_key);