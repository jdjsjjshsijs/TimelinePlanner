import sqlite3
import json
import csv
import io
from datetime import datetime
import time
from flask import Flask, request, jsonify, Response

app = Flask(__name__)

@app.after_request
def add_cors_headers(response):
    response.headers['Access-Control-Allow-Origin'] = '*'
    response.headers['Access-Control-Allow-Headers'] = 'Content-Type'
    response.headers['Access-Control-Allow-Methods'] = 'GET, POST, PUT, DELETE, OPTIONS'
    return response

DB_PATH = "timeline_planner.db"

def log(msg):
    print(f"[SERVER] {msg}", flush=True)


def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    return conn


def init_db():
    with get_db() as conn:
        conn.execute("""
            CREATE TABLE IF NOT EXISTS tasks (
                id INTEGER PRIMARY KEY,
                title TEXT NOT NULL,
                date_millis INTEGER NOT NULL,
                start_minute INTEGER NOT NULL,
                end_minute INTEGER NOT NULL,
                color TEXT DEFAULT '#4A90D9',
                notes TEXT DEFAULT '',
                order_index INTEGER DEFAULT 0,
                pause_segments TEXT DEFAULT '[]',
                updated_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)
        conn.execute("""
            CREATE TABLE IF NOT EXISTS practice_subjects (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                color TEXT DEFAULT '#4A90D9',
                created_at INTEGER NOT NULL
            )
        """)
        conn.execute("""
            CREATE TABLE IF NOT EXISTS practice_records (
                id INTEGER PRIMARY KEY,
                subject_id INTEGER NOT NULL,
                total_questions INTEGER NOT NULL,
                correct_questions INTEGER NOT NULL,
                accuracy REAL NOT NULL,
                date_millis INTEGER NOT NULL,
                notes TEXT DEFAULT '',
                created_at INTEGER DEFAULT 0,
                updated_at TEXT DEFAULT '',
                FOREIGN KEY(subject_id) REFERENCES practice_subjects(id) ON DELETE CASCADE
            )
        """)
        conn.execute("""
            CREATE TABLE IF NOT EXISTS courses (
                id INTEGER PRIMARY KEY,
                title TEXT NOT NULL,
                location TEXT DEFAULT '',
                teacher TEXT DEFAULT '',
                days_of_week TEXT NOT NULL,
                start_minute INTEGER NOT NULL,
                end_minute INTEGER NOT NULL,
                color TEXT DEFAULT '#4A90D9',
                notes TEXT DEFAULT '',
                start_date INTEGER NOT NULL,
                end_date INTEGER NOT NULL
            )
        """)
        conn.execute("""
            CREATE TABLE IF NOT EXISTS goals (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                deadline_millis INTEGER NOT NULL,
                color TEXT DEFAULT '#E74C3C',
                created_at INTEGER NOT NULL
            )
        """)
        # Ensure columns exist for older databases
        try:
            conn.execute("ALTER TABLE practice_records ADD COLUMN created_at INTEGER DEFAULT 0")
        except Exception:
            pass
        try:
            conn.execute("ALTER TABLE practice_records ADD COLUMN updated_at TEXT DEFAULT ''")
        except Exception:
            pass
        conn.commit()


init_db()


@app.route("/api/tasks/sync", methods=["POST"])
def sync_tasks():
    data = request.get_json()
    if not data or "tasks" not in data:
        return jsonify({"error": "missing tasks"}), 400

    tasks = data["tasks"]
    date_millis = data.get("dateMillis")

    with get_db() as conn:
        if date_millis:
            conn.execute("DELETE FROM tasks WHERE date_millis = ?", (date_millis,))

        for t in tasks:
            conn.execute("""
                INSERT OR REPLACE INTO tasks
                (id, title, date_millis, start_minute, end_minute, color, notes, order_index, pause_segments, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                t["id"], t["title"], t["dateMillis"],
                t["startMinute"], t["endMinute"],
                t.get("color", "#4A90D9"), t.get("notes", ""),
                t.get("orderIndex", 0),
                json.dumps(t.get("pauseSegments", [])),
                datetime.now().isoformat()
            ))
        conn.commit()

    log(f"Tasks synced: {len(tasks)} tasks for date {date_millis}")
    return jsonify({"ok": True, "count": len(tasks)})


