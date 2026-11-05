import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";

const headers = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
};
type Obj = Record<string, unknown>;
const reply = (status: number, body: Obj) =>
  new Response(JSON.stringify(body), { status, headers });
const str = (value: unknown, max = 2000) =>
  typeof value === "string" ? value.trim().slice(0, max) : "";
const arr = (value: unknown): unknown[] => Array.isArray(value) ? value : [];

function validatePack(value: unknown) {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    throw new Error("The AI returned an invalid lesson pack.");
  }
  const p = value as Obj;
  const title = str(p.lesson_title, 180);
  const introduction = str(p.introduction, 5000);
  const sections = arr(p.sections).slice(0, 8).map((raw) => {
    if (!raw || typeof raw !== "object" || Array.isArray(raw)) throw new Error("Invalid lesson section.");
    const s = raw as Obj;
    const heading = str(s.heading, 180);
    const explanation = str(s.explanation, 5000);
    const examples = arr(s.examples).map(v => str(v, 1000)).filter(Boolean).slice(0, 8);
    if (!heading || !explanation) throw new Error("Every lesson section needs a heading and explanation.");
    return { heading, explanation, examples };
  });
  const outcomes = arr(p.outcomes).map(v => str(v, 1000)).filter(Boolean).slice(0, 10);
  const inquiry = arr(p.inquiry_questions).map(v => str(v, 1000)).filter(Boolean).slice(0, 8);
  const activities = arr(p.learning_activities).map((raw) => {
    if (!raw || typeof raw !== "object" || Array.isArray(raw)) return null;
    const a = raw as Obj;
    return {
      title: str(a.title, 180),
      instructions: str(a.instructions, 3000),
      resources: arr(a.resources).map(v => str(v, 200)).filter(Boolean).slice(0, 10),
      evidence: str(a.evidence, 500),
    };
  }).filter((v) => v && v.title && v.instructions).slice(0, 8);
  const vocabulary = arr(p.key_vocabulary).map((raw) => {
    if (!raw || typeof raw !== "object" || Array.isArray(raw)) return null;
    const v = raw as Obj;
    return { term: str(v.term, 100), meaning: str(v.meaning, 600) };
  }).filter((v) => v && v.term && v.meaning).slice(0, 15);
  const misconceptions = arr(p.common_misconceptions).map(v => str(v, 800)).filter(Boolean).slice(0, 8);
  const assessment = arr(p.formative_checks).map(v => str(v, 1000)).filter(Boolean).slice(0, 8);
  const summary = arr(p.key_takeaways).map(v => str(v, 800)).filter(Boolean).slice(0, 8);
  const task = str(p.digital_task, 2000);
  if (!title || !introduction || sections.length < 1 || outcomes.length < 1 ||
      inquiry.length < 1 || activities.length < 1 || summary.length < 1) {
    throw new Error("The generated lesson pack is incomplete. Please retry after checking the curriculum outcomes.");
  }
  return {
    lesson_title: title, introduction, outcomes, sections,
    key_vocabulary: vocabulary, inquiry_questions: inquiry,
    learning_activities: activities, common_misconceptions: misconceptions,
    formative_checks: assessment, digital_task: task, key_takeaways: summary,
  };
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers });
  if (req.method !== "POST") return reply(405, { error: "Use POST." });
  const authHeader = req.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) return reply(401, { error: "Sign-in is required." });

  const url = Deno.env.get("SUPABASE_URL");
  const anon = Deno.env.get("SUPABASE_ANON_KEY");
  const service = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const openAiKey = Deno.env.get("OPENAI_API_KEY");
  const model = Deno.env.get("OPENAI_MODEL") || "gpt-5.1";
  if (!url || !anon || !service) return reply(500, { error: "Server configuration is incomplete." });
  if (!openAiKey) return reply(503, { error: "AI content generation is not configured on the server." });

  const token = authHeader.slice(7);
  const caller = createClient(url, anon, { auth: { persistSession: false }, global: { headers: { Authorization: `Bearer ${token}` } } });
  const admin = createClient(url, service, { auth: { persistSession: false } });
  const { data: auth, error: authError } = await caller.auth.getUser(token);
  if (authError || !auth.user) return reply(401, { error: "Your session is invalid or expired." });
  let body: Obj;
  try { body = await req.json(); } catch { return reply(400, { error: "Request body must be valid JSON." }); }

  const gradeId = str(body.grade_id, 20).toLowerCase();
  const areaId = str(body.learning_area_id, 120);
  const strandId = str(body.strand_id, 80);
  const subId = str(body.sub_strand_id, 80);
  const language = str(body.language || "en", 16);
  if (!gradeId || !areaId || !strandId || !subId) {
    return reply(400, { error: "Grade, learning area, strand and sub-strand are required." });
  }

  const { data: roles, error: rolesError } = await admin
    .from("user_role_assignments").select("role_key,school_id").eq("user_id", auth.user.id);
  if (rolesError) return reply(500, { error: "Unable to verify curriculum permissions." });
  const permitted = (roles ?? []).some((r) =>
    ["teacher", "curriculum_editor", "platform_admin"].includes(r.role_key));
  if (!permitted) return reply(403, { error: "Only teachers and curriculum administrators can generate lesson content." });

  const oneHourAgo = new Date(Date.now() - 60 * 60 * 1000).toISOString();
  const { count: recentGenerations, error: limitError } = await admin
    .from("ai_substrand_content")
    .select("id", { count: "exact", head: true })
    .eq("generated_by", auth.user.id)
    .gte("created_at", oneHourAgo);
  if (limitError) return reply(500, { error: "Unable to check your content-generation limit." });
  if ((recentGenerations ?? 0) >= 5) {
    return reply(429, { error: "You have reached the limit of five generated sub-strand lesson drafts per hour. Try again later." });
  }

  const [gradeQ, areaQ, strandQ, subQ, outcomesQ, inquiriesQ, activitiesQ] = await Promise.all([
    admin.from("curriculum_grades").select("id,name").eq("id", gradeId).maybeSingle(),
    admin.from("learning_areas").select("id,name,kiswahili_name").eq("id", areaId).maybeSingle(),
    admin.from("curriculum_strands").select("id,grade_id,learning_area_id,title,description,content_status,is_published").eq("id", strandId).maybeSingle(),
    admin.from("curriculum_sub_strands").select("id,strand_id,title,description,is_published").eq("id", subId).maybeSingle(),
    admin.from("learning_outcomes").select("outcome_text,sequence").eq("sub_strand_id", subId).eq("is_published", true).order("sequence"),
    admin.from("inquiry_questions").select("question_text,sequence").eq("sub_strand_id", subId).eq("is_published", true).order("sequence"),
    admin.from("learning_activities").select("activity_text,sequence").eq("sub_strand_id", subId).eq("is_published", true).order("sequence"),
  ]);
  if ([gradeQ, areaQ, strandQ, subQ, outcomesQ, inquiriesQ, activitiesQ].some(q => q.error)) {
    return reply(500, { error: "Unable to load the selected published curriculum records." });
  }
  const grade = gradeQ.data, area = areaQ.data, strand = strandQ.data, sub = subQ.data;
  const outcomes = outcomesQ.data ?? [], inquiries = inquiriesQ.data ?? [], activities = activitiesQ.data ?? [];
  if (!grade || !area || !strand || !sub || strand.grade_id !== gradeId ||
      strand.learning_area_id !== areaId || sub.strand_id !== strandId ||
      !strand.is_published || strand.content_status !== "published" || !sub.is_published) {
    return reply(400, { error: "The selected grade → learning area → strand → sub-strand path does not match the published curriculum." });
  }
  if (!outcomes.length) return reply(409, { error: "This sub-strand has no published learning outcomes. Add and review the correct outcomes before generating lesson content." });

  const sourceSnapshot = {
    grade: grade.name, learning_area: area.name,
    strand: { title: strand.title, description: strand.description ?? "" },
    sub_strand: { title: sub.title, description: sub.description ?? "" },
    published_learning_outcomes: outcomes.map(x => x.outcome_text),
    published_inquiry_questions: inquiries.map(x => x.question_text),
    published_learning_activities: activities.map(x => x.activity_text),
  };

  const systemPrompt = `You are a Kenyan CBC curriculum lesson designer. Generate a detailed, accurate lesson pack for ONLY the exact selected sub-strand. The hierarchy and scope are strict: grade → learning area → main strand → sub-strand. The main strand defines the conceptual domain; the sub-strand defines the precise lesson scope. Never substitute a neighbouring sub-strand, broaden into the whole strand, or infer a different subject. Use the supplied published learning outcomes as the required learning targets; explain them in learner-friendly language without changing their meaning. If the supplied outcomes conflict with the sub-strand title or strand context, do not invent a correction: return a clear content_conflict in JSON. Adapt vocabulary, depth, examples, task complexity and scaffolding to the stated grade. Use Kenyan home, school and community contexts where relevant, but do not invent statistics or claim KICD endorsement. Do not invent official curriculum outcomes, citations, textbook page numbers or source references. Build coherent teaching explanations, subject-specific worked examples, practical activities, formative checks, likely misconceptions and a short digital/portfolio task only where suitable. All activities must directly teach or assess the supplied outcomes. For languages, use the requested language. Return valid JSON only with keys: lesson_title, introduction, outcomes (array), sections (array of {heading,explanation,examples}), key_vocabulary (array of {term,meaning}), inquiry_questions (array), learning_activities (array of {title,instructions,resources,evidence}), common_misconceptions (array), formative_checks (array), digital_task, key_takeaways, content_conflict. Do not include markdown.`;

  try {
    const response = await fetch("https://api.openai.com/v1/chat/completions", {
      method: "POST",
      headers: { Authorization: `Bearer ${openAiKey}`, "Content-Type": "application/json" },
      signal: AbortSignal.timeout(90000),
      body: JSON.stringify({
        model, max_completion_tokens: 7000, response_format: { type: "json_object" },
        messages: [
          { role: "system", content: systemPrompt },
          { role: "user", content: JSON.stringify({ curriculum: sourceSnapshot, language, grade_level: grade.name }) },
        ],
      }),
    });
    if (!response.ok) {
      console.error("Sub-strand content provider error", response.status, (await response.text()).slice(0, 800));
      return reply(502, { error: "The AI provider could not complete this lesson request." });
    }
    const completion = await response.json();
    const raw = completion.choices?.[0]?.message?.content;
    if (typeof raw !== "string") throw new Error("The AI provider returned no lesson content.");
    const parsedRaw = JSON.parse(raw) as Obj;
    if (str(parsedRaw.content_conflict, 1000)) {
      return reply(409, { error: str(parsedRaw.content_conflict, 1000) });
    }
    const pack = validatePack(parsedRaw);
    // Preserve the exact published learning outcomes; the model may explain them but never rewrite them.
    pack.outcomes = outcomes.map((row) => row.outcome_text);
    const { data: saved, error: saveError } = await admin.from("ai_substrand_content").insert({
      grade_id: gradeId, learning_area_id: areaId, strand_id: strandId, sub_strand_id: subId,
      generated_by: auth.user.id, model, content_payload: pack,
      source_snapshot: sourceSnapshot, review_status: "pending_review",
    }).select("id,review_status,created_at").single();
    if (saveError || !saved) throw new Error("The generated lesson could not be saved for curriculum review.");
    return reply(200, {
      content_id: saved.id, review_status: saved.review_status, generated_at: saved.created_at,
      content: pack, source: sourceSnapshot,
      message: "Generated from the selected published sub-strand and saved for curriculum review.",
    });
  } catch (error) {
    console.error("Sub-strand generation failed", error);
    return reply(422, { error: error instanceof Error ? error.message : "Sub-strand generation failed." });
  }
});
