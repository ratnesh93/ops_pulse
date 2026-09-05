# Ops Pulse

Agentic intelligence layer for enterprise employee mobility — demo build for MoveInSync hackathon.

## Stack

- **Backend:** Java 17, Spring Boot 3, PostgreSQL 16
- **Frontend:** React (Vite)
- **Infra:** Docker Compose

## Quick Start

```bash
cd opspulse
docker compose up --build
```

First boot loads ~216K July trips + bill data (~30 seconds on SSD). Subsequent boots skip ingest.

- **Dashboard:** http://localhost:4210
- **API:** http://localhost:8090/api/brief
- **Health:** http://localhost:8090/api/health

> Ports 4200/8080 are used if available; current compose maps **4210** (UI) and **8090** (API).

## Troubleshooting Docker build (registry / IPv6)

If `docker compose build` fails with `network is unreachable` on `registry-1.docker.io` (IPv6):

```bash
# Option 1: use cached layers only (works if you built successfully before)
docker compose build --pull=false backend

# Option 2: hot-swap JAR into running container (no image rebuild)
./scripts/rebuild-backend.sh   # requires Maven in .tools/ (see script)

# Option 3: prefer IPv4 for Docker (system-wide, needs sudo)
# Add to /etc/docker/daemon.json: { "ip6tables": false }
# Or /etc/gai.conf: precedence ::ffff:0:0/96  100
```

If containers are already running (`docker compose ps`), you can demo without rebuilding.

## Demo Flow (90 seconds)

1. Open http://localhost:4210 — brief shows Rohan Travel OTA breach (~64% vs 90% SLA)
2. Transport Manager view — review finding + pending `ESCALATE_VENDOR`
3. Click **Confirm** — action confirmed, audit log updates
4. Switch to **Facilities Head** — copy leadership memo
5. Click **Run Agent Now** — activity log replays SENSE → REASON → ACT

## Plan

See [../docs/OPS_PULSE_FINAL_PLAN.md](../docs/OPS_PULSE_FINAL_PLAN.md) for full build spec (Direction A).

## Env Vars

| Variable | Default | Purpose |
|---|---|---|
| `DATA_PATH` | `/data/moveinsync` | CSV folder mount |
| `ANALYSIS_VENDOR` | Rohan Mikhailov Travel | Focus vendor |
| `PEER_VENDOR` | Priya Mikhailov Travel | Peer benchmark |
| `SLA_OTA_PCT` | 90 | SLA target |
| `SKIP_DATA_LOAD` | false | Skip ingest on restart |
| `SARVAM_API_KEY` | *(empty)* | Sarvam speech-to-text — get key at [indus.sarvam.ai](https://indus.sarvam.ai/) |

### Sarvam speech input (chat)

1. Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```
2. Add your API key:
   ```
   SARVAM_API_KEY=your-key-here
   ```
3. Restart backend:
   ```bash
   docker compose up -d backend
   ```
4. Open **Chat** tab in the UI — use text or 🎤 mic button.

## Project Layout

```
opspulse/
├── docker-compose.yml
├── backend/          # Spring Boot API + agent
├── frontend/         # React dashboard
└── README.md
```