@app.route("/api/tasks", methods=["GET"])
def get_tasks():
    date_millis = request.args.get("date", type=int)
    with get_db() as conn:
        if date_millis:
            rows = conn.execute(
                "SELECT * FROM tasks WHERE date_millis = ? ORDER BY start_minute",
                (date_millis,)
            ).fetchall()
        else:
            rows = conn.execute(
                "SELECT * FROM tasks ORDER BY date_millis, start_minute"
            ).fetchall()

    tasks = []
    for r in rows:
        segments = json.loads(r["pause_segments"]) if r["pause_segments"] else []
        tasks.append({
            "id": r["id"],
            "title": r["title"],
            "dateMillis": r["date_millis"],
            "startMinute": r["start_minute"],
            "endMinute": r["end_minute"],
            "color": r["color"],
            "notes": r["notes"],
            "orderIndex": r["order_index"],
            "pauseSegments": segments
        })
    return jsonify({"tasks": tasks})


@app.route("/api/tasks/all", methods=["GET"])
def get_all_tasks():
    with get_db() as conn:
        rows = conn.execute("SELECT * FROM tasks ORDER BY date_millis, start_minute").fetchall()

    tasks = []
    for r in rows:
        segments = json.loads(r["pause_segments"]) if r["pause_segments"] else []
        tasks.append({
            "id": r["id"],
            "title": r["title"],
            "dateMillis": r["date_millis"],
            "startMinute": r["start_minute"],
            "endMinute": r["end_minute"],
            "color": r["color"],
            "notes": r["notes"],
            "orderIndex": r["order_index"],
            "pauseSegments": segments
        })
    return jsonify({"tasks": tasks})


@app.route("/api/tasks/<int:task_id>", methods=["DELETE"])
def delete_task(task_id):
    with get_db() as conn:
        conn.execute("DELETE FROM tasks WHERE id = ?", (task_id,))
        conn.commit()
    log(f"Task [{task_id}] deleted")
    return jsonify({"ok": True})


# ── Practice endpoints ───────────────────────────────────────────────

@app.route("/api/practice/sync", methods=["POST"])
def sync_practice():
    data = request.get_json()
    if not data:
        return jsonify({"error": "missing data"}), 400
    subjects = data.get("subjects", [])
    records = data.get("records", [])
    now_iso = datetime.now().isoformat()
    with get_db() as conn:
        for s in subjects:
            conn.execute(
                "INSERT OR REPLACE INTO practice_subjects (id, name, color, created_at) VALUES (?, ?, ?, ?)",
                (s["id"], s["name"], s.get("color", "#4A90D9"), s["createdAt"]),
            )
        for r in records:
            created_at = r.get("createdAtMillis", 0) or 0
            conn.execute(
                "INSERT OR REPLACE INTO practice_records (id, subject_id, total_questions, correct_questions, accuracy, date_millis, notes, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                (r["id"], r["subjectId"], r["totalQuestions"], r["correctQuestions"],
                 r["accuracy"], r["dateMillis"], r.get("notes", ""), created_at, now_iso),
            )
        conn.commit()
    log(f"Practice synced: {len(subjects)} subjects, {len(records)} records")
    return jsonify({"ok": True, "count": len(subjects) + len(records)})


@app.route("/api/practice/all", methods=["GET"])
def get_all_practice():
    with get_db() as conn:
        subjects = [dict(r) for r in conn.execute("SELECT * FROM practice_subjects").fetchall()]
        records = [dict(r) for r in conn.execute("SELECT * FROM practice_records").fetchall()]
    log(f"Fetch practice: {len(subjects)} subjects, {len(records)} records")
    return jsonify({
        "subjects": [{"id": s["id"], "name": s["name"], "color": s["color"], "createdAt": s["created_at"]} for s in subjects],
        "records": [{"id": r["id"], "subjectId": r["subject_id"], "totalQuestions": r["total_questions"],
                      "correctQuestions": r["correct_questions"], "accuracy": r["accuracy"],
                      "dateMillis": r["date_millis"], "notes": r["notes"],
                      "createdAtMillis": r["created_at"] or 0} for r in records],
    })


@app.route("/api/practice/records/<int:record_id>", methods=["DELETE"])
def delete_practice_record(record_id):
    with get_db() as conn:
        conn.execute("DELETE FROM practice_records WHERE id = ?", (record_id,))
        conn.commit()
    log(f"Practice record [{record_id}] deleted")
    return jsonify({"ok": True})


