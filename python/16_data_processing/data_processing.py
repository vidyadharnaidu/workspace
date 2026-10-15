"""
Data processing questions: the "employee/salary/word count/log file" round, with the standard library only.

Run: python python/16_data_processing/data_processing.py

REMEMBER:
  * group + aggregate   -> defaultdict(list) or Counter (no sorting needed)
  * itertools.groupby   -> ONLY works on data already sorted by the same key
  * max per group       -> max(group, key=...)  |  Nth highest -> sorted(set(...), reverse=True)[n-1]
  * running totals      -> itertools.accumulate
  * text                -> re.findall(r"[a-z']+", text.lower()) + Counter
  * big files           -> iterate the file object (lazy); never read it all into memory
"""
import csv
import io
import json
import re
import statistics
from collections import Counter, defaultdict
from dataclasses import dataclass
from itertools import accumulate, groupby
from operator import attrgetter, itemgetter


def check(label, actual, expected):
    assert actual == expected, f"{label}: expected {expected!r}, got {actual!r}"
    print(f"{label:<34} {actual!r}")


@dataclass
class Employee:
    name: str
    dept: str
    salary: float
    age: int
    gender: str


EMPLOYEES = [
    Employee("Alice", "IT", 120000, 30, "F"),
    Employee("Bob", "IT", 95000, 45, "M"),
    Employee("Carol", "HR", 70000, 28, "F"),
    Employee("Dan", "HR", 82000, 35, "M"),
    Employee("Eve", "Sales", 60000, 25, "F"),
    Employee("Frank", "Sales", 99000, 50, "M"),
    Employee("Grace", "IT", 120000, 38, "F"),
]


def highest_paid_per_dept(employees):
    by_dept = defaultdict(list)
    for e in employees:
        by_dept[e.dept].append(e)
    return {dept: max(members, key=attrgetter("salary")).name for dept, members in by_dept.items()}


def highest_paid_per_dept_groupby(employees):
    """itertools.groupby needs SORTED input - the classic mistake."""
    ordered = sorted(employees, key=attrgetter("dept"))
    return {dept: max(group, key=attrgetter("salary")).name
            for dept, group in groupby(ordered, key=attrgetter("dept"))}


def nth_highest_salary(employees, n):
    salaries = sorted({e.salary for e in employees}, reverse=True)
    return salaries[n - 1] if n <= len(salaries) else None


def average_salary_per_dept(employees):
    sums, counts = defaultdict(float), Counter()
    for e in employees:
        sums[e.dept] += e.salary
        counts[e.dept] += 1
    return {dept: round(total / counts[dept], 2) for dept, total in sums.items()}


def count_per_dept(employees):
    return Counter(e.dept for e in employees)


def names_per_dept(employees):
    groups = defaultdict(list)
    for e in employees:
        groups[e.dept].append(e.name)
    return dict(groups)


def partition_by(employees, predicate):
    result = {True: [], False: []}
    for e in employees:
        result[bool(predicate(e))].append(e.name)
    return result


def salary_statistics(employees):
    salaries = [e.salary for e in employees]
    return {
        "count": len(salaries),
        "min": min(salaries),
        "max": max(salaries),
        "mean": round(statistics.mean(salaries), 2),
        "median": statistics.median(salaries),
    }


def top_n_earners(employees, n):
    return [e.name for e in sorted(employees, key=lambda e: (-e.salary, e.name))[:n]]


def dept_with_most_employees(employees):
    return count_per_dept(employees).most_common(1)[0][0]


def give_raise(employees, dept, pct):
    """Return NEW objects instead of mutating (dataclasses.replace also works)."""
    return [Employee(e.name, e.dept, round(e.salary * (1 + pct), 2), e.age, e.gender) if e.dept == dept else e
            for e in employees]


# ---------------------------------------------------------------- text processing

def word_frequencies(text, top=None):
    return Counter(re.findall(r"[a-z']+", text.lower())).most_common(top)


def top_k_words_with_ties(text, k):
    """Ties broken alphabetically - what interviewers usually want."""
    counts = Counter(re.findall(r"[a-z']+", text.lower()))
    return sorted(counts.items(), key=lambda kv: (-kv[1], kv[0]))[:k]


