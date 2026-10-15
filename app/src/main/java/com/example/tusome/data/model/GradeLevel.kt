package com.example.tusome.data.model

enum class GradeCategory(val displayName: String) {
    PRE_PRIMARY("Pre-Primary"),
    LOWER_PRIMARY("Lower Primary (Grade 1 - 3)"),
    UPPER_PRIMARY("Upper Primary (Grade 4 - 6)"),
    JUNIOR_SECONDARY("Junior School (Grade 7 - 9)"),
    SENIOR_SECONDARY("Senior School (Grade 10 - 12)")
}

enum class GradeLevel(
    val code: String,
    val title: String,
    val category: GradeCategory
) {
    PP1("PP1", "Pre-Primary 1", GradeCategory.PRE_PRIMARY),
    PP2("PP2", "Pre-Primary 2", GradeCategory.PRE_PRIMARY),
    GRADE_1("G1", "Grade 1", GradeCategory.LOWER_PRIMARY),
    GRADE_2("G2", "Grade 2", GradeCategory.LOWER_PRIMARY),
    GRADE_3("G3", "Grade 3", GradeCategory.LOWER_PRIMARY),
    GRADE_4("G4", "Grade 4", GradeCategory.UPPER_PRIMARY),
    GRADE_5("G5", "Grade 5", GradeCategory.UPPER_PRIMARY),
    GRADE_6("G6", "Grade 6", GradeCategory.UPPER_PRIMARY),
    GRADE_7("G7", "Grade 7", GradeCategory.JUNIOR_SECONDARY),
    GRADE_8("G8", "Grade 8", GradeCategory.JUNIOR_SECONDARY),
    GRADE_9("G9", "Grade 9", GradeCategory.JUNIOR_SECONDARY),
    GRADE_10("G10", "Grade 10", GradeCategory.SENIOR_SECONDARY),
    GRADE_11("G11", "Grade 11", GradeCategory.SENIOR_SECONDARY),
    GRADE_12("G12", "Grade 12", GradeCategory.SENIOR_SECONDARY)
}
