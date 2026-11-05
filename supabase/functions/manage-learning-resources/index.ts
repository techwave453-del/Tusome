import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
};

const RESOURCE_TYPES = new Set([
  "lesson_notes", "worked_example", "learner_activity", "worksheet", "quiz",
  "assessment", "marking_scheme", "revision_notes", "teacher_guide", "project",
]);

function reply(status: number, body: Record<string, unknown>) {
  return new Response(JSON.stringify(body), { status, headers: corsHeaders });
}

function cleanText(value: unknown, maxLength: number, required = false): string | null {
  if (typeof value !== "string") return required ? null : "";
  const text = value.trim();
  if (required && text.length === 0) return null;
  if (text.length > maxLength) return null;
  return text;
}

function validContent(value: unknown): value is { sections: Array<Record<string, unknown>> } {
  if (!value || typeof value !== "object" || Array.isArray(value)) return false;
  const sections = (value as Record<string, unknown>).sections;
  if (!Array.isArray(sections) || sections.length > 80) return false;
  let blockCount = 0;
  for (const section of sections) {
    if (!section || typeof section !== "object" || Array.isArray(section)) return false;
    const blocks = (section as Record<string, unknown>).blocks;
    if (!Array.isArray(blocks)) return false;
    blockCount += blocks.length;
    if (blockCount > 500) return false;
    for (const block of blocks) {
      if (!block || typeof block !== "object" || Array.isArray(block)) return false;
      const item = block as Record<string, unknown>;
      if (typeof item.type !== "string" || typeof item.text !== "string") return false;
      if (item.text.length > 20000) return false;
      if (item.items !== undefined &&
        (!Array.isArray(item.items) || item.items.length > 100 ||
          item.items.some((entry) => typeof entry !== "string" || entry.length > 4000))) {
        return false;
      }
      if (item.media_url !== undefined &&
        (typeof item.media_url !== "string" || item.media_url.length > 2000)) return false;
    }
  }
  return JSON.stringify(value).length <= 500_000;
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return reply(405, { error: "Use POST." });

  const authorization = req.headers.get("Authorization");
  if (!authorization?.startsWith("Bearer ")) return reply(401, { error: "Sign-in is required." });

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !anonKey || !serviceKey) {
    return reply(500, { error: "Server authentication configuration is incomplete." });
  }

  const token = authorization.slice("Bearer ".length);
  const caller = createClient(supabaseUrl, anonKey, {
    auth: { persistSession: false, autoRefreshToken: false },
    global: { headers: { Authorization: `Bearer ${token}` } },
  });
  const admin = createClient(supabaseUrl, serviceKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const { data: authData, error: authError } = await caller.auth.getUser(token);
  if (authError || !authData.user) {
    return reply(401, { error: "Your session is invalid or expired. Sign in again." });
  }
  const actorId = authData.user.id;

  let body: Record<string, unknown>;
  try {
    body = await req.json();
  } catch {
    return reply(400, { error: "Request body must be valid JSON." });
  }

  const action = String(body.action ?? "");
  const [{ data: assignments, error: rolesError }, { data: profile, error: profileError }] =
    await Promise.all([
      admin.from("user_role_assignments").select("role_key,school_id").eq("user_id", actorId),
      admin.from("user_profiles").select("school_id,grade_id").eq("user_id", actorId).maybeSingle(),
    ]);
  if (rolesError || profileError) {
    return reply(500, { error: "Unable to verify your learning-content permissions." });
  }

  const roles = assignments ?? [];
  const isPlatformAdmin = roles.some(
    (role) => role.role_key === "platform_admin" && role.school_id === null,
  );
  const isCurriculumEditor = roles.some(
    (role) => role.role_key === "curriculum_editor" && role.school_id === null,
  );
  const isGlobalEditor = isPlatformAdmin || isCurriculumEditor;
  const teacherSchoolIds = new Set(
    roles.filter((role) => role.role_key === "teacher" && role.school_id)
      .map((role) => role.school_id as string),
  );
  const profileSchoolId = profile?.school_id ?? null;
  const canCreateSchoolDraft = !!profileSchoolId && teacherSchoolIds.has(profileSchoolId);
  const canCreateGlobalDraft = isGlobalEditor;
  const canReview = isGlobalEditor;

  if (action === "create_draft") {
    if (!canCreateGlobalDraft && !canCreateSchoolDraft) {
      return reply(403, { error: "Only an authorised teacher or curriculum editor can create learning resources." });
    }

    const gradeId = cleanText(body.grade_id, 20, true);
    const learningAreaId = cleanText(body.learning_area_id, 120, true);
    const strandId = cleanText(body.strand_id, 80, true);
    const subStrandId = cleanText(body.sub_strand_id, 80, true);
    const resourceType = cleanText(body.resource_type, 40, true);
    const title = cleanText(body.title, 180, true);
    const summary = cleanText(body.summary, 1200) ?? "";
    const languageCode = cleanText(body.language_code ?? "en", 16, true);
    const content = body.content;
    const outcomes = Array.isArray(body.learning_outcomes)
      ? body.learning_outcomes.filter((value): value is string =>
          typeof value === "string" && value.trim().length > 0 && value.length <= 1000)
      : [];

    if (!gradeId || !learningAreaId || !strandId || !subStrandId || !resourceType ||
      !title || !languageCode || !RESOURCE_TYPES.has(resourceType) ||
      !/^[a-z]{2,3}(-[A-Za-z0-9]{2,8})?$/.test(languageCode) || !validContent(content)) {
      return reply(400, { error: "The resource details or structured content are invalid." });
    }
    if (outcomes.length > 30) return reply(400, { error: "A resource can include at most 30 learning outcomes." });

    const [
      { data: grade, error: gradeError },
      { data: strand, error: strandError },
      { data: subStrand, error: subStrandError },
    ] = await Promise.all([
      admin.from("curriculum_grades").select("id").eq("id", gradeId).maybeSingle(),
      admin.from("curriculum_strands")
        .select("id,grade_id,learning_area_id").eq("id", strandId).maybeSingle(),
      admin.from("curriculum_sub_strands")
        .select("id,strand_id").eq("id", subStrandId).maybeSingle(),
    ]);
    if (gradeError || strandError || subStrandError) {
      return reply(500, { error: "Unable to validate the selected CBC curriculum node." });
    }
    if (!grade || !strand || !subStrand || strand.grade_id !== gradeId ||
      strand.learning_area_id !== learningAreaId || subStrand.strand_id !== strandId) {
      return reply(400, { error: "The grade, learning area, strand and sub-strand do not match." });
    }

    const requestedSchoolId = body.school_id ? String(body.school_id) : null;
    const schoolId = isGlobalEditor ? requestedSchoolId : profileSchoolId;
    if (schoolId && schoolId !== profileSchoolId && !isPlatformAdmin) {
      return reply(403, { error: "You cannot create resources for another school." });
    }
    if (!isGlobalEditor && !schoolId) {
      return reply(403, { error: "Your teacher account must be linked to an active school." });
    }

    const minutes = body.estimated_minutes == null ? null : Number(body.estimated_minutes);
    if (minutes !== null && (!Number.isInteger(minutes) || minutes < 1 || minutes > 600)) {
      return reply(400, { error: "Estimated minutes must be between 1 and 600." });
    }

    const resourcePayload = {
      grade_id: gradeId,
      learning_area_id: learningAreaId,
      strand_id: strandId,
      sub_strand_id: subStrandId,
      school_id: schoolId,
      resource_type: resourceType,
      title,
      summary,
      language_code: languageCode,
      status: "draft",
      source: body.source === "teacher_created" ? "teacher_created" : "ai_generated",
      version: 1,
      estimated_minutes: minutes,
      learning_outcomes: outcomes,
      content,
      created_by: actorId,
    };
    const { data: resource, error: createError } = await admin
      .from("learning_resources").insert(resourcePayload).select("*").single();
    if (createError || !resource) {
      console.error("Learning resource draft insert failed:", createError?.message);
      return reply(500, { error: "The learning resource draft could not be saved." });
    }

    const { error: versionError } = await admin.from("learning_resource_versions").insert({
      resource_id: resource.id,
      version: 1,
      content,
      title,
      summary,
      learning_outcomes: outcomes,
      changed_by: actorId,
      change_summary: "Initial draft",
    });
    if (versionError) {
      await admin.from("learning_resources").delete().eq("id", resource.id);
      return reply(500, { error: "The draft could not be versioned and was rolled back." });
    }
    return reply(201, { resource });
  }

  if (action === "submit_for_review") {
    const resourceId = String(body.resource_id ?? "");
    if (!resourceId) return reply(400, { error: "A resource ID is required." });
    const { data: resource, error } = await admin.from("learning_resources")
      .select("id,status,created_by,school_id").eq("id", resourceId).maybeSingle();
    if (error || !resource) return reply(404, { error: "Learning resource not found." });
    const isOwner = resource.created_by === actorId;
    const isSchoolTeacher = !!resource.school_id && teacherSchoolIds.has(resource.school_id);
    if (!isGlobalEditor && !(isOwner && isSchoolTeacher)) {
      return reply(403, { error: "You cannot submit this resource for review." });
    }
    if (!["draft", "rejected"].includes(resource.status)) {
      return reply(409, { error: "Only draft or rejected resources can be submitted for review." });
    }
    const { data, error: updateError } = await admin.from("learning_resources")
      .update({ status: "pending_review", review_feedback: "" })
      .eq("id", resourceId).select("id,status,updated_at").single();
    if (updateError) return reply(500, { error: "The resource could not be submitted for review." });
    return reply(200, { resource: data });
  }

  if (action === "review") {
    if (!canReview) return reply(403, { error: "Only a curriculum editor or platform administrator can review resources." });
    const resourceId = String(body.resource_id ?? "");
    const decision = String(body.decision ?? "");
    const feedback = cleanText(body.feedback ?? "", 5000) ?? "";
    if (!resourceId || !["approved", "rejected", "changes_requested"].includes(decision)) {
      return reply(400, { error: "Choose a resource and a valid review decision." });
    }
    const { data: resource, error } = await admin.from("learning_resources")
      .select("id,status,version").eq("id", resourceId).maybeSingle();
    if (error || !resource) return reply(404, { error: "Learning resource not found." });
    if (resource.status !== "pending_review") {
      return reply(409, { error: "Only resources awaiting review can be reviewed." });
    }

    const { error: reviewError } = await admin.from("learning_resource_reviews").insert({
      resource_id: resourceId,
      version: resource.version,
      reviewer_id: actorId,
      decision,
      feedback,
    });
    if (reviewError) return reply(500, { error: "The review decision could not be recorded." });

    const nextStatus = decision === "approved" ? "approved" : decision === "rejected" ? "rejected" : "draft";
    const { data, error: updateError } = await admin.from("learning_resources")
      .update({
        status: nextStatus,
        reviewed_by: actorId,
        reviewed_at: new Date().toISOString(),
        review_feedback: feedback,
      })
      .eq("id", resourceId).select("id,status,review_feedback,reviewed_at").single();
    if (updateError) return reply(500, { error: "The review was recorded but resource status could not be updated." });
    return reply(200, { resource: data });
  }

  if (action === "publish" || action === "withdraw") {
    if (!canReview) return reply(403, { error: "Only a curriculum editor or platform administrator can publish resources." });
    const resourceId = String(body.resource_id ?? "");
    if (!resourceId) return reply(400, { error: "A resource ID is required." });
    const { data: resource, error } = await admin.from("learning_resources")
      .select("id,status").eq("id", resourceId).maybeSingle();
    if (error || !resource) return reply(404, { error: "Learning resource not found." });
    if (action === "publish" && resource.status !== "approved") {
      return reply(409, { error: "A resource must be approved before it can be published." });
    }
    if (action === "withdraw" && resource.status !== "published") {
      return reply(409, { error: "Only published resources can be withdrawn." });
    }
    const update = action === "publish"
      ? { status: "published", published_by: actorId, published_at: new Date().toISOString() }
      : { status: "withdrawn" };
    const { data, error: updateError } = await admin.from("learning_resources")
      .update(update).eq("id", resourceId).select("id,status,published_at").single();
    if (updateError) return reply(500, { error: "The resource status could not be updated." });
    return reply(200, { resource: data });
  }

  return reply(400, { error: "Unsupported learning-resource action." });
});
