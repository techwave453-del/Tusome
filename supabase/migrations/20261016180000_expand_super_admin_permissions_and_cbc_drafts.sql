-- Ensure the platform super-user has every defined permission, including permissions
-- added in future releases. Application-level checks must still validate the signed-in user.
insert into public.role_permissions (role_key, permission_key)
select 'platform_admin', p.permission_key
from public.app_permissions p
on conflict (role_key, permission_key) do nothing;

create or replace function private.grant_new_permission_to_platform_admin()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    insert into public.role_permissions (role_key, permission_key)
    values ('platform_admin', new.permission_key)
    on conflict (role_key, permission_key) do nothing;
    return new;
end;
$$;

revoke all on function private.grant_new_permission_to_platform_admin() from public, anon, authenticated;

drop trigger if exists grant_new_permission_to_platform_admin on public.app_permissions;
create trigger grant_new_permission_to_platform_admin
after insert on public.app_permissions
for each row execute function private.grant_new_permission_to_platform_admin();

-- Additional original, CBC-aligned supplementary content for Grade 4.
-- This content is deliberately kept in review and unpublished until a curriculum
-- reviewer verifies it against the complete current KICD curriculum design.
insert into public.curriculum_sources (title, source_url, notes)
values (
    'KICD Grade 4 Science and Technology curriculum design (2024)',
    'https://kicd.ac.ke/wp-content/uploads/2024/10/Grade-4-Science-and-Technology.pdf',
    'Official KICD source. The supplementary draft below uses original paraphrased learning activities and outcomes; verify against the full design before publishing.'
)
on conflict (source_url) do update
set title = excluded.title, notes = excluded.notes;

do $$
declare
    v_math_source_id uuid;
    v_science_source_id uuid;
    v_math_strand_id uuid;
    v_science_strand_id uuid;
    v_math_sub1_id uuid;
    v_math_sub2_id uuid;
    v_science_sub1_id uuid;
    v_science_sub2_id uuid;
