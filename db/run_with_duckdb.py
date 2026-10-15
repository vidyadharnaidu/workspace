"""
Run every .sql file in this folder (in order) against an in-memory DuckDB database
and print each query's result. No database server needed.

    pip install duckdb
    python db/run_with_duckdb.py                      # run everything
    python db/run_with_duckdb.py 05_window_functions  # setup + only files whose path contains the text

DuckDB understands PostgreSQL syntax. To use a real PostgreSQL instead, see db/README.md.
"""
import sys
from pathlib import Path

import duckdb

ROOT = Path(__file__).parent
MAX_WIDTH = 60
RESULT_TYPES = {"SELECT", "EXPLAIN"}


def print_result(cursor):
    headers = [d[0] for d in cursor.description]
    rows = [["NULL" if v is None else str(v).replace("\n", " ")[:MAX_WIDTH] for v in row] for row in cursor.fetchall()]
    widths = [max([len(h)] + [len(r[i]) for r in rows]) for i, h in enumerate(headers)]
    line = lambda cells: " | ".join(c.ljust(w) for c, w in zip(cells, widths)).rstrip()
    print(line(headers))
    print("-+-".join("-" * w for w in widths))
    for r in rows:
        print(line(r))
    print(f"({len(rows)} row{'' if len(rows) == 1 else 's'})\n")


def question_title(sql):
    """The '-- Qn. ...' comment that precedes a statement (DuckDB keeps leading comments in the statement text)."""
    titles = [l.strip().lstrip("-").strip() for l in sql.splitlines() if l.strip().startswith("-- Q")]
    return titles[-1] if titles else None


def run_file(con, path):
    print("=" * 100)
    print(f"FILE: {path.relative_to(ROOT).as_posix()}")
    print("=" * 100)
    for stmt in con.extract_statements(path.read_text(encoding="utf-8")):
        title = question_title(stmt.query)
        if title:
            print(f">>> {title}")
        cursor = con.execute(stmt.query)
        is_dml_returning = stmt.type.name in {"INSERT", "UPDATE", "DELETE"} and "RETURNING" in stmt.query.upper()
        if stmt.type.name in RESULT_TYPES or is_dml_returning:
            print_result(cursor)


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    filters = sys.argv[1:]
    con = duckdb.connect()
    for path in sorted(ROOT.rglob("*.sql")):
        rel = path.relative_to(ROOT).as_posix()
        if rel.startswith("00_setup") or not filters or any(f in rel for f in filters):
            run_file(con, path)


if __name__ == "__main__":
    main()
