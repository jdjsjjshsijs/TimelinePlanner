import sqlite3
import json
from datetime import datetime
from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

DB_PATH = "timeline_planner.db"


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

    return jsonify({"tasks": tasks})


@app.route("/api/tasks/<int:task_id>", methods=["DELETE"])
def delete_task(task_id):
    """删除指定任务"""
    with get_db() as conn:
        conn.execute("DELETE FROM tasks WHERE id = ?", (task_id,))
        conn.commit()
    return jsonify({"ok": True})


if __name__ == "__main__":
    print("TimelinePlanner sync server running on 0.0.0.0:5000")
    app.run(host="0.0.0.0", port=5000, debug=False)
