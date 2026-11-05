-- Keep the progress table inaccessible to anonymous clients and prevent row deletion.
revoke all on public.learner_activity_progress from anon;
revoke delete on public.learner_activity_progress from authenticated;
grant select, insert, update on public.learner_activity_progress to authenticated;
