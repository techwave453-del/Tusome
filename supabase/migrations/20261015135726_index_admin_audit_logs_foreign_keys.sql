-- Cover the audit log foreign keys to avoid slow delete/update checks.
create index if not exists admin_audit_logs_actor_id_idx
  on public.admin_audit_logs(actor_id);

create index if not exists admin_audit_logs_target_user_id_idx
  on public.admin_audit_logs(target_user_id);
