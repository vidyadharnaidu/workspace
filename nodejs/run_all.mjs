/**
 * Run every solution file and report PASS/FAIL. Each file checks its own answers.
 *
 *   node nodejs/run_all.mjs            # everything
 *   node nodejs/run_all.mjs streams    # only paths containing "streams"
 */
import { execFile } from 'node:child_process';
import { readdir } from 'node:fs/promises';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';
import { promisify } from 'node:util';

const execFileAsync = promisify(execFile);
const ROOT = dirname(fileURLToPath(import.meta.url));

async function collect(dir) {
  const entries = await readdir(dir, { withFileTypes: true });
  const files = await Promise.all(
    entries.map(async (entry) => {
      const full = join(dir, entry.name);
      if (entry.isDirectory()) return collect(full);
      return entry.name.endsWith('.mjs') && entry.name !== 'run_all.mjs' ? [full] : [];
    }),
  );
  return files.flat();
}

const filters = process.argv.slice(2);
const files = (await collect(ROOT))
  .filter((file) => !filters.length || filters.some((needle) => file.includes(needle)))
  .sort();

let failed = 0;
for (const file of files) {
  const label = relative(ROOT, file).replaceAll('\\', '/');
  const started = Date.now();
  try {
    await execFileAsync(process.execPath, [file], { timeout: 120_000 });
    console.log(`PASS  ${label.padEnd(44)} ${((Date.now() - started) / 1000).toFixed(2)}s`);
  } catch (error) {
    failed++;
    console.log(`FAIL  ${label}`);
    console.log(String(error.stdout ?? '').slice(-1500));
    console.log(String(error.stderr ?? '').slice(-1500));
  }
}

console.log(`\n${files.length - failed}/${files.length} files passed`);
process.exitCode = failed ? 1 : 0;
