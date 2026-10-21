# Tusome Supabase CBC Curriculum

## Project

- Supabase project: `Tusome`
- Project ref: `wkwnoflielrotknvoflz`
- Region: `eu-west-1` (Ireland)
- API URL: `https://wkwnoflielrotknvoflz.supabase.co`
- Android uses the Supabase **publishable** key in `SupabaseCurriculumRepository.kt`. This key is intended for client apps; never add a service-role key to Android.

## Tables

- `curriculum_grades`: PP1, PP2 and Grades 1–12.
- `learning_areas`: subject/learning-area catalogue.
- `grade_learning_areas`: grade-to-area catalogue with pathway labels.
- `curriculum_strands`, `curriculum_sub_strands`: curriculum hierarchy.
- `learning_outcomes`, `inquiry_questions`, `learning_activities`, `assessment_rubrics`: teaching and assessment content.
- `curriculum_sources`: source provenance.

Row Level Security is enabled on all ten tables. The Android app can read published catalogue/content only. No client-side write policies are present; privileged data changes must go through a trusted server-side workflow, not a service key in the APK.

## Current content coverage

The grade and learning-area catalogue is seeded for PP1–Grade 12, using the KICD regular curriculum index and grade-specific subject catalogues. Detailed Grade 4 Agriculture and Nutrition content remains available: 4 strands, 13 sub-strands and 39 paraphrased learning outcomes. Additional published starter packs have now been added for Grades 1, 2, 3, 5, 6, 7, 8 and 9. Current published counts are:

| Grade | Strands | Sub-strands | Learning outcomes |
|---|---:|---:|---:|
| Grade 1 | 4 | 8 | 16 |
| Grade 2 | 4 | 8 | 16 |
| Grade 3 | 4 | 8 | 16 |
| Grade 4 | 4 | 13 | 39 |
| Grade 5 | 6 | 12 | 24 |
| Grade 6 | 6 | 12 | 24 |
| Grade 7 | 5 | 10 | 20 |
| Grade 8 | 5 | 10 | 20 |
| Grade 9 | 5 | 10 | 20 |

The new packs include age-banded starter topics, learning outcomes, inquiry prompts and suggested practical activities across the learning areas available for each grade. They are paraphrased learning-practice material and are **not a complete or independently verified reproduction of KICD curriculum designs**. A curriculum editor should validate each grade/learning-area pack against the current KICD design before relying on it for formal teaching or assessment. PP1, PP2 and Grades 10–12 still need their own detailed, pathway-aware packs. Assessment rubric rows remain unpublished pending teacher review. Do not represent catalogue entries as complete lesson content.

## Source references

- [KICD regular curriculum designs](https://kicd.ac.ke/cbc-materials/curriculum-designs/regular-curriculum-designs/)
- [KICD Grade 9 curriculum designs](https://kicd.ac.ke/cbc-materials/curriculum-designs/grade-nine-designs/)
- [KICD Grade 10 curriculum designs](https://kicd.ac.ke/cbc-materials/curriculum-designs/grade-ten/)
- [KICD Grade 4 Agriculture and Nutrition design (revised 2024)](https://kicd.ac.ke/wp-content/uploads/2024/02/GRADE.4.AGRICULTURE.NUTRITION.16.1.2024.pdf)

## Android behavior

`SupabaseCurriculumRepository` requests published learning areas and curriculum details through the Supabase REST API on a background thread. The Home screen shows connection status and can display strand/sub-strand/outcome content. If the network is unavailable, the app labels the bundled Grade 4 fallback as sample content rather than live official curriculum.
