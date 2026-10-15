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

The grade and learning-area catalogue is seeded for PP1–Grade 12, using the KICD regular curriculum index and grade-specific subject catalogues. Detailed content currently loaded and published is the Grade 4 Agriculture and Nutrition design: 4 strands, 13 sub-strands, 39 paraphrased learning outcomes, 13 inquiry prompts and 13 suggested learning activities. Assessment rubric rows are present but intentionally unpublished pending teacher review.

**This is not yet a complete, verified strand-by-strand curriculum for every subject and grade.** Subject names and grade/pathway availability should be checked against each current KICD design before publishing additional detailed content. Do not represent catalogue entries as complete lesson content.

## Source references

- [KICD regular curriculum designs](https://kicd.ac.ke/cbc-materials/curriculum-designs/regular-curriculum-designs/)
- [KICD Grade 9 curriculum designs](https://kicd.ac.ke/cbc-materials/curriculum-designs/grade-nine-designs/)
- [KICD Grade 10 curriculum designs](https://kicd.ac.ke/cbc-materials/curriculum-designs/grade-ten/)
- [KICD Grade 4 Agriculture and Nutrition design (revised 2024)](https://kicd.ac.ke/wp-content/uploads/2024/02/GRADE.4.AGRICULTURE.NUTRITION.16.1.2024.pdf)

## Android behavior

`SupabaseCurriculumRepository` requests published learning areas and curriculum details through the Supabase REST API on a background thread. The Home screen shows connection status and can display strand/sub-strand/outcome content. If the network is unavailable, the app labels the bundled Grade 4 fallback as sample content rather than live official curriculum.
