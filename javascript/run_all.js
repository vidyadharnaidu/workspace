/**
 * Run every .js solution file and report PASS/FAIL.
 *
 *   node javascript/run_all.js            # everything
 *   node javascript/run_all.js async      # only paths containing "async"
 */
const { execFileSync } = require("node:child_process");
const { readdirSync, statSync } = require("node:fs");
const { join, relative } = require("node:path");

const ROOT = __dirname;

function collect(dir) {
  return readdirSync(dir).flatMap((entry) => {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) return collect(full);
    return full.endsWith(".js") && entry !== "run_all.js" ? [full] : [];
  });
}

const filters = process.argv.slice(2);
const files = collect(ROOT)
  .filter((f) => !filters.length || filters.some((needle) => f.includes(needle)))
  .sort();

let failed = 0;
for (const file of files) {
  const label = relative(ROOT, file).replace(/\\/g, "/");
  const started = Date.now();
  try {
    execFileSync(process.execPath, [file], { stdio: "pipe" });
    console.log(`PASS  ${label.padEnd(52)} ${((Date.now() - started) / 1000).toFixed(2)}s`);
  } catch (err) {
    failed++;
    console.log(`FAIL  ${label}`);
    console.log(String(err.stdout ?? "").slice(-1500));
    console.log(String(err.stderr ?? "").slice(-1500));
  }
}

console.log(`\n${files.length - failed}/${files.length} files passed`);
process.exit(failed ? 1 : 0);
