# Ops Pulse

Agentic intelligence layer for enterprise employee mobility — MoveInSync hackathon demo.

Ops Pulse ingests real MoveInSync trip data, benchmarks vendor SLA performance, and runs an agent loop that surfaces findings and draft actions for human confirmation.

## Features

- **Morning brief** — pre-shift summary with attention count, vendors below SLA, OTA rank, cost at risk
- **Vendor scorecard** — 23 vendors with OTA rank, gap vs peer, cost per on-time trip
- **Agent loop** — SENSE → REASON → ACT with audit trail
- **3 findings + 3 confirmable actions:**
  - `VENDOR_SLA_BREACH` → `ESCALATE_VENDOR` (Rohan Travel ~64% OTA vs 90% SLA)
  - `SAFETY_ESCALATION` → `ESCALATE_SAFETY` (July Sev-1 alerts from `alerts_data.csv`)
  - `CAPACITY_SHORTFALL` → `ADD_CAPACITY` (overbooked office/shift from trip occupancy)
- **Leadership memo** — Facilities Head view with copy-to-clipboard
- **Chat** — text + optional voice input (Sarvam STT)

## Stack

| Layer | Tech |
|---|---|
| Backend | Java 17, Spring Boot 3, PostgreSQL 16, Flyway |
| Frontend | React 18, Vite |
| Infra | Docker Compose |

## Prerequisites

