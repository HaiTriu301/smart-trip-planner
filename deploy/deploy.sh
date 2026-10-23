#!/usr/bin/env bash
# Puts the newest images on the server (design.md 17.2; WORKFLOW Task 4.8, decisions 4, 17).
# Run by GitHub Actions over SSH after the owner approves the deploy job: the key of GitHub may run this script and
# nothing else (command= in ~deploy/.ssh/authorized_keys, docs/deploy/aws.md 16.3). Can also be run by hand:
#
#   /opt/trip-planner/deploy.sh                     on the server, as the user deploy
#   APP_DIR=/other/folder ./deploy.sh               the application lives elsewhere
#
# Steps: copy of the database, pull, start the new containers, wait until the backend and nginx are healthy, remove
# the images left behind. A backend that does not become healthy fails the run (and the GitHub job) with its last
# log lines. The tag comes from IMAGE_TAG in .env (latest by default; docs/deploy/aws.md 18.4 to go back).
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/trip-planner}"
cd "$APP_DIR"

# Longest wait for a container to become healthy. The backend may take up to 2 minutes to start (start_period of
# its HEALTHCHECK) and a new image can carry Flyway migrations
HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-180}"

log() {
  echo "$(date -u '+%F %T') $*"
}

compose() {
  docker compose -f docker-compose.prod.yml "$@"
}

# One deploy at a time: a second one started meanwhile (by hand, or a retried job) stops here instead of pulling
# and restarting the same containers at the same time
exec 9>"$APP_DIR/.deploy.lock"
if ! flock -n 9; then
  log "deploy FAILED: another deploy is running"
  exit 1
fi

# Prints the health of the running container of a service: starting, healthy, unhealthy, or missing when no
# container runs (it stopped, or Docker is restarting it)
health_of() {
  local id
  id="$(compose ps -q "$1")"
  if [ -z "$id" ]; then
    echo missing
    return
  fi
  docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$id"
}

wait_healthy() {
  local service="$1" waited=0 status
  while [ "$waited" -lt "$HEALTH_TIMEOUT_SECONDS" ]; do
    status="$(health_of "$service")"
    if [ "$status" = healthy ]; then
      log "$service is healthy after ${waited}s"
      return 0
    fi
    if [ "$status" = unhealthy ]; then
      break
    fi
    sleep 5
    waited=$((waited + 5))
  done
  log "deploy FAILED: $service is $(health_of "$service") after ${waited}s; its last log lines:"
  compose logs --no-color --tail 100 "$service" || true
  return 1
}

log "deploy started in $APP_DIR"

# The copy runs before anything changes: a new image may migrate the schema (Flyway), and the copy is the way back
# (docs/deploy/aws.md 15.4). The very first deploy has no database yet, so nothing to copy
if [ -n "$(compose ps --status running -q mysql)" ]; then
  ./backup.sh
else
  log "mysql is not running: no backup before this deploy"
fi

log "pulling the images"
compose pull --quiet

# Recreates only the containers whose image or settings changed; MySQL and Redis keep running when theirs did not.
# --remove-orphans: a service removed from the compose file does not keep running
log "starting the containers"
compose up -d --remove-orphans

wait_healthy backend
wait_healthy nginx

# The previous "latest" images are left without a tag after the pull: remove them so the disk does not fill up.
# Tagged images (an older commit set in IMAGE_TAG) stay; docs/deploy/aws.md 18.5 removes those by hand
docker image prune -f > /dev/null

compose images
log "deploy ok"
