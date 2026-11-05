import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
};

type JsonObject = Record<string, unknown>;
function reply(status: number, body: JsonObject) {
  return new Response(JSON.stringify(body), { status, headers: corsHeaders });
}
function textValue(value: unknown, max = 5000): string {
  return typeof value === "string" ? value.trim().slice(0, max) : "";
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return reply(405, { error: "Use POST." });
  const authorization = req.headers.get("Authorization");
  if (!authorization?.startsWith("Bearer ")) return reply(401, { error: "Sign-in is required." });

  const url = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !anonKey || !serviceKey) return reply(500, { error: "Server configuration is incomplete." });

  const token = authorization.slice("Bearer ".length);
  const caller = createClient(url, anonKey, {
    auth: { persistSession: false, autoRefreshToken: false },
    global: { headers: { Authorization: `Bearer ${token}` } },
  });
  const admin = createClient(url, serviceKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: authData, error: authError } = await caller.auth.getUser(token);
  if (authError || !authData.user) return reply(401, { error: "Your session is invalid or expired." });
  const actorId = authData.user.id;

  let body: JsonObject;
  try { body = await req.json(); } catch { return reply(400, { error: "Request body must be valid JSON." }); }
  const action = textValue(body.action, 40);
  const paperId = textValue(body.paper_id, 80);
  const feedback = textValue(body.feedback, 5000);

  const [{ data: assignments, error: roleError }, { data: profile }] = await Promise.all([
    admin.from("user_role_assignments").select("role_key,school_id").eq("user_id", actorId),
    admin.from("user_profiles").select("school_id").eq("user_id", actorId).maybeSingle(),
  ]);
  if (roleError) return reply(500, { error: "Unable to verify assessment permissions." });
  const roles = assignments ?? [];
  const globalAdmin = roles.some((r) => r.role_key === "platform_admin" && r.school_id === null);
  const globalEditor = roles.some((r) => r.role_key === "curriculum_editor" && r.school_id === null);
  const schoolId = profile?.school_id ?? null;
  const schoolTeacher = !!schoolId && roles.some((r) => r.role_key === "teacher" && r.school_id === schoolId);
  const canCreate = globalAdmin || globalEditor || schoolTeacher;
  const canReview = globalAdmin || globalEditor;
  const canPublish = globalAdmin || globalEditor;

  if (action === "list_mine") {
    if (!canCreate) return reply(403, { error: "You are not authorised to create assessments." });
    const { data, error } = await admin.from("assessment_papers")
      .select("id,paper_code,grade_id,learning_area_id,strand_id,sub_strand_id,title,assessment_type,content_status,total_marks,duration_minutes,version,created_at,updated_at")
      .eq("created_by", actorId).order("created_at", { ascending: false }).limit(100);
    if (error) return reply(500, { error: "Unable to load your assessment drafts." });
    return reply(200, { papers: data ?? [] });
  }

  if (action === "review_queue") {
    if (!canReview) return reply(403, { error: "Only curriculum editors and platform administrators can review assessments." });
    const { data, error } = await admin.from("assessment_papers")
      .select("id,paper_code,grade_id,learning_area_id,strand_id,sub_strand_id,title,assessment_type,content_status,total_marks,duration_minutes,version,created_by,created_at,updated_at")
      .eq("content_status", "in_review").order("updated_at", { ascending: true }).limit(100);
    if (error) return reply(500, { error: "Unable to load the assessment review queue." });
    return reply(200, { papers: data ?? [] });
  }

  if (!paperId) return reply(400, { error: "A paper ID is required." });
  const { data: paper, error: paperError } = await admin.from("assessment_papers")
    .select("id,created_by,school_id,content_status,version,reviewed_by,reviewed_at")
    .eq("id", paperId).maybeSingle();
  if (paperError || !paper) return reply(404, { error: "Assessment paper not found." });

  if (action === "submit_for_review") {
    const isOwner = paper.created_by === actorId;
    if (!isOwner || !canCreate) return reply(403, { error: "Only the authorised paper creator can submit this draft." });
    if (paper.content_status !== "draft") return reply(409, { error: "Only draft papers can be submitted for review." });
    const { error } = await admin.from("assessment_papers")
      .update({ content_status: "in_review" }).eq("id", paperId);
    if (error) return reply(500, { error: "Unable to submit this assessment for review." });
    return reply(200, { paper_id: paperId, status: "in_review" });
  }

  if (action === "approve" || action === "request_changes" || action === "reject") {
    if (!canReview) return reply(403, { error: "Only curriculum editors and platform administrators can review assessments." });
    if (paper.content_status !== "in_review") return reply(409, { error: "Only papers in the review queue can receive a review decision." });
    const decision = action === "approve" ? "approved" : action === "reject" ? "rejected" : "changes_requested";
    const nextStatus = action === "approve" ? "in_review" : "draft";
    const update = action === "approve"
      ? { content_status: nextStatus, reviewed_by: actorId, reviewed_at: new Date().toISOString() }
      : { content_status: nextStatus, reviewed_by: null, reviewed_at: null };
    const { error: updateError } = await admin.from("assessment_papers").update(update).eq("id", paperId);
    if (updateError) return reply(500, { error: "Unable to save the assessment review decision." });
    const { error: auditError } = await admin.from("assessment_paper_reviews").insert({
      paper_id: paperId, paper_version: paper.version, reviewer_id: actorId, decision, feedback,
    });
    if (auditError) return reply(500, { error: "The decision was saved, but its audit record could not be written." });
    return reply(200, { paper_id: paperId, decision, status: nextStatus });
  }

  if (action === "publish") {
    if (!canPublish) return reply(403, { error: "Only curriculum editors and platform administrators can publish assessments." });
    if (paper.content_status !== "in_review" || !paper.reviewed_by || !paper.reviewed_at) {
      return reply(409, { error: "This paper must be approved by a reviewer before publication." });
    }
    const { error } = await admin.from("assessment_papers").update({
      content_status: "published", published_by: actorId, published_at: new Date().toISOString(),
    }).eq("id", paperId);
    if (error) return reply(500, { error: "Unable to publish this assessment." });
    await admin.from("assessment_paper_reviews").insert({
      paper_id: paperId, paper_version: paper.version, reviewer_id: actorId,
      decision: "published", feedback,
    });
    return reply(200, { paper_id: paperId, status: "published" });
  }

  if (action === "withdraw") {
    if (!canPublish) return reply(403, { error: "Only curriculum editors and platform administrators can withdraw assessments." });
    if (paper.content_status !== "published") return reply(409, { error: "Only published assessments can be withdrawn." });
    const { error } = await admin.from("assessment_papers").update({
      content_status: "archived", published_by: actorId, published_at: null,
    }).eq("id", paperId);
    if (error) return reply(500, { error: "Unable to withdraw this assessment." });
    await admin.from("assessment_paper_reviews").insert({
      paper_id: paperId, paper_version: paper.version, reviewer_id: actorId,
      decision: "withdrawn", feedback,
    });
    return reply(200, { paper_id: paperId, status: "archived" });
  }

  return reply(400, { error: "Unsupported assessment action." });
});
