# Bootstrap the first Tusome platform administrator

New public registrations must never receive administrator privileges automatically. The database intentionally assigns every new account only the `learner` role.

To establish the first trusted platform administrator:

1. Register the trusted administrator's email through the Tusome app.
2. If Supabase email confirmation is enabled, complete the confirmation step.
3. Open the Tusome project SQL Editor: https://supabase.com/dashboard/project/wkwnoflielrotknvoflz/sql/new
4. Replace the email literal below with the exact email address of that trusted account, then run this once:

```sql
insert into public.user_role_assignments (user_id, role_key, school_id, assigned_by)
select u.id, 'platform_admin', null, null
from auth.users u
where lower(u.email) = lower('trusted-admin@example.com')
  and not exists (
    select 1
    from public.user_role_assignments ura
    where ura.user_id = u.id
      and ura.role_key = 'platform_admin'
      and ura.school_id is null
  );
```

5. Verify that exactly the intended account has the global role:

```sql
select u.email, ura.role_key, ura.school_id
from public.user_role_assignments ura
join auth.users u on u.id = ura.user_id
where ura.role_key = 'platform_admin';
```

Do not use a public registration form to choose this role. Do not add service-role credentials to Android or share them with school administrators.

After this one-time bootstrap, the deployed `manage-school-users` Edge Function can create schools and invite users with approved roles. It validates the caller's Supabase session and checks the caller's database role on every request. Platform administrators can create schools and assign roles; school administrators can invite learners and teachers only into their own school.
