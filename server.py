# server.py  -- run with:  python3 server.py
from flask import Flask, request
from apscheduler.schedulers.background import BackgroundScheduler
import requests
import datetime

app = Flask(__name__)

# ---- Supabase Configuration ----
SUPABASE_URL = "<URL>"
SUPABASE_API_KEY = "<KEY>"
SUPABASE_LOGS_TABLE = "logs"
SUPABASE_STATUS_TABLE = "status"

# ---- Common HTTP headers ----
HEADERS = {
    "apikey": SUPABASE_API_KEY,
    "Authorization": f"Bearer {SUPABASE_API_KEY}",
    "Content-Type": "application/json"
}

@app.route('/flame', methods=['POST'])
def flame():
    data = request.json
    esp32_id = data.get('esp32_id')
    status = data.get('status')

    if not esp32_id or not status:
        return {"error": "Missing esp32_id or status"}, 400

    timestamp = datetime.datetime.utcnow().isoformat()

    if status == 'fire':
        print(f"Fire detected from ESP32 [{esp32_id}]")
    else:
        print(f"Safe report from ESP32 [{esp32_id}]")

    # Insert into logs
    log_payload = {
        "esp32_id": esp32_id,
        "status": status,
        "timestamp": timestamp
    }

    try:
        res = requests.post(f"{SUPABASE_URL}/rest/v1/{SUPABASE_LOGS_TABLE}",
                            headers=HEADERS, json=log_payload)
        if res.status_code not in [200, 201]:
            print(f"Log insert failed: {res.status_code} {res.text}")
    except Exception as e:
        print(f"Error inserting log: {e}")

    # Upsert into status
    status_payload = {
        "esp32_id": esp32_id,
        "last_status": status,
        "last_updated": timestamp
    }

    try:
	# Copy headers so we can safely add Prefer
        headers_with_upsert = HEADERS.copy()
        headers_with_upsert["Prefer"] = "resolution=merge-duplicates"
        res2 = requests.post( f"{SUPABASE_URL}/rest/v1/{SUPABASE_STATUS_TABLE}?on_conflict=esp32_id", headers=headers_with_upsert, json=status_payload)
        if res2.status_code not in [200, 201]:
            print(f"Status upsert failed: {res2.status_code} {res2.text}")
    except Exception as e:
        print(f"Error upserting status: {e}")

    return {"message": "Data received"}, 200


def check_failed_devices():
    """Mark ESP32 as Failed if no data for >10 minutes."""
    try:
        res = requests.get(f"{SUPABASE_URL}/rest/v1/{SUPABASE_STATUS_TABLE}",
                           headers=HEADERS)
        if res.status_code != 200:
            print(f"Failed to fetch statuses: {res.status_code} {res.text}")
            return

        devices = res.json()
        now = datetime.datetime.now(datetime.timezone.utc)

        for d in devices:
            last_time = datetime.datetime.fromisoformat(
                d['last_updated'].replace('Z', '+00:00')
            )
            delta = now - last_time

            if delta.total_seconds() > 600 and d['last_status'] != "failed":
                print(f"ESP32 [{d['esp32_id']}] inactive for >10 min, marking as failed")
                update_payload = {
                    "last_status": "failed",
                    "last_updated": now.isoformat()
                }
                requests.patch(
                    f"{SUPABASE_URL}/rest/v1/{SUPABASE_STATUS_TABLE}?esp32_id=eq.{d['esp32_id']}",
                    headers=HEADERS,
                    json=update_payload
                )
    except Exception as e:
        print(f"Error checking failed devices: {e}")


# ---- Scheduler ----
scheduler = BackgroundScheduler()
scheduler.add_job(check_failed_devices, 'interval', minutes=1)
scheduler.start()

if __name__ == '__main__':
    print("Flask server running with Supabase sync and failure monitor")
    app.run(host='0.0.0.0', port=5000)
