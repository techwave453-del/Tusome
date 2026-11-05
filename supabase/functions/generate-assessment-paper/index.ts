import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
};

type JsonObject = Record<string, unknown>;
const ASSESSMENT_TYPES = new Set(["diagnostic", "formative", "summative", "sba", "practice"]);
const QUESTION_TYPES = new Set([
  "multiple_choice", "true_false", "short_answer", "long_answer", "structured",
  "essay", "matching", "table_response", "practical", "performance",
  "fill_blank", "numeric", "diagram_label", "observation", "portfolio",
]);

function reply(status: number, body: JsonObject) {
  return new Response(JSON.stringify(body), { status, headers: corsHeaders });
}
function textValue(value: unknown, max = 2000): string {
  return typeof value === "string" ? value.trim().slice(0, max) : "";
}
function safeArray(value: unknown): unknown[] {
  return Array.isArray(value) ? value : [];
}
function validateGeneratedPaper(value: unknown) {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    throw new Error("The AI returned an invalid assessment object.");
  }
  const paper = value as JsonObject;
  const title = textValue(paper.title, 180);
  const instructions = textValue(paper.instructions, 5000);
  const sections = safeArray(paper.sections);
  const questions = safeArray(paper.questions);
  const marking = safeArray(paper.marking_guide);
  const performance = safeArray(paper.performance_level_grid);
  if (!title || !instructions || sections.length < 1 || sections.length > 8 ||
      questions.length < 1 || questions.length > 80) {
    throw new Error("The generated paper is missing its title, instructions, sections or questions.");
  }
  const sectionIds = new Set<string>();
  const normalizedSections = sections.map((raw, index) => {
    if (!raw || typeof raw !== "object" || Array.isArray(raw)) throw new Error("Invalid assessment section.");
    const item = raw as JsonObject;
    const id = textValue(item.id, 80) || `section-${index + 1}`;
    const sectionTitle = textValue(item.title, 180);
    if (!sectionTitle || sectionIds.has(id)) throw new Error("Every assessment section needs a unique ID and title.");
    sectionIds.add(id);
    return {
      id,
      title: sectionTitle,
      instructions: textValue(item.instructions, 2000),
      total_marks: Number(item.total_marks) || 0,
      question_ids: safeArray(item.question_ids).map((id) => textValue(id, 80)).filter(Boolean),
    };
  });

  let calculatedMarks = 0;
  const normalizedQuestions = questions.map((raw, index) => {
    if (!raw || typeof raw !== "object" || Array.isArray(raw)) throw new Error("Invalid assessment question.");
    const item = raw as JsonObject;
    const id = textValue(item.id, 80) || `q-${index + 1}`;
    const type = textValue(item.question_type, 40).toLowerCase();
    const questionText = textValue(item.question_text, 6000);
    const marks = Number(item.marks);
    if (!questionText || !QUESTION_TYPES.has(type) || !Number.isFinite(marks) || marks <= 0) {
      throw new Error(`Question ${index + 1} is missing valid text, type or marks.`);
    }
    const sectionId = textValue(item.section_id, 80) || normalizedSections[0].id;
    if (!sectionIds.has(sectionId)) throw new Error(`Question ${index + 1} refers to an unknown section.`);
    const options = safeArray(item.options).map((option) => textValue(option, 500)).filter(Boolean);
    if (type === "multiple_choice" && options.length < 2) {
      throw new Error(`Question ${index + 1} needs at least two options.`);
    }
    const subQuestions = safeArray(item.sub_questions).map((rawSub, subIndex) => {
      if (!rawSub || typeof rawSub !== "object" || Array.isArray(rawSub)) {
        throw new Error(`Question ${index + 1} contains an invalid sub-question.`);
      }
      const sub = rawSub as JsonObject;
      const subText = textValue(sub.question_text, 3000);
      const subMarks = Number(sub.marks);
      if (!subText || !Number.isFinite(subMarks) || subMarks <= 0) {
        throw new Error(`Question ${index + 1}, part ${subIndex + 1} is incomplete.`);
      }
      return {
        id: textValue(sub.id, 80) || `${id}-${subIndex + 1}`,
        label: textValue(sub.label, 20) || `(${String.fromCharCode(97 + subIndex)})`,
        question_text: subText,
        question_type: QUESTION_TYPES.has(textValue(sub.question_type, 40))
          ? textValue(sub.question_type, 40) : "short_answer",
        marks: subMarks,
        options: safeArray(sub.options).map((option) => textValue(option, 500)).filter(Boolean),
      };
    });
    const questionMarks = subQuestions.length
      ? subQuestions.reduce((sum, sub) => sum + sub.marks, 0)
      : marks;
    if (subQuestions.length && Math.abs(questionMarks - marks) > 0.01) {
      throw new Error(`Marks for question ${index + 1} do not equal the sum of its sub-questions.`);
    }
    calculatedMarks += questionMarks;
    const tableData = item.table_data && typeof item.table_data === "object"
      ? item.table_data as JsonObject : null;
    return {
      id,
      section_id: sectionId,
      question_number: textValue(item.question_number, 30) || String(index + 1),
      question_type: type,
      question_text: questionText,
      stimulus: textValue(item.stimulus, 6000),
      image_alt: textValue(item.image_alt, 500),
      image_url: "", // Assessment images must be reviewed and uploaded to Tusome storage separately.
      marks: questionMarks,
      options,
      sub_questions: subQuestions,
      table_data: tableData,
      competency_tags: safeArray(item.competency_tags).map((tag) => textValue(tag, 80)).filter(Boolean),
      difficulty: textValue(item.difficulty, 40) || "age_appropriate",
    };
  });
  if (!Number.isFinite(calculatedMarks) || calculatedMarks <= 0) {
    throw new Error("The generated paper has no valid mark allocation.");
  }
  const requestedMarks = Number(paper.total_marks);
  if (Number.isFinite(requestedMarks) && requestedMarks > 0 &&
      Math.abs(requestedMarks - calculatedMarks) > 0.01) {
    throw new Error("The total marks do not match the marks allocated to the questions.");
  }
  for (const section of normalizedSections) {
    const sectionMarks = normalizedQuestions
      .filter((question) => question.section_id === section.id)
      .reduce((sum, question) => sum + question.marks, 0);
    if (section.total_marks > 0 && Math.abs(section.total_marks - sectionMarks) > 0.01) {
      throw new Error(`Section ${section.title} has an incorrect mark total.`);
    }
  }
  return {
    title,
    description: textValue(paper.description, 1200),
    instructions,
    duration_minutes: Math.max(5, Math.min(240, Math.round(Number(paper.duration_minutes) || 60))),
    total_marks: calculatedMarks,
    sections: normalizedSections,
    questions: normalizedQuestions,
    marking_guide: marking.slice(0, 100),
    performance_level_grid: performance.slice(0, 40),
    learner_detail_fields: safeArray(paper.learner_detail_fields)
      .map((field) => textValue(field, 80)).filter(Boolean).slice(0, 12),
    competency_tags: safeArray(paper.competency_tags)
      .map((tag) => textValue(tag, 80)).filter(Boolean).slice(0, 20),
  };
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return reply(405, { error: "Use POST." });
  const authorization = req.headers.get("Authorization");
  if (!authorization?.startsWith("Bearer ")) return reply(401, { error: "Sign-in is required." });

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const openAiKey = Deno.env.get("OPENAI_API_KEY");
  const model = Deno.env.get("OPENAI_MODEL") || "gpt-5.1";
  if (!supabaseUrl || !anonKey || !serviceKey) return reply(500, { error: "Server configuration is incomplete." });
  if (!openAiKey) return reply(503, { error: "Assessment generation is not configured on the server." });

  const token = authorization.slice("Bearer ".length);
  const caller = createClient(supabaseUrl, anonKey, {
    auth: { persistSession: false, autoRefreshToken: false },
    global: { headers: { Authorization: `Bearer ${token}` } },
  });
  const admin = createClient(supabaseUrl, serviceKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: authData, error: authError } = await caller.auth.getUser(token);
  if (authError || !authData.user) return reply(401, { error: "Your session is invalid or expired." });
  const actorId = authData.user.id;

  let body: JsonObject;
  try { body = await req.json(); } catch { return reply(400, { error: "Request body must be valid JSON." }); }

  const gradeId = textValue(body.grade_id, 20).toLowerCase();
  const learningAreaId = textValue(body.learning_area_id, 120);
  const strandId = textValue(body.strand_id, 80);
  const subStrandId = textValue(body.sub_strand_id, 80);
  const assessmentType = textValue(body.assessment_type || "practice", 30);
  const languageCode = textValue(body.language_code || "en", 16).toLowerCase();
  const requestedMinutes = Number(body.duration_minutes || 60);
  const requestedCount = Number(body.question_count || 20);
  const termNumber = Number(body.term_number || 0);
  const trialNumber = Number(body.trial_number || 0);
  const pathway = textValue(body.pathway, 100);
  const academicYear = Number(body.academic_year || new Date().getUTCFullYear());
  const difficulty = textValue(body.difficulty || "age_appropriate", 80);
  const localContext = textValue(body.local_context || "Use familiar Kenyan home, school and community contexts.", 1200);
  const additionalInstructions = textValue(body.additional_instructions, 2000);
  const requestedOutcomes = safeArray(body.learning_outcomes)
    .map((value) => textValue(value, 1000)).filter(Boolean);

  if (!gradeId || !learningAreaId || !strandId || !subStrandId ||
      !ASSESSMENT_TYPES.has(assessmentType) ||
      !/^[a-z]{2,3}(-[a-z0-9]{2,8})?$/.test(languageCode) ||
      !Number.isInteger(requestedMinutes) || requestedMinutes < 5 || requestedMinutes > 240 ||
      !Number.isInteger(requestedCount) || requestedCount < 1 || requestedCount > 80 ||
      (termNumber !== 0 && (termNumber < 1 || termNumber > 3)) ||
      (trialNumber !== 0 && (trialNumber < 1 || trialNumber > 20))) {
    return reply(400, { error: "Choose a valid grade, learning area, curriculum node and assessment specification." });
  }

  const [{ data: assignments, error: rolesError }, { data: profile, error: profileError }] =
    await Promise.all([
      admin.from("user_role_assignments").select("role_key,school_id").eq("user_id", actorId),
      admin.from("user_profiles").select("school_id").eq("user_id", actorId).maybeSingle(),
    ]);
  if (rolesError || profileError) return reply(500, { error: "Unable to verify assessment permissions." });
  const roles = assignments ?? [];
  const globalAdmin = roles.some((role) => role.role_key === "platform_admin" && role.school_id === null);
  const globalEditor = roles.some((role) => role.role_key === "curriculum_editor" && role.school_id === null);
  const schoolId = profile?.school_id ?? null;
  const schoolTeacher = !!schoolId && roles.some((role) =>
    role.role_key === "teacher" && role.school_id === schoolId);
  if (!globalAdmin && !globalEditor && !schoolTeacher) {
    return reply(403, { error: "Only an authorised teacher, curriculum editor or platform administrator can generate assessments." });
  }

  const hourAgo = new Date(Date.now() - 60 * 60 * 1000).toISOString();
  const [{ count: recentCount }, { count: activeCount }] = await Promise.all([
    admin.from("assessment_generation_jobs").select("id", { count: "exact", head: true })
      .eq("requested_by", actorId).gte("created_at", hourAgo),
    admin.from("assessment_generation_jobs").select("id", { count: "exact", head: true })
      .eq("requested_by", actorId).in("status", ["queued", "running"]),
  ]);
  if ((recentCount ?? 0) >= 10) return reply(429, { error: "Assessment generation limit reached. Try again later." });
  if ((activeCount ?? 0) >= 2) return reply(429, { error: "You already have two assessment generation jobs running." });

  const [gradeResult, areaResult, strandResult, subResult, assignmentResult] = await Promise.all([
    admin.from("curriculum_grades").select("id,name").eq("id", gradeId).maybeSingle(),
    admin.from("learning_areas").select("id,name,kiswahili_name").eq("id", learningAreaId).maybeSingle(),
    admin.from("curriculum_strands").select("id,grade_id,learning_area_id,title,description,content_status,is_published")
      .eq("id", strandId).maybeSingle(),
    admin.from("curriculum_sub_strands").select("id,strand_id,title,description,is_published")
      .eq("id", subStrandId).maybeSingle(),
    admin.from("grade_learning_areas").select("learning_area_id")
      .eq("grade_id", gradeId).eq("learning_area_id", learningAreaId).eq("is_published", true).maybeSingle(),
  ]);
  if (gradeResult.error || areaResult.error || strandResult.error || subResult.error || assignmentResult.error) {
    return reply(500, { error: "Unable to validate the selected CBC curriculum path." });
  }
  const grade = gradeResult.data, area = areaResult.data, strand = strandResult.data, sub = subResult.data;
  if (!grade || !area || !strand || !sub || !assignmentResult.data ||
      strand.grade_id !== gradeId || strand.learning_area_id !== learningAreaId ||
      sub.strand_id !== strandId || !strand.is_published ||
      strand.content_status !== "published" || !sub.is_published) {
    return reply(400, { error: "The selected grade, learning area, strand or sub-strand is not a matching published CBC curriculum node." });
  }

  const [outcomeResult, inquiryResult, activityResult] = await Promise.all([
    admin.from("learning_outcomes").select("outcome_text,sequence")
      .eq("sub_strand_id", subStrandId).eq("is_published", true).order("sequence"),
    admin.from("inquiry_questions").select("question_text,sequence")
      .eq("sub_strand_id", subStrandId).eq("is_published", true).order("sequence"),
    admin.from("learning_activities").select("activity_text,sequence")
      .eq("sub_strand_id", subStrandId).eq("is_published", true).order("sequence"),
  ]);
  if (outcomeResult.error || inquiryResult.error || activityResult.error) {
    return reply(500, { error: "Unable to load the published CBC learning outcomes." });
  }
  const officialOutcomes = (outcomeResult.data ?? []).map((row) => row.outcome_text).filter(Boolean);
  if (!officialOutcomes.length) return reply(409, { error: "This sub-strand has no published learning outcomes. Add and review its outcomes before generating an assessment." });
  if (requestedOutcomes.some((outcome) => !officialOutcomes.includes(outcome))) {
    return reply(400, { error: "Only published learning outcomes for this sub-strand can be assessed." });
  }
  const selectedOutcomes = requestedOutcomes.length ? [...new Set(requestedOutcomes)] : officialOutcomes;

  const requestPayload = {
    grade_id: gradeId, learning_area_id: learningAreaId, strand_id: strandId,
    sub_strand_id: subStrandId, assessment_type: assessmentType,
    language_code: languageCode, duration_minutes: requestedMinutes,
    question_count: requestedCount, term_number: termNumber || null,
    trial_number: trialNumber || null, pathway, academic_year: academicYear,
    difficulty, local_context: localContext, learning_outcomes: selectedOutcomes,
  };
  const { data: job, error: jobError } = await admin.from("assessment_generation_jobs").insert({
    requested_by: actorId, school_id: globalAdmin || globalEditor ? null : schoolId,
    request_payload: requestPayload, provider: "openai", model, status: "running",
    started_at: new Date().toISOString(),
  }).select("id").single();
  if (jobError || !job) return reply(500, { error: "Unable to start the assessment generation job." });

  try {
    const systemPrompt = `You are a senior Kenyan CBC assessment designer and experienced classroom examiner.
Create an original, high-quality assessment for the exact published grade, learning area, strand, sub-strand and learning outcomes supplied by Tusome.

REFERENCE FORMAT:
Use the uploaded Grade 10 revision-trials guide only as a structural reference: learner instructions, timed papers, named sections, mark allocations, numbered questions and sub-questions, varied response formats, practical/performance tasks where appropriate, and a performance-level grid where appropriate. Do not copy its questions, wording, logos, branding or purported official status.

CURRICULUM RULES:
- The supplied published CBC learning outcomes are authoritative. Every question must assess at least one supplied outcome.
- Respect the learner's actual grade. Do not use Grade 10 complexity or terminology for PP1–Grade 9, and do not simplify senior-school pathways inappropriately.
- For PP1–Grade 3 use short, concrete, accessible prompts, oral/observation/practical responses and age-appropriate mark allocations.
- For Grades 4–6 use clear written and practical tasks, familiar local contexts, and a balanced range of recall, application and reasoning.
- For Grades 7–9 use inquiry, problem-solving, practical/project evidence and subject-specific reasoning.
- For Grades 10–12 respect the selected pathway and learning area; include relevant theory, application and practical/performance evidence.
- Use diverse question types: multiple choice, true/false, short answer, structured parts, extended response, matching, tables, diagram interpretation/labels, practical tasks and portfolio/performance evidence as suitable. Never force a type that does not fit the subject.
- Use familiar Kenyan contexts without inventing statistics or falsely claiming official KICD/KNEC endorsement.
- Questions must be original, unambiguous, age-appropriate, assessable and aligned to the selected outcomes. Avoid repeated questions and trick wording.
- Ensure every mark is accounted for. A parent question with sub-questions must have marks equal to the sum of its sub-question marks. Section totals must equal the marks in questions assigned to that section. Paper total must equal all question marks.
- Generate a separate marking guide with acceptable answer points and mark allocation. Do not reveal the marking guide in learner-facing instructions.
- Generate CBC performance-level descriptors only when appropriate and make them criterion-specific. Do not mechanically assign EE/ME/AE/BE to every subject or age group.
- If an image/diagram is required, describe it in stimulus and image_alt; do not invent external image URLs. The diagram can be added and reviewed by an editor.
- Respond in the requested language. For Kikamba, do not substitute Kikuyu; if uncertain about a technical term, retain the English term and explain it.
- Return valid JSON only, with no Markdown fences.

OUTPUT JSON:
{
 "title":"...", "description":"...", "instructions":"...",
 "duration_minutes":60, "total_marks":40,
 "sections":[{"id":"section-a","title":"SECTION A","instructions":"...","total_marks":20}],
 "questions":[{
  "id":"q1","section_id":"section-a","question_number":"1",
  "question_type":"multiple_choice|true_false|short_answer|long_answer|structured|essay|matching|table_response|practical|performance|fill_blank|numeric|diagram_label|observation|portfolio",
  "question_text":"...", "stimulus":"optional shared scenario",
  "image_alt":"optional description of diagram needed", "marks":2,
  "options":["..."], "sub_questions":[{"id":"q1a","label":"(a)","question_text":"...","question_type":"short_answer","marks":2,"options":[]}],
  "table_data":{"headers":["..."],"rows":[["..."]]},
  "competency_tags":["critical_thinking"], "difficulty":"age_appropriate"
 }],
 "marking_guide":[{"question_id":"q1","suggested_answer":"...","marking_notes":"...","marks":2}],
 "performance_level_grid":[{"criterion":"...","exceeds_expectation":"...","meets_expectation":"...","approaching_expectation":"...","below_expectation":"..."}],
 "learner_detail_fields":["Learner name","School","Date"],
 "competency_tags":["communication","critical_thinking"]
}

Produce approximately ${requestedCount} numbered questions and ${requestedMinutes} minutes of work. Requested assessment type: ${assessmentType}. Difficulty: ${difficulty}. Language: ${languageCode}. Local context: ${localContext}.
Additional examiner instructions: ${additionalInstructions || "None"}.`;

    const curriculumContext = {
      grade: grade.name, learning_area: area.name, pathway,
      strand: strand.title, strand_description: strand.description ?? "",
      sub_strand: sub.title, sub_strand_description: sub.description ?? "",
      official_published_learning_outcomes: selectedOutcomes,
      inquiry_questions: (inquiryResult.data ?? []).map((row) => row.question_text),
      learning_activities: (activityResult.data ?? []).map((row) => row.activity_text),
    };
    const aiResponse = await fetch("https://api.openai.com/v1/chat/completions", {
      method: "POST",
      headers: { "Authorization": `Bearer ${openAiKey}`, "Content-Type": "application/json" },
      signal: AbortSignal.timeout(90_000),
      body: JSON.stringify({
        model, max_completion_tokens: 10000,
        response_format: { type: "json_object" },
        messages: [
          { role: "system", content: systemPrompt },
          { role: "user", content: JSON.stringify({ curriculum: curriculumContext, specification: requestPayload }) },
        ],
      }),
    });
    if (!aiResponse.ok) {
      const errorBody = await aiResponse.text();
      console.error("Assessment generation provider error", aiResponse.status, errorBody.slice(0, 1000));
      throw new Error("The assessment provider could not complete this request.");
    }
    const completion = await aiResponse.json();
    const rawContent = completion.choices?.[0]?.message?.content;
    if (typeof rawContent !== "string") throw new Error("The assessment provider returned no content.");
    const parsed = validateGeneratedPaper(JSON.parse(rawContent));
    const paperCode = `TUS-${gradeId.toUpperCase()}-${Date.now()}`;
    const paperMetadata = {
      assessment_series: "Tusome CBC practice assessment",
      generated_at: new Date().toISOString(),
      model, term_number: termNumber || null, trial_number: trialNumber || null,
      pathway, academic_year: academicYear, source_note: "AI-generated draft aligned to published CBC learning outcomes; pending human review.",
    };
    const coverageMap = selectedOutcomes.map((outcome) => ({
      strand_id: strandId, sub_strand_id: subStrandId, learning_outcome: outcome,
    }));
    const { data: savedPaper, error: saveError } = await admin.from("assessment_papers").insert({
      paper_code: paperCode, grade_id: gradeId, learning_area_id: learningAreaId,
      strand_id: strandId, sub_strand_id: subStrandId,
      school_id: globalAdmin || globalEditor ? null : schoolId,
      assessment_type: assessmentType, title: parsed.title,
      description: parsed.description, instructions: parsed.instructions,
      duration_minutes: parsed.duration_minutes, total_marks: parsed.total_marks,
      question_payload: parsed.questions, marking_payload: parsed.marking_guide,
      sections_payload: parsed.sections, performance_level_grid: parsed.performance_level_grid,
      learner_detail_fields: parsed.learner_detail_fields, paper_metadata: paperMetadata,
      competency_tags: parsed.competency_tags, coverage_map: coverageMap,
      source_reference: "Published Tusome CBC curriculum outcomes",
      generation_method: "ai_generated", content_status: "draft", version: 1,
      created_by: actorId,
    }).select("id,paper_code,title,content_status,total_marks,duration_minutes").single();
    if (saveError || !savedPaper) throw new Error("The generated assessment could not be saved for review.");

    await admin.from("assessment_generation_jobs").update({
      paper_id: savedPaper.id, status: "completed", completed_at: new Date().toISOString(),
    }).eq("id", job.id);
    return reply(200, { paper: savedPaper, status: "draft", message: "Assessment generated and saved as a draft for teacher/editor review." });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Assessment generation failed.";
    await admin.from("assessment_generation_jobs").update({
      status: "failed", error_message: message.slice(0, 3000), completed_at: new Date().toISOString(),
    }).eq("id", job.id);
    return reply(422, { error: message });
  }
});
