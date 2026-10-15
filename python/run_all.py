"""
Run every solution file and report PASS/FAIL. Each file checks its own answers with asserts.

    python python/run_all.py            # run everything
    python python/run_all.py trees      # only files whose path contains "trees"
    python python/run_all.py -v         # also show each file's output
"""
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).parent


def main():
    args = [a for a in sys.argv[1:] if a != "-v"]
    verbose = "-v" in sys.argv[1:]
    files = sorted(p for p in ROOT.rglob("*.py") if p.name != "run_all.py")
    if args:
        files = [p for p in files if any(a in p.as_posix() for a in args)]

    failures = []
    for path in files:
        started = time.perf_counter()
        result = subprocess.run([sys.executable, str(path)], capture_output=True, text=True,
                                encoding="utf-8", errors="replace")
        seconds = time.perf_counter() - started
        status = "PASS" if result.returncode == 0 else "FAIL"
        print(f"{status}  {path.relative_to(ROOT).as_posix():<52} {seconds:5.2f}s")
        if verbose and result.stdout:
            print(result.stdout)
        if result.returncode != 0:
            failures.append(path)
            print(result.stdout[-2000:])
            print(result.stderr[-2000:])

    print(f"\n{len(files) - len(failures)}/{len(files)} files passed")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