- **Docker** and **Docker Compose** (v2)
- **MoveInSync dataset** — CSV files on disk (not committed to this repo)
- *(Optional)* Sarvam API key for voice chat — [indus.sarvam.ai](https://indus.sarvam.ai/)

## Setup (first time)

### 1. Clone and place the dataset

The compose file expects the anonymised dataset at a path **relative to `opspulse/`**:

```
moveinsync_hackthon/
├── data/
│   └── MoveInSync - Anonymised Trip-Log Dataset-.../
│       └── MoveInSync - Anonymised Trip-Log Dataset/
│           ├── Ride_data _trip-July_2026.csv
│           ├── Ride_data _trip-June_2026.csv
│           ├── bill_data.csv
│           ├── alerts_data.csv
│           └── ...
└── opspulse/          ← you run commands from here
    └── docker-compose.yml
```

If your dataset lives elsewhere, edit the volume mount in `docker-compose.yml`:

```yaml
volumes:
  - /absolute/path/to/dataset:/data/moveinsync:ro
```

### 2. Configure environment (optional)

Voice chat needs a Sarvam key. Text chat works without it.

```bash
cd opspulse
cp .env.example .env
# Edit .env and set SARVAM_API_KEY=your-key-here
```

### 3. Start the stack

```bash
cd opspulse
docker compose up --build -d
```

**First boot** loads ~216K July trips + bill costs + June vendor stats. Expect **30–90 seconds** before the API is ready. Subsequent boots skip ingest (stored in Postgres volume).

### 4. Verify

```bash
curl http://localhost:8090/api/health
# {"status":"UP"}

curl http://localhost:8090/api/brief | head
```

Open the dashboard: **http://localhost:4210**

Hard-refresh after frontend updates: `Ctrl+Shift+R` (or `Cmd+Shift+R` on Mac).

## URLs

| Service | URL |
|---|---|
| Dashboard | http://localhost:4210 |
| API (direct) | http://localhost:8090 |
| Brief | http://localhost:8090/api/brief |
| Health | http://localhost:8090/api/health |

Ports **4210** (UI) and **8090** (API) avoid conflicts with common 4200/8080 defaults.

## Demo flow (~2 minutes)

1. Open http://localhost:4210 — morning brief + KPI bar (Rohan Travel OTA breach)
2. **Transport Manager** — review 3 findings and 3 pending actions
3. Click **Confirm** on `ESCALATE_VENDOR`, `ESCALATE_SAFETY`, and `ADD_CAPACITY`
4. Watch the **Agent Activity Log** update (`ACT → CONFIRMED`)
5. **Facilities Head** — read and copy the leadership memo
6. **Vendors** — sort by rank, peer gap, cost/on-time
7. **Chat** — ask about OTA, vendors, or use the mic (if Sarvam key is set)
8. Click **Run Agent Now** to replay the agent cycle

## API endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/health` | Health check |
| GET | `/api/brief` | KPIs, morning brief, findings, pending actions |
| GET | `/api/vendors` | All vendor scorecard rows |
| POST | `/api/agent/run` | Trigger agent cycle |
| POST | `/api/actions/{id}/confirm` | Confirm a pending action |
| GET | `/api/activity-log` | Agent audit trail |
| GET | `/api/leadership/memo` | Leadership memo text |
| POST | `/api/chat` | Text chat |
| POST | `/api/chat/speech` | Voice → STT → chat (needs `SARVAM_API_KEY`) |

## Development

### Rebuild backend after Java changes

```bash
./scripts/rebuild-backend.sh
```

This builds the JAR inside a Maven Docker container and hot-swaps it into the running backend — no Docker Hub pull required.

### Rebuild frontend after React changes

```bash
docker compose build frontend && docker compose up -d frontend
```

### Full rebuild (recreates containers)

```bash
docker compose up --build -d
```

> **Note:** `docker compose up -d` recreates containers from images and **wipes hot-swapped JARs**. After a full compose up, run `./scripts/rebuild-backend.sh` again if you were using hot-swap.

### Run backend tests locally

```bash
cd backend
docker run --rm -v "$PWD:/app" -w /app maven:3.9-eclipse-temurin-17 mvn test
```

### Reset database (re-ingest from scratch)

```bash
docker compose down -v    # removes pgdata volume
docker compose up -d      # triggers fresh ingest
```

## Environment variables

Set in `docker-compose.yml` or `.env`:

| Variable | Default | Purpose |
|---|---|---|
| `DATA_PATH` | `/data/moveinsync` | CSV folder inside container |
| `TRIP_FILE` | `Ride_data _trip-July_2026.csv` | July trips |
| `PRIOR_TRIP_FILE` | `Ride_data _trip-June_2026.csv` | Prior month for trends |
| `BILL_FILE` | `bill_data.csv` | Trip costs |
| `ANALYSIS_VENDOR` | `Rohan Mikhailov Travel` | Focus vendor |
| `PEER_VENDOR` | `Priya Mikhailov Travel` | Peer benchmark |
| `SLA_OTA_PCT` | `90` | SLA target (%) |
| `SKIP_DATA_LOAD` | `false` | Skip CSV ingest on startup |
| `SARVAM_API_KEY` | *(empty)* | Sarvam speech-to-text for voice chat |

## Troubleshooting

### Docker build fails (registry / IPv6)

```bash
# Use cached images only
docker compose build --pull=false

# Or hot-swap backend without rebuilding the image
./scripts/rebuild-backend.sh
```

### API returns 502 / connection refused

Backend may still be ingesting data on first boot. Wait 60–90s and check logs:

```bash
docker compose logs -f backend
```

Look for `Ops Pulse ready`.

### UI shows stale content after deploy

Hard-refresh the browser (`Ctrl+Shift+R`). The nginx config disables caching for `index.html`.

### Voice chat not working

1. Confirm `SARVAM_API_KEY` is set in `.env`
2. Restart backend: `docker compose up -d backend`
3. Check `/api/chat/status` for STT availability

### Dataset not found

```
DATA_PATH not found: /data/moveinsync
```

Fix the volume mount in `docker-compose.yml` so it points to your local CSV folder.

## Project layout

```
opspulse/
├── docker-compose.yml
├── .env.example
├── scripts/
│   └── rebuild-backend.sh
├── backend/
│   └── src/main/java/com/moveinsync/opspulse/
│       ├── agent/          # AgentOrchestrator
│       ├── benchmark/      # SLA metrics, morning brief, insights
│       ├── api/            # REST controllers
│       ├── data/           # CSV ingest
│       └── narration/      # Finding / memo templates
└── frontend/
    └── src/
        ├── App.jsx
        ├── MorningBriefBanner.jsx
        ├── VendorPanel.jsx
        └── ChatPanel.jsx
```

## Deploy on Render.com (same pattern as [aimyexp-sys/travel](https://github.com/aimyexp-sys/travel))

```
opspulse/
├── render.yaml              # Blueprint: API + UI + Postgres
├── docker-compose.yml
├── backend/Dockerfile       # dockerContext: ./backend
├── backend/data/            # Optional CSVs for Render image
└── frontend/
    ├── Dockerfile           # dockerContext: ./frontend
    └── nginx.conf.template  # Proxies /api → backend
```

| | Local (Compose) | Render |
|--|-----------------|--------|
| Backend | `build: ./backend` | `dockerfilePath: ./backend/Dockerfile`, `dockerContext: ./backend` |
| Frontend | nginx → `http://backend:8080` | nginx → `https://{api-host}.onrender.com` |
| Browser | Same-origin `/api/*` | Same-origin `/api/*` (no `VITE_API_BASE_URL`) |

Frontend Render env vars (in `render.yaml`): `BACKEND_SCHEME=https`, `BACKEND_HOST` from API service, `BACKEND_DOMAIN_SUFFIX=.onrender.com`.

### Deploy

1. Push to GitHub → Render **New Blueprint** → apply `render.yaml`
2. Set `SARVAM_API_KEY` and `OPENAI_API_KEY` in the dashboard
3. Copy CSVs into `backend/data/` before build, or upload to `/data/moveinsync` on the API service, then set `SKIP_DATA_LOAD=false`

### URLs after deploy

| Service | URL |
|---|---|
| Dashboard | `https://opspulse-web.onrender.com` |
| API | `https://opspulse-api.onrender.com` |

## Further reading

- Build spec: [../docs/OPS_PULSE_FINAL_PLAN.md](../docs/OPS_PULSE_FINAL_PLAN.md)
- Dataset dictionary: [../data/.../Dictionary/README.md](../data/MoveInSync%20-%20Anonymised%20Trip-Log%20Dataset-20260905T010918Z-1-001/MoveInSync%20-%20Anonymised%20Trip-Log%20Dataset/Dictionary/README.md)
