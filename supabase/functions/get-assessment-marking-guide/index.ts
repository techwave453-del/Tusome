import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
};

function reply(status: number, body: Record<string, unknown>) {
  return new Response(JSON.stringify(body), { status, headers: corsHeaders });
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

  let body: Record<string, unknown>;
  try { body = await req.json(); } catch { return reply(400, { error: "Request body must be valid JSON." }); }
  const paperId = typeof body.paper_id === "string" ? body.paper_id.trim() : "";
  if (!paperId) return reply(400, { error: "A paper ID is required." });

  const { data: attempt, error: attemptError } = await admin.from("learner_assessment_attempts")
    .select("id,paper_version,submitted_at")
    .eq("user_id", authData.user.id)
    .eq("paper_id", paperId)
    .not("submitted_at", "is", null)
    .order("submitted_at", { ascending: false })
    .limit(1)
    .maybeSingle();
  if (attemptError) return reply(500, { error: "Unable to verify the submitted assessment." });
  if (!attempt) return reply(403, { error: "Submit this assessment first to access its marking guide." });

  const { data: paper, error: paperError } = await admin.from("assessment_papers")
    .select("id,version,content_status,marking_payload")
    .eq("id", paperId)
    .eq("content_status", "published")
    .maybeSingle();
  if (paperError || !paper) return reply(404, { error: "Published assessment not found." });
  if (attempt.paper_version !== paper.version) {
    return reply(409, { error: "The marking guide for this paper version is no longer available." });
  }
  return reply(200, {
    paper_id: paperId,
    paper_version: paper.version,
    marking_guide: paper.marking_payload ?? [],
  });
});
