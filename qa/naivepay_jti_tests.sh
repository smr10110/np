#!/usr/bin/env bash
# naivepay_jti_tests.sh
# End-to-end curl tests for JWT JTI validation + session close on expiration.

set -eo pipefail   # robusto (sin -u para evitar "unbound variable")

# -------- Config (puedes sobreescribir con env) --------
BASE="${BASE:-http://localhost:8080}"
LOGIN="${LOGIN:-/auth/login}"
LOGOUT="${LOGOUT:-/auth/logout}"
PRIV="${PRIV:-/api/funds/accounts/balance}"   # endpoint privado (GET)
USER="${USER:-peppe1232@yopmail.com}"
PASS="${PASS:-melboHD123}"
DEVICE_ID="${DEVICE_ID:-2222222}"
TTL_WAIT="${TTL_WAIT:-80}"                     # segundos para probar expiración
# -------------------------------------------------------

# Vars inicializadas (evita "unbound")
HTTP_CODE=""; BODY=""; RESP=""; DATA=""; TOKEN=""; TOKEN_EXP=""
TA=""; TB=""; BAD=""; SHORT=""

# Colores opcionales
if command -v tput >/dev/null 2>&1; then
  GREEN="$(tput setaf 2)"; RED="$(tput setaf 1)"; YELLOW="$(tput setaf 3)"; CYAN="$(tput setaf 6)"; BOLD="$(tput bold)"; RESET="$(tput sgr0)"
else
  GREEN=""; RED=""; YELLOW=""; CYAN=""; BOLD=""; RESET=""
fi

hr() { printf '%*s\n' "${COLUMNS:-80}" '' | tr ' ' '-'; }
heading() { echo -e "\n${BOLD}${CYAN}# $*${RESET}"; }
info() { echo -e "${BOLD}$*${RESET}"; }

curl_json() {
  # Uso: curl_json METHOD URL [DATA_JSON] [TOKEN]
  local METHOD="${1:-GET}"; shift || true
  local URL="${1:-/}";      shift || true
  local DATA_LOCAL="${1:-}"; shift || true
  local TOKEN_LOCAL="${1:-}"; shift || true

  local headers=(-H "Content-Type: application/json")
  if [[ -n "${TOKEN_LOCAL}" ]]; then
    headers+=(-H "Authorization: Bearer ${TOKEN_LOCAL}")
  fi

  HTTP_CODE=$(curl -sS -o /tmp/body.$$ -w "%{http_code}" -X "${METHOD}" "${BASE}${URL}" \
    "${headers[@]}" ${DATA_LOCAL:+-d "$DATA_LOCAL"})
  BODY="$(cat /tmp/body.$$)"
  rm -f /tmp/body.$$

  echo -e "${YELLOW}HTTP ${HTTP_CODE}${RESET}"
  if [[ -n "$BODY" ]]; then
    echo "$BODY" | sed 's/\\n/\n/g' | head -c 800
    # ---- FIX: evita cortarse con set -e ----
    BYTES=$(printf %s "$BODY" | wc -c)
    if [ "$BYTES" -gt 800 ]; then
      echo
      echo "... (truncated)"
    fi
    # ---------------------------------------
  else
    echo "(no body)"
  fi
}

looks_like_jwt() { [[ "$1" =~ ^[^.]+\.[^.]+\.[^.]+$ ]]; }

expect_code() {
  local expected_regex="$1"
  if [[ "$HTTP_CODE" =~ $expected_regex ]]; then
    echo -e "${GREEN}✔ PASS${RESET} (${HTTP_CODE} ~ /${expected_regex}/)"
  else
    echo -e "${RED}✘ FAIL${RESET} expected /${expected_regex}/ but got ${HTTP_CODE}"
  fi
}

expect_body_contains() {
  local needle="$1"
  if echo "$BODY" | grep -q "$needle"; then
    echo -e "${GREEN}✔ PASS${RESET} body contains: ${needle}"
  else
    echo -e "${RED}✘ FAIL${RESET} body DOES NOT contain: ${needle}"
  fi
}

# -------- Tests --------
heading "1) Login y acceso privado"
DATA=$(printf '{"identifier":"%s","password":"%s","deviceLinkId":%s}' "$USER" "$PASS" "$DEVICE_ID")
RESP=$(curl -s -i -X POST "${BASE}${LOGIN}" -H "Content-Type: application/json" -d "$DATA")
echo "$RESP" | head -n 1
TOKEN="$(echo "$RESP" | tr -d '\r' | sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
echo "TOKEN: ${TOKEN:0:30}..."

