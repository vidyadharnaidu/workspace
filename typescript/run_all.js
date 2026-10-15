/**
 * Type-check the whole folder, then run every .ts file.
 *
 *   node typescript/run_all.js
 *
 * Type stripping (running .ts directly) needs Node 22.6+ / 24. Type checking uses the installed TypeScript.
 */
const { execFileSync, execSync } = require("node:child_process");
const { readdirSync } = require("node:fs");
const { join } = require("node:path");

const ROOT = __dirname;

console.log("type-checking with tsc --noEmit ...");
try {
  execSync(`npx tsc --noEmit -p "${ROOT}"`, { stdio: "pipe" });
  console.log("PASS  tsc --noEmit (strict)\n");
} catch (err) {
  console.log("FAIL  tsc --noEmit");
  console.log(String(err.stdout ?? ""));
  process.exit(1);
}

const files = readdirSync(ROOT).filter((f) => f.endsWith(".ts")).sort();
let failed = 0;

for (const file of files) {
  const started = Date.now();
  try {
    execFileSync(process.execPath, ["--disable-warning=ExperimentalWarning", join(ROOT, file)], { stdio: "pipe" });
    console.log(`PASS  ${file.padEnd(40)} ${((Date.now() - started) / 1000).toFixed(2)}s`);
  } catch (err) {
    failed++;
    console.log(`FAIL  ${file}`);
    console.log(String(err.stdout ?? "").slice(-1500));
    console.log(String(err.stderr ?? "").slice(-1500));
  }
}

console.log(`\n${files.length - failed}/${files.length} files passed`);
process.exit(failed ? 1 : 0);