def longest_words(text, n):
    return sorted(set(re.findall(r"[a-z']+", text.lower())), key=lambda w: (-len(w), w))[:n]


# ---------------------------------------------------------------- CSV / JSON / logs

CSV_TEXT = """order_id,customer,amount,status
1,acme,120.50,completed
2,globex,80.00,cancelled
3,acme,45.25,completed
4,initech,300.00,completed
"""


def revenue_per_customer(csv_text):
    reader = csv.DictReader(io.StringIO(csv_text))
    totals = defaultdict(float)
    for row in reader:
        if row["status"] == "completed":
            totals[row["customer"]] += float(row["amount"])
    return dict(sorted(totals.items(), key=itemgetter(1), reverse=True))


def csv_to_json(csv_text):
    rows = list(csv.DictReader(io.StringIO(csv_text)))
    return json.dumps(rows[:1], separators=(",", ":"))


def flatten_json(obj, prefix=""):
    """{'a': {'b': 1}} -> {'a.b': 1} - very common interview task."""
    flat = {}
    for key, value in obj.items():
        path = f"{prefix}{key}"
        if isinstance(value, dict):
            flat.update(flatten_json(value, path + "."))
        elif isinstance(value, list):
            for i, item in enumerate(value):
                if isinstance(item, dict):
                    flat.update(flatten_json(item, f"{path}[{i}]."))
                else:
                    flat[f"{path}[{i}]"] = item
        else:
            flat[path] = value
    return flat


LOG_TEXT = """10.0.0.1 - GET /home 200 0.120
10.0.0.2 - GET /api/users 500 1.500
10.0.0.1 - POST /api/orders 201 0.300
10.0.0.3 - GET /home 200 0.090
10.0.0.1 - GET /missing 404 0.010
10.0.0.2 - GET /api/users 500 2.100
"""

LOG_PATTERN = re.compile(r"(?P<ip>\S+) - (?P<method>\S+) (?P<path>\S+) (?P<status>\d+) (?P<seconds>[\d.]+)")


def parse_logs(log_text):
    return [m.groupdict() for m in LOG_PATTERN.finditer(log_text)]


def log_summary(log_text):
    entries = parse_logs(log_text)
    errors = [e for e in entries if e["status"].startswith("5")]
    slowest = max(entries, key=lambda e: float(e["seconds"]))
    return {
        "requests": len(entries),
        "by_status": dict(Counter(e["status"] for e in entries)),
        "error_rate_pct": round(100 * len(errors) / len(entries), 1),
        "top_ip": Counter(e["ip"] for e in entries).most_common(1)[0][0],
        "slowest_path": slowest["path"],
    }


# ---------------------------------------------------------------- generic dict/list utilities

def merge_dicts_summing(a, b):
    return dict(Counter(a) + Counter(b))


def invert_dict(d):
    """Values become keys; duplicates collect into lists."""
    inverted = defaultdict(list)
    for key, value in d.items():
        inverted[value].append(key)
    return dict(inverted)


def dedupe_records(records, key, keep="last"):
    """Keep the first or last record per key (like a database upsert)."""
    result = {}
    for record in records:
        if keep == "last" or record[key] not in result:
            result[record[key]] = record
    return list(result.values())


def pivot(records, row_key, col_key, value_key):
    table = defaultdict(lambda: defaultdict(float))
    for r in records:
        table[r[row_key]][r[col_key]] += r[value_key]
    return {row: dict(cols) for row, cols in table.items()}


def running_balance(transactions):
    return list(accumulate(transactions))


def read_large_file_lazily(file_like, keyword):
    """Iterating a file object reads one line at a time: works on a 10 GB file."""
    return sum(1 for line in file_like if keyword in line)