if ! looks_like_jwt "$TOKEN"; then
  echo -e "${RED}No se pudo extraer un accessToken válido del login. Aborta.${RESET}"
  exit 1
fi

echo; info "→ PRIV con token (debe 200)"
curl_json GET "$PRIV" "" "${TOKEN:-}"
expect_code '^200$'

heading "2) Logout y re-uso del MISMO token (revocación por JTI)"
info "→ Logout (200 o 204)"
curl_json POST "$LOGOUT" "" "${TOKEN:-}"
expect_code '^(200|204)$'

info "→ PRIV con token ya cerrado (debe 401 TOKEN_CLOSED)"
curl_json GET "$PRIV" "" "${TOKEN:-}"
expect_code '^401$'
expect_body_contains 'TOKEN_CLOSED'

heading "3) Token expirado (cerrar y 401 TOKEN_EXPIRED)"
info "→ Login (token fresco)"
RESP=$(curl -s -i -X POST "${BASE}${LOGIN}" -H "Content-Type: application/json" -d "$DATA")
TOKEN_EXP="$(echo "$RESP" | tr -d '\r' | sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
echo "TOKEN_EXP: ${TOKEN_EXP:0:30}..."
if ! looks_like_jwt "$TOKEN_EXP"; then
  echo -e "${RED}No se pudo extraer accessToken para expiración. Aborta.${RESET}"
  exit 1
fi

info "→ Esperando ${TTL_WAIT}s para superar TTL..."
sleep "$TTL_WAIT"

info "→ PRIV con token expirado (debe 401 TOKEN_EXPIRED)"
curl_json GET "$PRIV" "" "${TOKEN_EXP:-}"
expect_code '^401$'
expect_body_contains 'TOKEN_EXPIRED'

heading "4) Segunda sesión independiente (multi-pestaña)"
info "→ Login A"
RESP=$(curl -s -i -X POST "${BASE}${LOGIN}" -H "Content-Type: application/json" -d "$DATA")
TA="$(echo "$RESP" | tr -d '\r' | sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
echo "TA: ${TA:0:30}..."

info "→ Login B"
RESP=$(curl -s -i -X POST "${BASE}${LOGIN}" -H "Content-Type: application/json" -d "$DATA")
TB="$(echo "$RESP" | tr -d '\r' | sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
echo "TB: ${TB:0:30}..."

info "→ Logout A"
curl_json POST "$LOGOUT" "" "${TA:-}"
expect_code '^(200|204)$'

info "→ PRIV con A (debe 401 TOKEN_CLOSED)"
curl_json GET "$PRIV" "" "${TA:-}"
expect_code '^401$'
expect_body_contains 'TOKEN_CLOSED'

info "→ PRIV con B (debe 200)"
curl_json GET "$PRIV" "" "${TB:-}"
expect_code '^200$'

heading "5) Token mal formado (firma rota / truncado) => 401 TOKEN_INVALID"
BAD="${TB%?}Z"
curl_json GET "$PRIV" "" "${BAD:-}"
expect_code '^401$'
expect_body_contains 'TOKEN_INVALID'

SHORT="abc.def"
curl_json GET "$PRIV" "" "${SHORT:-}"
expect_code '^401$'
expect_body_contains 'TOKEN_INVALID'

heading "6) Sin Authorization en privado => 401 EntryPoint"
curl_json GET "$PRIV" ""
expect_code '^401$'

heading "7) Prefijo 'bearer' en minúsculas => 200 (si el filtro lo permite)"
HTTP_CODE=$(curl -sS -o /tmp/body.$$ -w "%{http_code}" -H "authorization: bearer ${TB}" "${BASE}${PRIV}")
BODY=$(cat /tmp/body.$$); rm -f /tmp/body.$$
echo -e "${YELLOW}HTTP ${HTTP_CODE}${RESET}"
echo "$BODY" | head -c 600
expect_code '^200$'

heading "8) Logout sin header (según tu config: 204 o 401)"
curl_json POST "$LOGOUT" ""
expect_code '^(204|401)$'

hr
echo -e "${GREEN}Tests finalizados.${RESET}"
echo "BASE=${BASE} PRIV=${PRIV} LOGIN=${LOGIN} LOGOUT=${LOGOUT}"
