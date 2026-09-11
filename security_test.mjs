import { createClient } from '@supabase/supabase-js';
import readline from 'readline';

const { SUPABASE_URL, SUPABASE_ANON_KEY } = process.env;

if (!SUPABASE_URL || !SUPABASE_ANON_KEY) {
  console.error("BLOCKED: Missing required SUPABASE_URL or SUPABASE_ANON_KEY environment variables.");
  process.exit(1);
}

const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

const askQuestion = (query) => new Promise((resolve) => rl.question(query, resolve));

const askPassword = (query) => {
  return new Promise((resolve) => {
    process.stdout.write(query);
    let password = '';
    
    const onData = (char) => {
      char = char.toString('utf8');
      switch (char) {
        case '\n':
        case '\r':
        case '\u0004':
          process.stdin.removeListener('data', onData);
          process.stdin.setRawMode(false);
          process.stdin.pause();
          console.log();
          resolve(password);
          break;
        case '\u0003': // Ctrl+C
          process.stdin.removeListener('data', onData);
          process.stdin.setRawMode(false);
          process.exit(1);
          break;
        case '\b':
        case '\x7f': // Backspace
          if (password.length > 0) {
            password = password.slice(0, -1);
            process.stdout.write('\b \b');
          }
          break;
        default:
          password += char;
          process.stdout.write('*');
          break;
      }
    };
    
    process.stdin.resume();
    process.stdin.setRawMode(true);
    process.stdin.on('data', onData);
  });
};

async function runTests() {
  console.log("=== HAKEEM CAFETERIA SECURITY TESTS ===\n");
  
  const studentEmail = await askQuestion("Enter STUDENT_EMAIL: ");
  const studentPassword = await askPassword("Enter STUDENT_PASSWORD: ");
  const adminEmail = await askQuestion("Enter ADMIN_EMAIL: ");
  const adminPassword = await askPassword("Enter ADMIN_PASSWORD: ");
  
  rl.close();

  console.log("\n=== STARTING SECURITY TESTS ===");
  
  // 1. Authenticate Student
  console.log("\n--- Authenticating Student ---");
  const { data: studentAuth, error: studentAuthErr } = await supabase.auth.signInWithPassword({
    email: studentEmail,
    password: studentPassword
  });
  
  if (studentAuthErr || !studentAuth.user) {
    console.error("BLOCKED: Failed to authenticate student.", studentAuthErr?.message || "Unknown error");
    process.exit(1);
  }
  
  const studentUid = studentAuth.user.id;
  console.log(`Student authenticated successfully. UUID: ${studentUid}`);
  
  // 3. Student role-change attempt
  console.log("\n[Test 1] Student role-change attempt (Self-Promotion)");
  const { data: roleAttempt, error: roleError } = await supabase
    .from('users')
    .update({ role: 'ADMIN' })
    .eq('id', studentUid)
    .select();
    
  if (roleError) {
    console.log(`PASS: Student blocked from changing role. Error: ${roleError.message}`);
  } else {
    const updatedUser = roleAttempt[0];
    if (updatedUser && updatedUser.role === 'ADMIN') {
      console.error(`FAIL: Student successfully changed role to ADMIN!`);
      // Revert just in case it failed open
      await supabase.from('users').update({ role: 'STUDENT' }).eq('id', studentUid);
    } else {
      console.log(`PASS (Partial): Student update succeeded but role was silently reverted. Role is: ${updatedUser?.role}`);
    }
  }

  // 4. Student legitimate profile test
  console.log("\n[Test 2] Student legitimate profile update");
  const { data: currentData } = await supabase.from('users').select('full_name').eq('id', studentUid).single();
  const originalName = currentData?.full_name || 'Original Name';
  const testName = "Security Test Name";
  
  const { data: nameAttempt, error: nameError } = await supabase
    .from('users')
    .update({ full_name: testName })
    .eq('id', studentUid)
    .select();
    
  if (nameError) {
    console.error(`FAIL: Legitimate update blocked! Error: ${nameError.message}`);
  } else {
    console.log(`PASS: Legitimate update allowed. Value changed to: ${nameAttempt[0].full_name}`);
    
    // Restore
    await supabase.from('users').update({ full_name: originalName }).eq('id', studentUid);
    console.log("Restored original profile name.");
  }
  
  // 5. Student ID protection
  console.log("\n[Test 3] Student ID protection");
  const fakeId = "00000000-0000-0000-0000-000000000000";
  const { error: idError } = await supabase
    .from('users')
    .update({ id: fakeId })
    .eq('id', studentUid);
    
  if (idError) {
    console.log(`PASS: Student blocked from changing ID. Error: ${idError.message}`);
  } else {
    console.log(`FAIL or SILENT IGNORE: ID change request succeeded without exception.`);
  }
  
  // 6. Student created_at protection
  console.log("\n[Test 4] Student created_at protection");
  const { error: createdAtError } = await supabase
    .from('users')
    .update({ created_at: 0 })
    .eq('id', studentUid);
    
  if (createdAtError) {
    console.log(`PASS: Student blocked from changing created_at. Error: ${createdAtError.message}`);
  } else {
    console.log(`FAIL or SILENT IGNORE: created_at change request succeeded without exception.`);
  }

  // Sign out student securely before moving to Admin
  await supabase.auth.signOut();
  
  // 7. Admin Test
  console.log("\n--- Authenticating Admin ---");
  const { data: adminAuth, error: adminAuthErr } = await supabase.auth.signInWithPassword({
    email: adminEmail,
    password: adminPassword
  });
  
  if (adminAuthErr || !adminAuth.user) {
    console.error("BLOCKED: Failed to authenticate admin.", adminAuthErr?.message || "Unknown error");
    process.exit(1);
  }
  
  const adminUid = adminAuth.user.id;
  console.log(`Admin authenticated successfully. UUID: ${adminUid}`);
  
  console.log("\n[Test 5] Admin attempts to change student role");
  
  // Get student's current role before modification
  const { data: studentBefore } = await supabase.from('users').select('role').eq('id', studentUid).single();
  const originalRole = studentBefore?.role || 'STUDENT';
  
  const { data: adminRoleAttempt, error: adminRoleError } = await supabase
    .from('users')
    .update({ role: 'ADMIN' })
    .eq('id', studentUid)
    .select();
    
  if (adminRoleError) {
    console.error(`FAIL: Admin was blocked from changing student role! Error: ${adminRoleError.message}`);
  } else {
    if (adminRoleAttempt && adminRoleAttempt[0] && adminRoleAttempt[0].role === 'ADMIN') {
      console.log(`PASS: Admin successfully changed student role to ADMIN.`);
      
      // Critical cleanup: Immediately restore the student's original role
      await supabase.from('users').update({ role: originalRole }).eq('id', studentUid);
      console.log(`Restored student's original role (${originalRole}).`);
    } else {
      console.error(`FAIL: Admin update executed but role was not actually changed to ADMIN.`);
    }
  }
  
  console.log("\n=== SECURITY TESTS COMPLETED ===");
  process.exit(0);
}

runTests();
