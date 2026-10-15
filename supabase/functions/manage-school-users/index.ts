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

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !anonKey || !serviceKey) {
    return reply(500, { error: "Server authentication configuration is incomplete." });
  }

  const token = authorization.slice("Bearer ".length);
  const callerClient = createClient(supabaseUrl, anonKey, {
    auth: { persistSession: false, autoRefreshToken: false },
    global: { headers: { Authorization: `Bearer ${token}` } },
  });
  const admin = createClient(supabaseUrl, serviceKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const { data: authData, error: authError } = await callerClient.auth.getUser(token);
  if (authError || !authData.user) return reply(401, { error: "Your session is invalid or expired. Sign in again." });
  const actorId = authData.user.id;

  let body: Record<string, unknown>;
  try {
    body = await req.json();
  } catch {
    return reply(400, { error: "Request body must be valid JSON." });
  }

  const action = String(body.action ?? "");
  const { data: assignments, error: assignmentsError } = await admin
    .from("user_role_assignments")
    .select("role_key,school_id")
    .eq("user_id", actorId);
  if (assignmentsError) return reply(500, { error: "Unable to verify administrator permissions." });

  const isPlatformAdmin = (assignments ?? []).some(
    (item) => item.role_key === "platform_admin" && item.school_id === null,
  );
  const { data: actorProfile, error: profileError } = await admin
    .from("user_profiles")
    .select("school_id")
    .eq("user_id", actorId)
    .maybeSingle();
  if (profileError) return reply(500, { error: "Unable to load administrator profile." });

  const isSchoolAdmin = !!actorProfile?.school_id && (assignments ?? []).some(
    (item) => item.role_key === "school_admin" && item.school_id === actorProfile.school_id,
  );

  if (action === "create_school") {
    if (!isPlatformAdmin) return reply(403, { error: "Only a platform administrator can create schools." });
    const name = String(body.name ?? "").trim();
    const county = String(body.county ?? "").trim();
    if (name.length < 2 || name.length > 160) return reply(400, { error: "School name must be 2–160 characters." });
    const { data, error } = await admin.from("schools")
      .insert({ name, county })
      .select("id,name,county,created_at")
      .single();
    if (error) return reply(400, { error: "School could not be created." });
    return reply(201, { school: data });
  }

  if (action !== "invite_user") return reply(400, { error: "Unsupported action." });
  if (!isPlatformAdmin && !isSchoolAdmin) {
    return reply(403, { error: "You do not have permission to invite users." });
  }

  const email = String(body.email ?? "").trim().toLowerCase();
  const displayName = String(body.display_name ?? "").trim();
  const roleKey = String(body.role_key ?? "learner");
  const requestedSchoolId = body.school_id ? String(body.school_id) : null;
  const gradeId = body.grade_id ? String(body.grade_id) : null;

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return reply(400, { error: "Enter a valid email address." });
  if (displayName.length < 2 || displayName.length > 120) return reply(400, { error: "Display name must be 2–120 characters." });

  const { data: role, error: roleError } = await admin.from("app_roles")
    .select("role_key,scope").eq("role_key", roleKey).maybeSingle();
  if (roleError || !role) return reply(400, { error: "Unknown role." });

  if (isSchoolAdmin && !isPlatformAdmin) {
    if (!["learner", "teacher"].includes(roleKey)) {
      return reply(403, { error: "School administrators can invite learners and teachers only." });
    }
    if (requestedSchoolId !== actorProfile.school_id) {
      return reply(403, { error: "You can only invite users to your own school." });
    }
  } else if (role.scope === "school" && !requestedSchoolId) {
    return reply(400, { error: "A school is required for this role." });
  }

  if (role.scope === "school" && requestedSchoolId) {
    const { data: school, error } = await admin.from("schools")
      .select("id,is_active").eq("id", requestedSchoolId).maybeSingle();
    if (error || !school || !school.is_active) return reply(400, { error: "Choose an active school." });
  }
  if (gradeId) {
    const { data: grade, error } = await admin.from("curriculum_grades")
      .select("id").eq("id", gradeId).maybeSingle();
    if (error || !grade) return reply(400, { error: "Choose a valid grade." });
  }

  const { data: invite, error: inviteError } = await admin.auth.admin.inviteUserByEmail(email, {
    data: { display_name: displayName },
  });
  if (inviteError || !invite.user) {
    return reply(400, { error: inviteError?.message ?? "Invitation could not be sent." });
  }

  const newUserId = invite.user.id;
  const { error: updateProfileError } = await admin.from("user_profiles")
    .update({ display_name: displayName, school_id: requestedSchoolId, grade_id: gradeId })
    .eq("user_id", newUserId);
  if (updateProfileError) {
    await admin.auth.admin.deleteUser(newUserId);
    return reply(500, { error: "Invitation setup failed; the new account was rolled back." });
  }

  // The auth trigger has already assigned the safe default learner role.
  // Add a second assignment only when a different role is requested.
  if (roleKey !== "learner") {
    const scopedSchoolId = role.scope === "school" ? requestedSchoolId : null;
    const { error: assignmentError } = await admin.from("user_role_assignments").insert({
      user_id: newUserId,
      role_key: roleKey,
      school_id: scopedSchoolId,
      assigned_by: actorId,
    });
    if (assignmentError) {
      await admin.auth.admin.deleteUser(newUserId);
      return reply(500, { error: "Role assignment failed; the invitation was rolled back." });
    }
  }

  return reply(201, {
    user_id: newUserId,
    email,
    display_name: displayName,
    role_key: roleKey,
    school_id: requestedSchoolId,
    message: "Invitation sent. The user must complete the email invitation before signing in.",
  });
});
