#!/usr/bin/env bash
# Optional wire-up into Odin native observability stack.
# Does NOT rewrite nginx unless --with-nginx; does NOT modify Home Executive Overview.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROM_CFG="${PROM_CFG:-/usr/local/etc/prometheus.yml}"
GRAFANA_DASH_DIR="${GRAFANA_DASH_DIR:-/usr/local/var/lib/grafana/dashboards}"
RULES_DIR="${ROOT}/observability/prometheus/rules"
DASH_SRC="${ROOT}/observability/grafana/dashboards/fcs-fulfilment-obs.json"
DASH_DST="${GRAFANA_DASH_DIR}/fcs-fulfilment-obs.json"
BEGIN_MARK="BEGIN FCS_INTERVIEW_SCRAPE"
END_MARK="END FCS_INTERVIEW_SCRAPE"
BEGIN_RULES="BEGIN FCS_INTERVIEW_RULES"
END_RULES="END FCS_INTERVIEW_RULES"
NGINX_SRC="${ROOT}/observability/nginx/fcs-app.conf"
NGINX_DST="${NGINX_DST:-/usr/local/etc/nginx/services/fcs-app.conf}"

ACTION="${1:-}"
WITH_NGINX=0
for arg in "${@:2}"; do
  [[ "$arg" == "--with-nginx" ]] && WITH_NGINX=1
done

die() { echo "BLOCK: $*" >&2; exit 2; }
info() { echo "==> $*"; }

require_file() { [[ -f "$1" ]] || die "missing file: $1"; }

backup_prom() {
  cp -p "$PROM_CFG" "${PROM_CFG}.bak-fcs-$(date +%Y%m%d%H%M%S)"
}

inject_scrape() {
  require_file "$PROM_CFG"
  if grep -qF "$BEGIN_MARK" "$PROM_CFG"; then
    info "scrape markers already present — skip inject"
    return 0
  fi
  if grep -qE 'job_name:[[:space:]]*["'\'']?fcs-fulfilment' "$PROM_CFG"; then
    die "job fcs-fulfilment exists without markers — resolve manually"
  fi
  backup_prom
  {
    echo ""
    echo "  # $BEGIN_MARK — managed by repo scripts/install-local-odin.sh"
    cat <<'EOF'
  - job_name: "fcs-fulfilment"
    scrape_interval: 10s
    scrape_timeout: 5s
    metrics_path: /q/metrics
    static_configs:
      - targets: ["127.0.0.1:8080"]
        labels:
          project: "fcs-interview"
          app: "fulfilment-warehouse"
          service: "fcs-fulfilment"
          env: "local"
EOF
    echo "  # $END_MARK"
  } >> "$PROM_CFG"
  info "appended scrape job to $PROM_CFG"
}

remove_scrape() {
  require_file "$PROM_CFG"
  if ! grep -qF "$BEGIN_MARK" "$PROM_CFG"; then
    info "no FCS scrape markers — nothing to remove"
    return 0
  fi
  backup_prom
  awk -v b="$BEGIN_MARK" -v e="$END_MARK" '
    index($0,b){skip=1; next}
    index($0,e){skip=0; next}
    !skip{print}
  ' "$PROM_CFG" > "${PROM_CFG}.tmp"
  mv "${PROM_CFG}.tmp" "$PROM_CFG"
  info "removed scrape block from $PROM_CFG"
}

inject_rules() {
  require_file "$PROM_CFG"
  if grep -qF "$BEGIN_RULES" "$PROM_CFG"; then
    info "rule_files markers already present — skip"
    return 0
  fi
  backup_prom
  {
    echo ""
    echo "# $BEGIN_RULES — managed by repo scripts/install-local-odin.sh"
    echo "rule_files:"
    echo "  - ${RULES_DIR}/*.yml"
    echo "# $END_RULES"
  } >> "$PROM_CFG"
  info "appended rule_files pointing at repo rules"
}

remove_rules() {
  require_file "$PROM_CFG"
  if ! grep -qF "$BEGIN_RULES" "$PROM_CFG"; then
    return 0
  fi
  backup_prom
  awk -v b="$BEGIN_RULES" -v e="$END_RULES" '
    index($0,b){skip=1; next}
    index($0,e){skip=0; next}
    !skip{print}
  ' "$PROM_CFG" > "${PROM_CFG}.tmp"
  mv "${PROM_CFG}.tmp" "$PROM_CFG"
  info "removed rule_files block"
}

install_dashboard() {
  require_file "$DASH_SRC"
  [[ -d "$GRAFANA_DASH_DIR" ]] || die "Grafana dashboards dir missing: $GRAFANA_DASH_DIR"
  python3 - "$DASH_SRC" "$DASH_DST" <<'PY'
import json, sys
src, dst = sys.argv[1], sys.argv[2]
with open(src) as f:
    d = json.load(f)
LOCAL_UID = "ef9a4xfon3toge"
for var in d.get("templating", {}).get("list", []):
    if var.get("name") == "datasource" and var.get("type") == "datasource":
        var["current"] = {"selected": True, "text": "Prometheus", "value": LOCAL_UID}
with open(dst, "w") as f:
    json.dump(d, f, indent=2)
    f.write("\n")
print(f"installed dashboard → {dst}")
PY
}

remove_dashboard() {
  if [[ -f "$DASH_DST" ]]; then
    rm -f "$DASH_DST"
    info "removed $DASH_DST"
  fi
}

reload_prometheus() {
  if pgrep -f '[p]rometheus' >/dev/null 2>&1; then
    kill -HUP "$(pgrep -f '[p]rometheus' | head -1)" 2>/dev/null || true
    info "sent SIGHUP to prometheus"
    return 0
  fi
  info "WARN: prometheus process not found — restart LaunchAgent if needed"
}

validate_prometheus_config() {
  if command -v promtool >/dev/null 2>&1; then
    promtool check config "$PROM_CFG" || die "promtool check config failed"
  fi
}

install_nginx_optional() {
  [[ "$WITH_NGINX" -eq 1 ]] || { info "nginx install skipped (pass --with-nginx)"; return 0; }
  require_file "$NGINX_SRC"
  cp "$NGINX_SRC" "$NGINX_DST"
  nginx -t || die "nginx -t failed"
  nginx -s reload
  info "nginx snippet installed + reloaded"
}

uninstall_nginx_optional() {
  if [[ -f "$NGINX_DST" ]]; then
    rm -f "$NGINX_DST"
    if nginx -t 2>/dev/null; then nginx -s reload; fi
    info "removed nginx snippet $NGINX_DST"
  fi
}

case "$ACTION" in
  install)
    inject_scrape
    inject_rules
    validate_prometheus_config
    install_dashboard
    reload_prometheus
    install_nginx_optional
    info "DONE. Start app: make run-senior  then: make metrics-smoke"
    info "Dashboard: http://localhost/grafana/d/fcs-fulfilment-obs/"
    info "Home Executive Overview was NOT modified."
    ;;
  uninstall)
    remove_scrape
    remove_rules
    remove_dashboard
    reload_prometheus
    uninstall_nginx_optional
    info "DONE uninstall"
    ;;
  *)
    die "usage: $0 install|uninstall [--with-nginx]"
    ;;
esac
