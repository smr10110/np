#!/usr/bin/env bash
# naivepay_password_recovery_tests.sh
# Pruebas end-to-end para bloqueo por intentos fallidos y recuperacion de contraseña.

set -eo pipefail

# ---------- Configuración (se puede sobreescribir con variables de entorno) ----------
BASE="${BASE:-http://localhost:8080}"
LOGIN_PATH="${LOGIN_PATH:-/auth/login}"
REQUEST_PATH="${REQUEST_PATH:-/auth/password/request}"
VERIFY_PATH="${VERIFY_PATH:-/auth/password/verify}"
RESET_PATH="${RESET_PATH:-/auth/password/reset}"
DEVICE_FP="${DEVICE_FP:-qa-script}"

LOGIN_EMAIL="${LOGIN_EMAIL:-}"
LOGIN_PASSWORD="${LOGIN_PASSWORD:-}"
LOGIN_BAD_PASSWORD="${LOGIN_BAD_PASSWORD:-}"
RECOVERY_CODE="${RECOVERY_CODE:-}"
NEW_PASSWORD_SHORT="${NEW_PASSWORD_SHORT:-}"
NEW_PASSWORD_STRONG="${NEW_PASSWORD_STRONG:-}"
# ------------------------------------------------------------------------------------

# ---------- Inputs interactivos -----------------------------------------------------
read -rp "Base URL [${BASE}]: " input && [[ -n "$input" ]] && BASE="$input"
read -rp "Fingerprint del dispositivo [${DEVICE_FP}]: " input && [[ -n "$input" ]] && DEVICE_FP="$input"

while [[ -z "$LOGIN_EMAIL" ]]; do
  read -rp "Correo del usuario de prueba: " LOGIN_EMAIL
done

while [[ -z "$LOGIN_PASSWORD" ]]; do
  read -rsp "Contraseña correcta actual: " LOGIN_PASSWORD
  echo
done

while [[ -z "$LOGIN_BAD_PASSWORD" ]]; do
  read -rsp "Contraseña incorrecta para simular fallos: " LOGIN_BAD_PASSWORD
  echo
done

read -rp "Código de recuperación (opcional, ENTER para omitir): " input && [[ -n "$input" ]] && RECOVERY_CODE="$input"
read -rsp "Contraseña corta para validar rechazo (opcional): " input && echo && [[ -n "$input" ]] && NEW_PASSWORD_SHORT="$input"
read -rsp "Nueva contraseña válida (opcional): " input && echo && [[ -n "$input" ]] && NEW_PASSWORD_STRONG="$input"
# ------------------------------------------------------------------------------------

# ---------- Utilidades visuales ----------------------------------------------------
if command -v tput >/dev/null 2>&1; then
  GREEN="$(tput setaf 2)"; RED="$(tput setaf 1)"; YELLOW="$(tput setaf 3)"; CYAN="$(tput setaf 6)"; BOLD="$(tput bold)"; RESET="$(tput sgr0)"
else
  GREEN=""; RED=""; YELLOW=""; CYAN=""; BOLD=""; RESET=""
fi

hr() { printf '%*s\n' "${COLUMNS:-80}" '' | tr ' ' '-'; }
heading() { echo -e "\n${BOLD}${CYAN}# $*${RESET}"; }
status_ok() { echo -e "${GREEN}✔ PASS${RESET} $*"; }
status_fail() { echo -e "${RED}✘ FAIL${RESET} $*"; }

HTTP_CODE=""; BODY=""; TMP_BODY=""