begin
    select id into v_math_source_id
    from public.curriculum_sources
    where source_url = 'https://kicd.ac.ke/cbc-materials/curriculum-designs/regular-curriculum-designs/';

    select id into v_science_source_id
    from public.curriculum_sources
    where source_url = 'https://kicd.ac.ke/wp-content/uploads/2024/10/Grade-4-Science-and-Technology.pdf';

    if v_math_source_id is null or v_science_source_id is null then
        raise exception 'Required official KICD source records were not found.';
    end if;

    insert into public.curriculum_strands
        (grade_id, learning_area_id, title, sequence, description, source_id, content_status, is_published)
    select 'g4', 'mathematics', 'Using Numbers in Everyday Situations', 90,
        'Supplementary practice on representing quantities, explaining number patterns and choosing operations for familiar Kenyan contexts.',
        v_math_source_id, 'in_review', false
    where not exists (
        select 1 from public.curriculum_strands
        where grade_id = 'g4' and learning_area_id = 'mathematics'
          and title = 'Using Numbers in Everyday Situations'
    );

    select id into v_math_strand_id
    from public.curriculum_strands
    where grade_id = 'g4' and learning_area_id = 'mathematics'
      and title = 'Using Numbers in Everyday Situations'
    order by created_at limit 1;

    insert into public.curriculum_strands
        (grade_id, learning_area_id, title, sequence, description, source_id, content_status, is_published)
    select 'g4', 'science_technology', 'Living Things and Their Needs', 90,
        'Supplementary inquiry activities about observable needs of plants and animals and responsible care for living things.',
        v_science_source_id, 'in_review', false
    where not exists (
        select 1 from public.curriculum_strands
        where grade_id = 'g4' and learning_area_id = 'science_technology'
          and title = 'Living Things and Their Needs'
    );

    select id into v_science_strand_id
    from public.curriculum_strands
    where grade_id = 'g4' and learning_area_id = 'science_technology'
      and title = 'Living Things and Their Needs'
    order by created_at limit 1;

    insert into public.curriculum_sub_strands
        (strand_id, title, sequence, description, source_id, is_published)
    select v_math_strand_id, 'Representing and Comparing Quantities', 1,
        'Use objects, drawings and number sentences to represent and compare quantities from everyday situations.',
        v_math_source_id, false
    where not exists (select 1 from public.curriculum_sub_strands
        where strand_id = v_math_strand_id and title = 'Representing and Comparing Quantities');

    select id into v_math_sub1_id from public.curriculum_sub_strands
    where strand_id = v_math_strand_id and title = 'Representing and Comparing Quantities'
    order by created_at limit 1;

    insert into public.curriculum_sub_strands
        (strand_id, title, sequence, description, source_id, is_published)
    select v_math_strand_id, 'Choosing Operations to Solve Problems', 2,
        'Explain the operation selected when solving simple addition, subtraction, multiplication or sharing problems.',
        v_math_source_id, false
    where not exists (select 1 from public.curriculum_sub_strands
        where strand_id = v_math_strand_id and title = 'Choosing Operations to Solve Problems');

    select id into v_math_sub2_id from public.curriculum_sub_strands
    where strand_id = v_math_strand_id and title = 'Choosing Operations to Solve Problems'
    order by created_at limit 1;

    insert into public.curriculum_sub_strands
        (strand_id, title, sequence, description, source_id, is_published)
    select v_science_strand_id, 'Conditions for Plant Growth', 1,
        'Investigate how water, light, air and suitable growing conditions affect plants.',
        v_science_source_id, false
    where not exists (select 1 from public.curriculum_sub_strands
        where strand_id = v_science_strand_id and title = 'Conditions for Plant Growth');

    select id into v_science_sub1_id from public.curriculum_sub_strands
    where strand_id = v_science_strand_id and title = 'Conditions for Plant Growth'
    order by created_at limit 1;

    insert into public.curriculum_sub_strands
        (strand_id, title, sequence, description, source_id, is_published)
    select v_science_strand_id, 'Animal Needs and Responsible Care', 2,
        'Observe how animals obtain food, water, shelter and suitable living conditions.',
        v_science_source_id, false
    where not exists (select 1 from public.curriculum_sub_strands
        where strand_id = v_science_strand_id and title = 'Animal Needs and Responsible Care');

    select id into v_science_sub2_id from public.curriculum_sub_strands
    where strand_id = v_science_strand_id and title = 'Animal Needs and Responsible Care'
    order by created_at limit 1;

    insert into public.learning_outcomes (sub_strand_id, outcome_text, outcome_type, sequence, source_id, is_published)
    select x.sub_id, x.outcome_text, 'specific', x.seq, x.source_id, false
    from (values
        (v_math_sub1_id, 'Represent and compare quantities using appropriate objects, drawings and number symbols.', 1, v_math_source_id),
        (v_math_sub1_id, 'Explain a number pattern noticed in a practical classroom or home example.', 2, v_math_source_id),
        (v_math_sub2_id, 'Select and explain a suitable operation when solving a familiar everyday problem.', 1, v_math_source_id),
        (v_math_sub2_id, 'Check a solution using a second method, estimation or a practical model.', 2, v_math_source_id),
        (v_science_sub1_id, 'Describe observable conditions that help a seedling grow and remain healthy.', 1, v_science_source_id),
        (v_science_sub1_id, 'Record and compare the growth of similar seedlings when one condition is changed safely.', 2, v_science_source_id),
        (v_science_sub2_id, 'Identify basic needs of a selected local animal and relate them to its habitat.', 1, v_science_source_id),
        (v_science_sub2_id, 'Suggest safe and responsible ways people can care for domestic animals and local wildlife.', 2, v_science_source_id)
    ) as x(sub_id, outcome_text, seq, source_id)
    where not exists (
        select 1 from public.learning_outcomes lo
        where lo.sub_strand_id = x.sub_id and lo.outcome_text = x.outcome_text
    );

    insert into public.inquiry_questions (sub_strand_id, question_text, sequence, source_id, is_published)
    select x.sub_id, x.question_text, 1, x.source_id, false
    from (values
        (v_math_sub1_id, 'Which representation makes it easiest to compare these quantities, and why?', v_math_source_id),
        (v_math_sub2_id, 'How can you explain that the operation you chose matches the story problem?', v_math_source_id),
        (v_science_sub1_id, 'What changes when a seedling receives less light or water than another seedling?', v_science_source_id),
        (v_science_sub2_id, 'How does an animal’s habitat help it meet its daily needs?', v_science_source_id)
    ) as x(sub_id, question_text, source_id)
    where not exists (
        select 1 from public.inquiry_questions q
        where q.sub_strand_id = x.sub_id and q.question_text = x.question_text
    );

    insert into public.learning_activities (sub_strand_id, activity_text, resources, sequence, source_id, is_published)
    select x.sub_id, x.activity_text, x.resources::jsonb, 1, x.source_id, false
    from (values
        (v_math_sub1_id, 'In small groups, collect safe classroom objects, sort them into sets, record each set in a table and explain which set has more or fewer items.',
         '["counting objects","paper","pencils"]', v_math_source_id),
        (v_math_sub2_id, 'Create a short story problem based on shopping at a local market, solve it, and explain the chosen operation to a partner.',
         '["number cards","exercise book","locally familiar price examples"]', v_math_source_id),
        (v_science_sub1_id, 'Grow two similar seedlings in safe containers, change one condition with teacher guidance, and record observations over several days.',
         '["seedlings","reused containers","water","observation chart"]', v_science_source_id),
        (v_science_sub2_id, 'Observe pictures or safely observe familiar animals; compare their food, water and shelter needs and propose a responsible care routine.',
         '["animal pictures","observation worksheet"]', v_science_source_id)
    ) as x(sub_id, activity_text, resources, source_id)
    where not exists (
        select 1 from public.learning_activities a
        where a.sub_strand_id = x.sub_id and a.activity_text = x.activity_text
    );

    insert into public.assessment_rubrics
        (sub_strand_id, criteria, exceeds_expectation, meets_expectation, approaching_expectation, below_expectation, source_id, is_published)
    select v_math_sub1_id, 'Representing and comparing quantities',
        'Selects an efficient representation, compares accurately and clearly justifies the reasoning.',
        'Represents and compares quantities correctly with a clear explanation.',
        'Represents quantities with prompts and makes occasional comparison errors.',
        'Needs guided support to represent or compare quantities.',
        v_math_source_id, false
    where not exists (select 1 from public.assessment_rubrics where sub_strand_id = v_math_sub1_id);

    insert into public.assessment_rubrics
        (sub_strand_id, criteria, exceeds_expectation, meets_expectation, approaching_expectation, below_expectation, source_id, is_published)
    select v_science_sub1_id, 'Investigating plant growth',
        'Records careful observations, identifies the changed condition and explains a plausible effect.',
        'Records observations and describes a relationship between a condition and growth.',
        'Records partial observations with support and gives a limited explanation.',
        'Needs step-by-step support to observe or record plant growth.',
        v_science_source_id, false
    where not exists (select 1 from public.assessment_rubrics where sub_strand_id = v_science_sub1_id);
end;
$$;