@app.route("/api/practice/subjects/<int:subject_id>", methods=["DELETE"])
def delete_practice_subject(subject_id):
    with get_db() as conn:
        conn.execute("DELETE FROM practice_records WHERE subject_id = ?", (subject_id,))
        conn.execute("DELETE FROM practice_subjects WHERE id = ?", (subject_id,))
        conn.commit()
    log(f"Practice subject [{subject_id}] deleted")
    return jsonify({"ok": True})


# ── Course endpoints ─────────────────────────────────────────────────

@app.route("/api/courses/sync", methods=["POST"])
def sync_courses():
    data = request.get_json()
    if not data:
        return jsonify({"error": "missing data"}), 400
    courses = data if isinstance(data, list) else data.get("courses", [])
    with get_db() as conn:
        for cr in courses:
            conn.execute(
                "INSERT OR REPLACE INTO courses (id, title, location, teacher, days_of_week, start_minute, end_minute, color, notes, start_date, end_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                (cr["id"], cr["title"], cr.get("location", ""), cr.get("teacher", ""),
                 cr["daysOfWeek"], cr["startMinute"], cr["endMinute"],
                 cr.get("color", "#4A90D9"), cr.get("notes", ""),
                 cr["startDate"], cr["endDate"]),
            )
        conn.commit()
    log(f"Courses synced: {len(courses)} courses")
    return jsonify({"ok": True, "count": len(courses)})


@app.route("/api/courses/all", methods=["GET"])
def get_all_courses():
    with get_db() as conn:
        rows = conn.execute("SELECT * FROM courses").fetchall()
    courses = []
    for r in rows:
        courses.append({
            "id": r["id"], "title": r["title"],
            "location": r["location"], "teacher": r["teacher"],
            "daysOfWeek": r["days_of_week"],
            "startMinute": r["start_minute"], "endMinute": r["end_minute"],
            "color": r["color"], "notes": r["notes"],
            "startDate": r["start_date"], "endDate": r["end_date"],
        })
    log(f"Fetch courses: {len(courses)} found")
    return jsonify({"courses": courses})


@app.route("/api/courses/<int:course_id>", methods=["DELETE"])
def delete_course(course_id):
    with get_db() as conn:
        conn.execute("DELETE FROM courses WHERE id = ?", (course_id,))
        conn.commit()
    log(f"Course [{course_id}] deleted")
    return jsonify({"ok": True})

# ── Goal endpoints ───────────────────────────────────────────────────

@app.route("/api/goals/sync", methods=["POST"])
def sync_goals():
    data = request.get_json()
    if not data:
        return jsonify({"error": "missing data"}), 400
    goals = data if isinstance(data, list) else data.get("goals", [])
    with get_db() as conn:
        for g in goals:
            conn.execute(
                "INSERT OR REPLACE INTO goals (id, name, deadline_millis, color, created_at) VALUES (?, ?, ?, ?, ?)",
                (g["id"], g["name"], g["deadlineMillis"], g.get("color", "#E74C3C"), g["createdAt"]),
            )
        conn.commit()
    log(f"Goals synced: {len(goals)} goals")
    return jsonify({"ok": True, "count": len(goals)})


@app.route("/api/goals/all", methods=["GET"])
def get_all_goals():
    with get_db() as conn:
        rows = conn.execute("SELECT * FROM goals").fetchall()
    goals = []
    for r in rows:
        goals.append({
            "id": r["id"], "name": r["name"],
            "deadlineMillis": r["deadline_millis"],
            "color": r["color"], "createdAt": r["created_at"],
        })
    log(f"Fetch goals: {len(goals)} found")
    return jsonify({"goals": goals})



@app.route("/api/goals/<int:goal_id>", methods=["DELETE"])
def delete_goal(goal_id):
    with get_db() as conn:
        conn.execute("DELETE FROM goals WHERE id = ?", (goal_id,))
        conn.commit()
    log(f"Goal [{goal_id}] deleted")
    return jsonify({"ok": True})

@app.route("/api/ping", methods=["GET"])
def ping():
    return jsonify({"ok": True, "timestamp": int(time.time() * 1000)})


if __name__ == "__main__":
    print("TimelinePlanner sync server running on 0.0.0.0:5000")
    app.run(host="0.0.0.0", port=5000, debug=False)




