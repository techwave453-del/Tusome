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

async function writeAudit(
  admin: ReturnType<typeof createClient>,
  actorId: string,
  actionKey: string,
  targetUserId: string | null,
  schoolId: string | null,
  details: Record<string, unknown>,
) {
  const { error } = await admin.from("admin_audit_logs").insert({
    actor_id: actorId,
    action_key: actionKey,
    target_user_id: targetUserId,
    school_id: schoolId,
    details,
  });
  if (error) console.error("Admin audit write failed:", actionKey, error.message);
  return !error;
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
  const canManageUsers = isPlatformAdmin || isSchoolAdmin;

  if (action === "list_schools") {
    if (!canManageUsers) return reply(403, { error: "You do not have permission to view schools." });
    let query = admin.from("schools").select("id,name,county,is_active").order("name");
    if (!isPlatformAdmin) query = query.eq("id", actorProfile!.school_id);
    const { data, error } = await query;
    if (error) return reply(500, { error: "Unable to load schools." });
    return reply(200, { schools: data ?? [] });
  }

  if (action === "list_users") {
    if (!canManageUsers) return reply(403, { error: "You do not have permission to view users." });
    let profilesQuery = admin.from("user_profiles")
      .select("user_id,display_name,school_id,grade_id,created_at")
      .order("created_at", { ascending: false })
      .limit(200);
    if (!isPlatformAdmin) profilesQuery = profilesQuery.eq("school_id", actorProfile!.school_id);
    const { data: profiles, error: profilesError } = await profilesQuery;
    if (profilesError) return reply(500, { error: "Unable to load user profiles." });
    const userIds = (profiles ?? []).map((profile) => profile.user_id);
    if (userIds.length === 0) return reply(200, { users: [] });

    const [{ data: roleRows, error: rolesError }, { data: authUsers, error: authUsersError }, { data: schools, error: schoolsError }] =
      await Promise.all([
        admin.from("user_role_assignments")
          .select("id,user_id,role_key,school_id")
          .in("user_id", userIds),
        admin.auth.admin.listUsers({ page: 1, perPage: 1000 }),
        admin.from("schools").select("id,name"),
      ]);
    if (rolesError || authUsersError || schoolsError) {
      return reply(500, { error: "Unable to assemble the user directory." });
    }

    const emailById = new Map((authUsers?.users ?? []).map((user) => [user.id, user.email ?? ""]));
    const schoolNameById = new Map((schools ?? []).map((school) => [school.id, school.name]));
    const rolesByUser = new Map<string, Array<{ id: string; role_key: string; school_id: string | null }>>();
    for (const row of roleRows ?? []) {
      const current = rolesByUser.get(row.user_id) ?? [];
      current.push({ id: row.id, role_key: row.role_key, school_id: row.school_id });
      rolesByUser.set(row.user_id, current);
    }
    const users = (profiles ?? []).map((profile) => ({
      ...profile,
      email: emailById.get(profile.user_id) ?? "",
      school_name: profile.school_id ? schoolNameById.get(profile.school_id) ?? "" : "",
      roles: rolesByUser.get(profile.user_id) ?? [],
    }));
    return reply(200, { users });
  }

  if (action === "list_audit_logs") {
    if (!canManageUsers) return reply(403, { error: "You do not have permission to view the audit log." });
    let query = admin.from("admin_audit_logs")
      .select("id,actor_id,action_key,target_user_id,school_id,details,created_at")
      .order("created_at", { ascending: false })
      .limit(50);
    if (!isPlatformAdmin) query = query.eq("school_id", actorProfile!.school_id);
    const { data, error } = await query;
    if (error) return reply(500, { error: "Unable to load the administration audit log." });
    return reply(200, { events: data ?? [] });
  }

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
    await writeAudit(admin, actorId, "school.created", null, data.id, { name: data.name, county: data.county });
    return reply(201, { school: data });
  }

  if (action === "invite_user") {
    if (!canManageUsers) return reply(403, { error: "You do not have permission to invite users." });

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

    if (!isPlatformAdmin && isSchoolAdmin) {
      if (!["learner", "teacher"].includes(roleKey)) {
        return reply(403, { error: "School administrators can invite learners and teachers only." });
      }
      if (requestedSchoolId !== actorProfile!.school_id) {
        return reply(403, { error: "You can only invite users to your own school." });
      }
    } else if (role.scope === "school" && !requestedSchoolId) {
      return reply(400, { error: "A school is required for this role." });
    }

    if (requestedSchoolId) {
      const { data: school, error } = await admin.from("schools")
        .select("id,is_active").eq("id", requestedSchoolId).maybeSingle();
      if (error || !school || !school.is_active) return reply(400, { error: "Choose an active school." });
    }
    const profileSchoolId = role.scope === "school" || roleKey === "learner" ? requestedSchoolId : null;

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
      .update({ display_name: displayName, school_id: profileSchoolId, grade_id: gradeId })
      .eq("user_id", newUserId);
    if (updateProfileError) {
      await admin.auth.admin.deleteUser(newUserId);
      return reply(500, { error: "Invitation setup failed; the new account was rolled back." });
    }

    if (roleKey !== "learner") {
      // The auth trigger gives every new account the default learner role.
      // Inviting someone to a different primary role must replace that default,
      // not accidentally leave the new user with two roles.
      const { error: defaultRoleError } = await admin.from("user_role_assignments")
        .delete()
        .eq("user_id", newUserId)
        .eq("role_key", "learner")
        .is("school_id", null);
      if (defaultRoleError) {
        await admin.auth.admin.deleteUser(newUserId);
        return reply(500, { error: "Default role setup failed; the invitation was rolled back." });
      }

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

    await writeAudit(admin, actorId, "user.invited", newUserId, profileSchoolId, {
      email, display_name: displayName, role_key: roleKey,
    });
    return reply(201, {
      user_id: newUserId,
      email,
      display_name: displayName,
      role_key: roleKey,
      school_id: profileSchoolId,
      message: "Invitation sent. The user must complete the email invitation before signing in.",
    });
  }

  if (action === "replace_secondary_role") {
    if (!isPlatformAdmin) return reply(403, { error: "Only the super user can change role assignments." });
    const targetUserId = String(body.user_id ?? actorId).trim();
    const roleKey = String(body.role_key ?? "").trim();
    const requestedSchoolId = body.school_id ? String(body.school_id) : null;
    if (targetUserId !== actorId) {
      return reply(403, { error: "This action can only change your own secondary role." });
    }
    if (!["learner", "teacher", "school_admin", "curriculum_editor"].includes(roleKey)) {
      return reply(400, { error: "Choose a valid secondary role. The platform administrator role is protected." });
    }

    const { data: role, error: roleError } = await admin.from("app_roles")
      .select("role_key,scope").eq("role_key", roleKey).maybeSingle();
    if (roleError || !role) return reply(400, { error: "Unknown role." });
    if (role.scope === "school" && !requestedSchoolId) {
      return reply(400, { error: "Choose a school for this role." });
    }
    if (role.scope === "global" && requestedSchoolId) {
      return reply(400, { error: "Global roles do not accept a school scope." });
    }
    if (requestedSchoolId) {
      const { data: school, error } = await admin.from("schools")
        .select("id,is_active").eq("id", requestedSchoolId).maybeSingle();
      if (error || !school || !school.is_active) return reply(400, { error: "Choose an active school." });
    }

    const scopedSchoolId = role.scope === "school" ? requestedSchoolId : null;
    const { data: currentRows, error: currentRowsError } = await admin.from("user_role_assignments")
      .select("id,role_key,school_id")
      .eq("user_id", actorId);
    if (currentRowsError) return reply(500, { error: "Unable to read your current role assignments." });

    const existingDesired = (currentRows ?? []).find(
      (row) => row.role_key === roleKey && row.school_id === scopedSchoolId,
    );
    if (!existingDesired) {
      const { error: insertError } = await admin.from("user_role_assignments").insert({
        user_id: actorId,
        role_key: roleKey,
        school_id: scopedSchoolId,
        assigned_by: actorId,
      });
      if (insertError) return reply(500, { error: "The new secondary role could not be assigned." });
    }

    const obsoleteIds = (currentRows ?? [])
      .filter((row) => row.role_key !== "platform_admin" &&
        !(row.role_key === roleKey && row.school_id === scopedSchoolId))
      .map((row) => row.id);
    if (obsoleteIds.length > 0) {
      const { error: deleteError } = await admin.from("user_role_assignments")
        .delete().in("id", obsoleteIds);
      if (deleteError) return reply(500, { error: "The new role was assigned, but the previous secondary role could not be removed. Refresh and try again." });
    }

    await writeAudit(admin, actorId, "role.secondary_changed", actorId, scopedSchoolId, {
      role_key: roleKey,
      previous_roles: (currentRows ?? []).filter((row) => row.role_key !== "platform_admin")
        .map((row) => ({ role_key: row.role_key, school_id: row.school_id })),
    });
    return reply(200, { message: `Your secondary role is now ${roleKey.replaceAll("_", " ")}. Your platform administrator role remains active. Sign out and back in to refresh your permissions.` });
  }

  if (action === "assign_role") {
    if (!isPlatformAdmin) return reply(403, { error: "Only the super user can assign additional roles." });
    const targetUserId = String(body.user_id ?? "").trim();
    const roleKey = String(body.role_key ?? "").trim();
    const requestedSchoolId = body.school_id ? String(body.school_id) : null;
    if (!targetUserId || targetUserId === actorId) {
      return reply(400, { error: "Choose another user. Use the secondary-role control to change your own role." });
    }
    if (!["learner", "teacher", "school_admin", "curriculum_editor"].includes(roleKey)) {
      return reply(400, { error: "Choose a valid additional role. Platform administrator assignments are managed separately." });
    }

    const { data: role, error: roleError } = await admin.from("app_roles")
      .select("role_key,scope").eq("role_key", roleKey).maybeSingle();
    if (roleError || !role) return reply(400, { error: "Unknown role." });
    if (role.scope === "school" && !requestedSchoolId) {
      return reply(400, { error: "Choose a school for this role." });
    }
    if (role.scope === "global" && requestedSchoolId) {
      return reply(400, { error: "Global roles do not accept a school scope." });
    }
    if (requestedSchoolId) {
      const { data: school, error } = await admin.from("schools")
        .select("id,is_active").eq("id", requestedSchoolId).maybeSingle();
      if (error || !school || !school.is_active) return reply(400, { error: "Choose an active school." });
    }

    const scopedSchoolId = role.scope === "school" ? requestedSchoolId : null;
    const { data: existing, error: existingError } = await admin.from("user_role_assignments")
      .select("id").eq("user_id", targetUserId).eq("role_key", roleKey)
      .match(scopedSchoolId ? { school_id: scopedSchoolId } : {})
      .maybeSingle();
    if (existingError) return reply(500, { error: "Unable to check existing role assignments." });
    if (existing) return reply(409, { error: "That user already has this role in the selected scope." });

    const { error: insertError } = await admin.from("user_role_assignments").insert({
      user_id: targetUserId,
      role_key: roleKey,
      school_id: scopedSchoolId,
      assigned_by: actorId,
    });
    if (insertError) return reply(500, { error: "Additional role could not be assigned." });

    await writeAudit(admin, actorId, "role.assigned", targetUserId, scopedSchoolId, {
      role_key: roleKey,
      explicitly_assigned: true,
    });
    return reply(200, { message: `Additional ${roleKey.replaceAll("_", " ")} role assigned.` });
  }

  if (action === "revoke_role") {
    if (!canManageUsers) return reply(403, { error: "You do not have permission to manage roles." });
    const targetUserId = String(body.user_id ?? "").trim();
    const roleKey = String(body.role_key ?? "").trim();
    const requestedSchoolId = body.school_id ? String(body.school_id) : null;
    if (!targetUserId || !roleKey) return reply(400, { error: "A user and role are required." });
    if (targetUserId === actorId) return reply(400, { error: "You cannot revoke your own role assignment." });
    if (roleKey === "platform_admin") {
      return reply(403, { error: "The platform administrator role is protected and cannot be revoked from this screen." });
    }
    if (!isPlatformAdmin && (!isSchoolAdmin || roleKey !== "teacher" || requestedSchoolId !== actorProfile!.school_id)) {
      return reply(403, { error: "School administrators can revoke teacher roles only within their own school." });
    }

    const { data: role, error: roleError } = await admin.from("app_roles")
      .select("role_key,scope").eq("role_key", roleKey).maybeSingle();
    if (roleError || !role) return reply(400, { error: "Unknown role." });
    if (role.scope === "school" && !requestedSchoolId) {
      return reply(400, { error: "A school scope is required for this role." });
    }
    if (role.scope === "global" && requestedSchoolId) {
      return reply(400, { error: "Global roles do not accept a school scope." });
    }

    let assignmentQuery = admin.from("user_role_assignments")
      .select("id,user_id,role_key,school_id")
      .eq("user_id", targetUserId)
      .eq("role_key", roleKey);
    assignmentQuery = requestedSchoolId
      ? assignmentQuery.eq("school_id", requestedSchoolId)
      : assignmentQuery.is("school_id", null);
    const { data: assignment, error: assignmentError } = await assignmentQuery.maybeSingle();
    if (assignmentError || !assignment) return reply(404, { error: "That role assignment was not found." });

    const { error: deleteError } = await admin.from("user_role_assignments")
      .delete().eq("id", assignment.id);
    if (deleteError) return reply(500, { error: "The role could not be revoked." });

    await writeAudit(admin, actorId, "role.revoked", targetUserId, requestedSchoolId, {
      role_key: roleKey,
      assignment_id: assignment.id,
    });
    return reply(200, { message: `${roleKey.replaceAll("_", " ")} role revoked.` });
  }

  return reply(400, { error: "Unsupported action." });
});
