-- Learners may submit their own answers but cannot assign marks or alter a submitted attempt.
create or replace function private.guard_learner_assessment_attempt_update()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if auth.uid() is not null then
    if old.user_id <> (select auth.uid()) or new.user_id <> old.user_id then
      raise exception 'Learners may update only their own assessment attempt.';
    end if;
    if new.paper_id <> old.paper_id
      or new.paper_version <> old.paper_version
      or new.started_at <> old.started_at then
      raise exception 'Assessment identity and start time are immutable.';
    end if;
    if new.score is distinct from old.score
      or new.teacher_feedback is distinct from old.teacher_feedback then
      raise exception 'Only an authorised marker may update scores or teacher feedback.';
    end if;
    if old.submitted_at is not null and (
      new.answers is distinct from old.answers
      or new.submitted_at is distinct from old.submitted_at
    ) then
      raise exception 'Submitted assessment answers are locked.';
    end if;
  end if;
  return new;
end;
$$;

drop trigger if exists learner_assessment_attempt_update_guard on public.learner_assessment_attempts;
create trigger learner_assessment_attempt_update_guard
before update on public.learner_assessment_attempts
for each row execute function private.guard_learner_assessment_attempt_update();