if __name__ == "__main__":
    check("highest paid per dept", highest_paid_per_dept(EMPLOYEES), {"IT": "Alice", "HR": "Dan", "Sales": "Frank"})
    check("same via itertools.groupby", highest_paid_per_dept_groupby(EMPLOYEES), {"HR": "Dan", "IT": "Alice", "Sales": "Frank"})
    check("2nd highest salary", nth_highest_salary(EMPLOYEES, 2), 99000)
    check("average salary per dept", average_salary_per_dept(EMPLOYEES), {"IT": 111666.67, "HR": 76000.0, "Sales": 79500.0})
    check("count per dept", dict(count_per_dept(EMPLOYEES)), {"IT": 3, "HR": 2, "Sales": 2})
    check("names per dept", names_per_dept(EMPLOYEES),
          {"IT": ["Alice", "Bob", "Grace"], "HR": ["Carol", "Dan"], "Sales": ["Eve", "Frank"]})
    check("partition salary > 90k", partition_by(EMPLOYEES, lambda e: e.salary > 90000),
          {True: ["Alice", "Bob", "Frank", "Grace"], False: ["Carol", "Dan", "Eve"]})
    check("salary statistics", salary_statistics(EMPLOYEES),
          {"count": 7, "min": 60000, "max": 120000, "mean": 92285.71, "median": 95000})
    check("top 3 earners", top_n_earners(EMPLOYEES, 3), ["Alice", "Grace", "Frank"])
    check("dept with most employees", dept_with_most_employees(EMPLOYEES), "IT")
    check("10% raise for IT", [e.salary for e in give_raise(EMPLOYEES, "IT", 0.10) if e.dept == "IT"],
          [132000.0, 104500.0, 132000.0])

    text = "The quick brown fox jumps over the lazy dog. The dog barks; the fox runs!"
    check("word frequencies top 3", word_frequencies(text, 3), [("the", 4), ("fox", 2), ("dog", 2)])
    check("top k with alphabetical ties", top_k_words_with_ties(text, 3), [("the", 4), ("dog", 2), ("fox", 2)])
    check("longest words", longest_words(text, 2), ["barks", "brown"])

    check("revenue per customer (CSV)", revenue_per_customer(CSV_TEXT), {"initech": 300.0, "acme": 165.75})
    check("csv row as JSON", csv_to_json(CSV_TEXT),
          '[{"order_id":"1","customer":"acme","amount":"120.50","status":"completed"}]')
    check("flatten nested JSON",
          flatten_json({"user": {"name": "ann", "roles": ["admin", "dev"]}, "active": True}),
          {"user.name": "ann", "user.roles[0]": "admin", "user.roles[1]": "dev", "active": True})

    check("log summary", log_summary(LOG_TEXT),
          {"requests": 6, "by_status": {"200": 2, "500": 2, "201": 1, "404": 1},
           "error_rate_pct": 33.3, "top_ip": "10.0.0.1", "slowest_path": "/api/users"})

    check("merge dicts summing", merge_dicts_summing({"a": 1, "b": 2}, {"b": 3, "c": 4}), {"a": 1, "b": 5, "c": 4})
    check("invert dict", invert_dict({"a": 1, "b": 2, "c": 1}), {1: ["a", "c"], 2: ["b"]})

    records = [{"id": 1, "v": "old"}, {"id": 2, "v": "x"}, {"id": 1, "v": "new"}]
    check("dedupe keep last", dedupe_records(records, "id"), [{"id": 1, "v": "new"}, {"id": 2, "v": "x"}])
    check("dedupe keep first", dedupe_records(records, "id", keep="first"), [{"id": 1, "v": "old"}, {"id": 2, "v": "x"}])

    sales = [{"region": "east", "quarter": "Q1", "amount": 10.0}, {"region": "east", "quarter": "Q2", "amount": 20.0},
             {"region": "west", "quarter": "Q1", "amount": 5.0}, {"region": "east", "quarter": "Q1", "amount": 7.0}]
    check("pivot table", pivot(sales, "region", "quarter", "amount"),
          {"east": {"Q1": 17.0, "Q2": 20.0}, "west": {"Q1": 5.0}})
    check("running balance", running_balance([1000, -200, -300, 500]), [1000, 800, 500, 1000])
    check("lazy file scan", read_large_file_lazily(io.StringIO(LOG_TEXT), "500"), 2)

    print("\nall checks passed")
