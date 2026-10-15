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
        (id, grade_id, learning_area_id, title, sequence, description, source_id, content_status, is_published)
    values
        ('a1000000-0000-4000-8000-000000000001', 'g4', 'mathematics',
         'Using Numbers in Everyday Situations', 90,
         'Supplementary practice on representing quantities, explaining number patterns and choosing operations for familiar Kenyan contexts.',
         v_math_source_id, 'in_review', false),
        ('a1000000-0000-4000-8000-000000000002', 'g4', 'science_technology',
         'Living Things and Their Needs', 90,
         'Supplementary inquiry activities about observable needs of plants and animals and responsible care for living things.',
         v_science_source_id, 'in_review', false)
    on conflict (id) do update set
        title = excluded.title,
        description = excluded.description,
        source_id = excluded.source_id,
        content_status = 'in_review',
        is_published = false,
        updated_at = now();

    insert into public.curriculum_sub_strands
        (id, strand_id, title, sequence, description, source_id, is_published)
    values
        ('b1000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000001',
         'Representing and Comparing Quantities', 1,
         'Use objects, drawings and number sentences to represent and compare quantities from everyday situations.',
         v_math_source_id, false),
        ('b1000000-0000-4000-8000-000000000002', 'a1000000-0000-4000-8000-000000000001',
         'Choosing Operations to Solve Problems', 2,
         'Explain the operation selected when solving simple addition, subtraction, multiplication or sharing problems.',
         v_math_source_id, false),
        ('b1000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000002',
         'Conditions for Plant Growth', 1,
         'Investigate how water, light, air and suitable growing conditions affect plants.',
         v_science_source_id, false),
        ('b1000000-0000-4000-8000-000000000004', 'a1000000-0000-4000-8000-000000000002',
         'Animal Needs and Responsible Care', 2,
         'Observe how animals obtain food, water, shelter and suitable living conditions.',
         v_science_source_id, false)
    on conflict (id) do update set
        title = excluded.title,
        description = excluded.description,
        source_id = excluded.source_id,
        is_published = false;

    insert into public.learning_outcomes
        (id, sub_strand_id, outcome_text, outcome_type, sequence, source_id, is_published)
    values
        ('c1000000-0000-4000-8000-000000000001', 'b1000000-0000-4000-8000-000000000001',
         'Represent and compare quantities using appropriate objects, drawings and number symbols.', 'specific', 1, v_math_source_id, false),
        ('c1000000-0000-4000-8000-000000000002', 'b1000000-0000-4000-8000-000000000001',
         'Explain a number pattern noticed in a practical classroom or home example.', 'specific', 2, v_math_source_id, false),
        ('c1000000-0000-4000-8000-000000000003', 'b1000000-0000-4000-8000-000000000002',
         'Select and explain a suitable operation when solving a familiar everyday problem.', 'specific', 1, v_math_source_id, false),
        ('c1000000-0000-4000-8000-000000000004', 'b1000000-0000-4000-8000-000000000002',
         'Check a solution using a second method, estimation or a practical model.', 'specific', 2, v_math_source_id, false),
        ('c1000000-0000-4000-8000-000000000005', 'b1000000-0000-4000-8000-000000000003',
         'Describe observable conditions that help a seedling grow and remain healthy.', 'specific', 1, v_science_source_id, false),
        ('c1000000-0000-4000-8000-000000000006', 'b1000000-0000-4000-8000-000000000003',
         'Record and compare the growth of similar seedlings when one condition is changed safely.', 'specific', 2, v_science_source_id, false),
        ('c1000000-0000-4000-8000-000000000007', 'b1000000-0000-4000-8000-000000000004',
         'Identify basic needs of a selected local animal and relate them to its habitat.', 'specific', 1, v_science_source_id, false),
        ('c1000000-0000-4000-8000-000000000008', 'b1000000-0000-4000-8000-000000000004',
         'Suggest safe and responsible ways people can care for domestic animals and local wildlife.', 'specific', 2, v_science_source_id, false)
    on conflict (id) do update set
        outcome_text = excluded.outcome_text,
        outcome_type = excluded.outcome_type,
        sequence = excluded.sequence,
        source_id = excluded.source_id,
        is_published = false;

    insert into public.inquiry_questions
        (id, sub_strand_id, question_text, sequence, source_id, is_published)
    values
        ('d1000000-0000-4000-8000-000000000001', 'b1000000-0000-4000-8000-000000000001',
         'Which representation makes it easiest to compare these quantities, and why?', 1, v_math_source_id, false),
        ('d1000000-0000-4000-8000-000000000002', 'b1000000-0000-4000-8000-000000000002',
         'How can you explain that the operation you chose matches the story problem?', 1, v_math_source_id, false),
        ('d1000000-0000-4000-8000-000000000003', 'b1000000-0000-4000-8000-000000000003',
         'What changes when a seedling receives less light or water than another seedling?', 1, v_science_source_id, false),
        ('d1000000-0000-4000-8000-000000000004', 'b1000000-0000-4000-8000-000000000004',
         'How does an animal’s habitat help it meet its daily needs?', 1, v_science_source_id, false)
    on conflict (id) do update set
        question_text = excluded.question_text,
        sequence = excluded.sequence,
        source_id = excluded.source_id,
        is_published = false;

    insert into public.learning_activities
        (id, sub_strand_id, activity_text, resources, sequence, source_id, is_published)
    values
        ('e1000000-0000-4000-8000-000000000001', 'b1000000-0000-4000-8000-000000000001',
         'In small groups, collect safe classroom objects, sort them into sets, record each set in a table and explain which set has more or fewer items.',
         '["counting objects","paper","pencils"]'::jsonb, 1, v_math_source_id, false),
        ('e1000000-0000-4000-8000-000000000002', 'b1000000-0000-4000-8000-000000000002',
         'Create a short story problem based on shopping at a local market, solve it, and explain the chosen operation to a partner.',
         '["number cards","exercise book","locally familiar price examples"]'::jsonb, 1, v_math_source_id, false),
        ('e1000000-0000-4000-8000-000000000003', 'b1000000-0000-4000-8000-000000000003',
         'Grow two similar seedlings in safe containers, change one condition with teacher guidance, and record observations over several days.',
         '["seedlings","reused containers","water","observation chart"]'::jsonb, 1, v_science_source_id, false),
        ('e1000000-0000-4000-8000-000000000004', 'b1000000-0000-4000-8000-000000000004',
         'Observe pictures or safely observe familiar animals; compare their food, water and shelter needs and propose a responsible care routine.',
         '["animal pictures","observation worksheet"]'::jsonb, 1, v_science_source_id, false)
    on conflict (id) do update set
        activity_text = excluded.activity_text,
        resources = excluded.resources,
        sequence = excluded.sequence,
        source_id = excluded.source_id,
        is_published = false;

    insert into public.assessment_rubrics
        (id, sub_strand_id, criteria, exceeds_expectation, meets_expectation, approaching_expectation, below_expectation, source_id, is_published)
    values
        ('f1000000-0000-4000-8000-000000000001', 'b1000000-0000-4000-8000-000000000001',
         'Representing and comparing quantities',
         'Selects an efficient representation, compares accurately and clearly justifies the reasoning.',
         'Represents and compares quantities correctly with a clear explanation.',
         'Represents quantities with prompts and makes occasional comparison errors.',
         'Needs guided support to represent or compare quantities.',
         v_math_source_id, false),
        ('f1000000-0000-4000-8000-000000000002', 'b1000000-0000-4000-8000-000000000003',
         'Investigating plant growth',
         'Records careful observations, identifies the changed condition and explains a plausible effect.',
         'Records observations and describes a relationship between a condition and growth.',
         'Records partial observations with support and gives a limited explanation.',
         'Needs step-by-step support to observe or record plant growth.',
         v_science_source_id, false)
    on conflict (id) do update set
        criteria = excluded.criteria,
        exceeds_expectation = excluded.exceeds_expectation,
        meets_expectation = excluded.meets_expectation,
        approaching_expectation = excluded.approaching_expectation,
        below_expectation = excluded.below_expectation,
        source_id = excluded.source_id,
        is_published = false;
end;
$$;