curl_json() {
  # Uso: curl_json METHOD PATH DATA_JSON ["HEADER=VALUE" ...]
  local method="$1"; shift
  local path="$1"; shift
  local data_json="$1"; shift

  local headers=(-H "Content-Type: application/json")
  while [[ $# -gt 0 ]]; do
    headers+=(-H "$1")
    shift
  done

  TMP_BODY=$(mktemp)
  HTTP_CODE=$(curl -sS -o "$TMP_BODY" -w "%{http_code}" -X "$method" "${BASE}${path}" "${headers[@]}" ${data_json:+-d "$data_json"})
  BODY=$(cat "$TMP_BODY")
  rm -f "$TMP_BODY"

  echo -e "${YELLOW}HTTP ${HTTP_CODE}${RESET}"
  if [[ -n "$BODY" ]]; then
    echo "$BODY" | sed 's/\n/\n/g' | head -c 1000
    local bytes
    bytes=$(printf %s "$BODY" | wc -c)
    if [[ "$bytes" -gt 1000 ]]; then
      echo
      echo "... (truncated)"
    fi
  else
    echo "(sin cuerpo)"
  fi
}

expect_code() {
  local regex="$1"
  if [[ "$HTTP_CODE" =~ $regex ]]; then
    status_ok "HTTP ${HTTP_CODE} coincide con /${regex}/"
  else
    status_fail "HTTP ${HTTP_CODE}, esperado /${regex}/"
  fi
}

expect_body_contains() {
  local needle="$1"
  if echo "$BODY" | grep -q "$needle"; then
    status_ok "Body contiene '${needle}'"
  else
    status_fail "Body no contiene '${needle}'"
  fi
}

login_payload() {
  printf '{"identifier":"%s","password":"%s"}' "$1" "$2"
}

# ---------- Escenarios --------------------------------------------------------------
heading "1) Cuatro intentos fallidos (401 BAD_CREDENTIALS)"
BAD_PAYLOAD=$(login_payload "$LOGIN_EMAIL" "$LOGIN_BAD_PASSWORD")
for i in {1..4}; do
  echo "Intento fallido $i/4"
  curl_json POST "$LOGIN_PATH" "$BAD_PAYLOAD" "X-Device-Fingerprint: ${DEVICE_FP}"
  expect_code '^401$'
  expect_body_contains 'BAD_CREDENTIALS'
done

heading "2) Quinto fallo: la cuenta debe bloquearse (403 ACCOUNT_BLOCKED)"
curl_json POST "$LOGIN_PATH" "$BAD_PAYLOAD" "X-Device-Fingerprint: ${DEVICE_FP}"
expect_code '^403$'
expect_body_contains 'ACCOUNT_BLOCKED'

heading "3) Intento con contraseña correcta mientras está bloqueada (403 ACCOUNT_BLOCKED)"
GOOD_PAYLOAD=$(login_payload "$LOGIN_EMAIL" "$LOGIN_PASSWORD")
curl_json POST "$LOGIN_PATH" "$GOOD_PAYLOAD" "X-Device-Fingerprint: ${DEVICE_FP}"
expect_code '^403$'
expect_body_contains 'ACCOUNT_BLOCKED'

heading "4) Solicitar código de recuperación (200)"
REQ_PAYLOAD=$(printf '{"email":"%s"}' "$LOGIN_EMAIL")
curl_json POST "$REQUEST_PATH" "$REQ_PAYLOAD"
expect_code '^200$'

if [[ -n "$RECOVERY_CODE" ]]; then
  heading "5) Verificar código ingresado (${RECOVERY_CODE})"
  VERIFY_PAYLOAD=$(printf '{"email":"%s","code":"%s"}' "$LOGIN_EMAIL" "$RECOVERY_CODE")
  curl_json POST "$VERIFY_PATH" "$VERIFY_PAYLOAD"
  expect_code '^200$'
else
  heading "5) Verificación de código"
  echo "[SKIP] Ingresa RECOVERY_CODE para ejecutar esta parte."
fi

if [[ -n "$RECOVERY_CODE" && -n "$NEW_PASSWORD_SHORT" ]]; then
  heading "6) Reset con contraseña corta (esperado 400 PASSWORD_TOO_SHORT)"
  RESET_SHORT=$(printf '{"email":"%s","code":"%s","newPassword":"%s"}' "$LOGIN_EMAIL" "$RECOVERY_CODE" "$NEW_PASSWORD_SHORT")
  curl_json POST "$RESET_PATH" "$RESET_SHORT"
  expect_code '^400$'
  expect_body_contains 'PASSWORD_TOO_SHORT'
else
  heading "6) Reset con contraseña corta"
  echo "[SKIP] Define RECOVERY_CODE y NEW_PASSWORD_SHORT para probar el rechazo."
fi

RESET_DONE="false"
if [[ -n "$RECOVERY_CODE" && -n "$NEW_PASSWORD_STRONG" ]]; then
  heading "7) Reset exitoso con nueva contraseña"
  RESET_STRONG=$(printf '{"email":"%s","code":"%s","newPassword":"%s"}' "$LOGIN_EMAIL" "$RECOVERY_CODE" "$NEW_PASSWORD_STRONG")
  curl_json POST "$RESET_PATH" "$RESET_STRONG"
  expect_code '^200$'
  RESET_DONE="true"
else
  heading "7) Reset exitoso"
  echo "[SKIP] Define RECOVERY_CODE y NEW_PASSWORD_STRONG para validar el flujo completo."
fi

if [[ "$RESET_DONE" == "true" ]]; then
  heading "8) Login con la nueva contraseña (200)"
  NEW_PAYLOAD=$(login_payload "$LOGIN_EMAIL" "$NEW_PASSWORD_STRONG")
  curl_json POST "$LOGIN_PATH" "$NEW_PAYLOAD" "X-Device-Fingerprint: ${DEVICE_FP}"
  expect_code '^200$'
  echo "Actualiza LOGIN_PASSWORD si deseas reutilizar la prueba."
else
  heading "8) Login tras reset"
  echo "[SKIP] Solo se ejecuta si cambiaste la contraseña."
fi

hr
echo "Flujo finalizado. BASE=${BASE} LOGIN=${LOGIN_PATH}" |
  sed 's/\r//g'
