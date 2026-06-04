import sqlite3
import json
import csv
import io
from datetime import datetime
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
        conn.commit()


init_db()


@app.route("/api/tasks/sync", methods=["POST"])
def sync_tasks():
    """接收 Android 端全量同步的任务列表"""
    data = request.get_json()
    if not data or "tasks" not in data:
        return jsonify({"error": "missing tasks"}), 400

    tasks = data["tasks"]
    date_millis = data.get("dateMillis")

    with get_db() as conn:
        if date_millis:
            # 删除该日期的旧数据，用新数据替换
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
    for t in tasks:
        tid, tt, ts, te = t['id'], t['title'], t['startMinute'], t['endMinute']
        log(f'  - [{tid}] {tt} ({ts}-{te})')
    return jsonify({"ok": True, "count": len(tasks)})


@app.route("/api/tasks", methods=["GET"])
def get_tasks():
    """获取指定日期的任务"""
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
    """获取全部任务"""
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

    log(f"Fetch all tasks: {len(tasks)} found")
    return jsonify({"tasks": tasks})


@app.route("/api/tasks/<int:task_id>", methods=["DELETE"])
def delete_task(task_id):
    """删除指定任务"""
    with get_db() as conn:
        conn.execute("DELETE FROM tasks WHERE id = ?", (task_id,))
        conn.commit()
    log(f'Task [{task_id}] deleted')
    return jsonify({"ok": True})


def _get_all_tasks_flat():
    """获取全部任务，返回扁平化的字典列表"""
    with get_db() as conn:
        rows = conn.execute("SELECT * FROM tasks ORDER BY date_millis, start_minute").fetchall()

    result = []
    for r in rows:
        date_str = datetime.fromtimestamp(r["date_millis"] / 1000).strftime("%Y-%m-%d")
        start_h, start_m = divmod(r["start_minute"], 60)
        end_h, end_m = divmod(r["end_minute"], 60)
        duration = r["end_minute"] - r["start_minute"]
        segments = json.loads(r["pause_segments"]) if r["pause_segments"] else []
        pause_total = sum(s[1] - s[0] for s in segments)
        effective = duration - pause_total
        result.append({
            "日期": date_str,
            "任务名": r["title"],
            "开始时间": f"{start_h:02d}:{start_m:02d}",
            "结束时间": f"{end_h:02d}:{end_m:02d}",
            "时长(分钟)": effective,
            "备注": r["notes"] or "",
            "颜色": r["color"],
        })
    return result


@app.route("/api/tasks/export/csv", methods=["GET"])
def export_csv():
    """导出全部任务为 CSV"""
    tasks = _get_all_tasks_flat()
    if not tasks:
        return Response("暂无数据", status=204)

    output = io.StringIO()
    writer = csv.DictWriter(output, fieldnames=tasks[0].keys())
    writer.writeheader()
    writer.writerows(tasks)

    return Response(
        "﻿" + output.getvalue(),
        mimetype="text/csv",
        headers={"Content-Disposition": "attachment; filename=tasks_export.csv"}
    )


@app.route("/api/tasks/export/json", methods=["GET"])
def export_json():
    """导出全部任务为格式化 JSON"""
    tasks = _get_all_tasks_flat()
    return Response(
        json.dumps(tasks, ensure_ascii=False, indent=2),
        mimetype="application/json",
        headers={"Content-Disposition": "attachment; filename=tasks_export.json"}
    )




@app.route("/api/practice/sync", methods=["POST"])
def sync_practice():
    data = request.get_json()
    if not data:
        return jsonify({"error": "missing data"}), 400
    subjects = data.get("subjects", [])
    records = data.get("records", [])
    with get_db() as conn:
        for s in subjects:
            conn.execute(
                "INSERT OR REPLACE INTO practice_subjects (id, name, color, created_at) VALUES (?, ?, ?, ?)",
                (s["id"], s["name"], s.get("color", "#4A90D9"), s["createdAt"]),
            )
        for r in records:
            conn.execute(
                "INSERT OR REPLACE INTO practice_records (id, subject_id, total_questions, correct_questions, accuracy, date_millis, notes) VALUES (?, ?, ?, ?, ?, ?, ?)",
                (r["id"], r["subjectId"], r["totalQuestions"], r["correctQuestions"],
                 r["accuracy"], r["dateMillis"], r.get("notes", "")),
            )
        conn.commit()
    log(f"Practice synced: {len(subjects)} subjects, {len(records)} records")
    for s in subjects:
        sid, sn = s['id'], s['name']
        log(f'  - Subject [{sid}] {sn}]')
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
                      "dateMillis": r["date_millis"], "notes": r["notes"]} for r in records],
    })


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
    for cr in courses:
        cid, ct = cr['id'], cr['title']
        cd = cr.get('daysOfWeek', '?')
        cs, ce = cr.get('startMinute', 0), cr.get('endMinute', 0)
        log(f'  - [{cid}] {ct} days={cd} {cs//60}:{cs%60:02d}-{ce//60}:{ce%60:02d}')
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


if __name__ == "__main__":
    print("TimelinePlanner sync server running on 0.0.0.0:5000")
    app.run(host="0.0.0.0", port=5000, debug=False)
