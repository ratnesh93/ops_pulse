Place MoveInSync CSV files here for Docker/Render image builds.

Required for ingest (filenames must match):
- Ride_data _trip-July_2026.csv
- Ride_data _trip-June_2026.csv
- bill_data.csv
- alerts_data.csv

Local docker-compose bind-mounts the dataset from the parent repo instead (see docker-compose.yml).
