@echo off
setlocal EnableExtensions EnableDelayedExpansion

echo ==================================================
echo   NaivePay - Bloqueo de cuenta y recuperacion QA
echo ==================================================
echo.

rem ---- Requisitos previos -------------------------------------------------
where curl >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Esta prueba requiere ^"curl^" en el PATH.
  echo         Instala curl o usa Windows 10+ donde viene preinstalado.
  exit /b 1
)

rem ---- Configuracion -------------------------------------------------------
if not defined BASE_URL (
  set /p "BASE_URL=Base URL del backend [http://localhost:8080]: "
  if "!BASE_URL!"=="" set "BASE_URL=http://localhost:8080"
)

if not defined DEVICE_FP (
  set /p "DEVICE_FP=Fingerprint del dispositivo [qa-script]: "
  if "!DEVICE_FP!"=="" set "DEVICE_FP=qa-script"
)

if not defined LOGIN_EMAIL (
  set /p "LOGIN_EMAIL=Correo del usuario de prueba: "
  if "!LOGIN_EMAIL!"=="" (
    echo [ERROR] Debes ingresar un correo para continuar.
    exit /b 1
  )
)

if not defined LOGIN_PASSWORD (
  set /p "LOGIN_PASSWORD=Contrasena correcta del usuario: "
  if "!LOGIN_PASSWORD!"=="" (
    echo [ERROR] Debes ingresar la contrasena correcta.
    exit /b 1
  )
)

if not defined LOGIN_BAD_PASSWORD (
  set /p "LOGIN_BAD_PASSWORD=Contrasena incorrecta para simular fallos: "
  if "!LOGIN_BAD_PASSWORD!"=="" (
    echo [ERROR] Debes ingresar una contrasena invalida para las pruebas.
    exit /b 1
  )
)

echo [INFO] Base URL..............: !BASE_URL!
echo [INFO] Fingerprint usado....: !DEVICE_FP!
echo [INFO] Usuario de prueba....: %LOGIN_EMAIL%
echo.
echo [NOTA] Si aun no existen, puedes ingresar ahora datos opcionales para ampliar las pruebas.
if not defined RECOVERY_CODE (
  set /p "RECOVERY_CODE=Codigo de recuperacion (ENTER para omitir): "
)
if not defined NEW_PASSWORD_SHORT (
  set /p "NEW_PASSWORD_SHORT=Contrasena corta para validar rechazo (ENTER para omitir): "
)
if not defined NEW_PASSWORD_STRONG (
  set /p "NEW_PASSWORD_STRONG=Contrasena valida nueva (ENTER para omitir): "
)
echo.

rem Preparar payloads frecuentes
set "BAD_LOGIN_PAYLOAD={""identifier"":""%LOGIN_EMAIL%"",""password"":""%LOGIN_BAD_PASSWORD%""}"
set "GOOD_LOGIN_PAYLOAD={""identifier"":""%LOGIN_EMAIL%"",""password"":""%LOGIN_PASSWORD%""}"

rem Directorio temporal para respuestas
set "NP_QA_TMP=%TEMP%\np_qa"
if not exist "%NP_QA_TMP%" md "%NP_QA_TMP%" >nul 2>&1

echo --------------------------------------------------
echo [PASO 1] Simular 4 intentos fallidos (esperado HTTP 401)
echo --------------------------------------------------
for /L %%I in (1,1,4) do (
  call :postJson "Login fallido %%I/4" "!BASE_URL!/auth/login" "!BAD_LOGIN_PAYLOAD!" 401
)

echo --------------------------------------------------
echo [PASO 2] Quinto intento fallido debe bloquear (esperado HTTP 403)
echo --------------------------------------------------
call :postJson "Login fallido 5/5" "!BASE_URL!/auth/login" "!BAD_LOGIN_PAYLOAD!" 403

echo --------------------------------------------------
echo [PASO 3] Intento con password correcta mientras esta bloqueado (esperado HTTP 403)
echo --------------------------------------------------
call :postJson "Login con password correcta estando bloqueado" "!BASE_URL!/auth/login" "!GOOD_LOGIN_PAYLOAD!" 403

echo --------------------------------------------------
echo [PASO 4] Solicitar codigo de recuperacion (esperado HTTP 200)
echo --------------------------------------------------
set "REQUEST_PAYLOAD={""email"":""%LOGIN_EMAIL%""}"
call :postJson "Solicitud de codigo" "!BASE_URL!/auth/password/request" "!REQUEST_PAYLOAD!" 200

echo --------------------------------------------------
echo [PASO 5] Verificar codigo (requiere RECOVERY_CODE valido)
echo --------------------------------------------------
if defined RECOVERY_CODE (
  set "VERIFY_PAYLOAD={""email"":""%LOGIN_EMAIL%"",""code"":""%RECOVERY_CODE%""}"
  call :postJson "Verificacion de codigo" "!BASE_URL!/auth/password/verify" "!VERIFY_PAYLOAD!" 200
) else (
  echo [SKIP] Define RECOVERY_CODE para ejecutar la verificacion real del codigo recibido.
)

echo --------------------------------------------------
echo [PASO 6] Reset con contrasena corta (esperado HTTP 400 PASSWORD_TOO_SHORT)
echo --------------------------------------------------
if defined RECOVERY_CODE if defined NEW_PASSWORD_SHORT (
  set "RESET_SHORT_PAYLOAD={""email"":""%LOGIN_EMAIL%"",""code"":""%RECOVERY_CODE%"",""newPassword"":""%NEW_PASSWORD_SHORT%""}"
  call :postJson "Reset con contrasena corta" "!BASE_URL!/auth/password/reset" "!RESET_SHORT_PAYLOAD!" 400
) else (
  echo [SKIP] Define RECOVERY_CODE y NEW_PASSWORD_SHORT para validar el rechazo por longitud.
)

echo --------------------------------------------------
echo [PASO 7] Reset exitoso con nueva contrasena (esperado HTTP 200)
echo --------------------------------------------------
if defined RECOVERY_CODE if defined NEW_PASSWORD_STRONG (
  set "RESET_STRONG_PAYLOAD={""email"":""%LOGIN_EMAIL%"",""code"":""%RECOVERY_CODE%"",""newPassword"":""%NEW_PASSWORD_STRONG%""}"
  call :postJson "Reset con contrasena valida" "!BASE_URL!/auth/password/reset" "!RESET_STRONG_PAYLOAD!" 200
  echo [INFO] Actualiza LOGIN_PASSWORD con la nueva contrasena antes de volver a ejecutar la bateria.
) else (
  echo [SKIP] Define RECOVERY_CODE y NEW_PASSWORD_STRONG para probar el reseteo exitoso.
)

echo --------------------------------------------------
echo [PASO 8] Login posterior al reseteo (esperado HTTP 200)
echo --------------------------------------------------
if defined NEW_PASSWORD_STRONG if defined RECOVERY_CODE (
  set "GOOD_AFTER_RESET={""identifier"":""%LOGIN_EMAIL%"",""password"":""%NEW_PASSWORD_STRONG%""}"
  call :postJson "Login con password nueva" "!BASE_URL!/auth/login" "!GOOD_AFTER_RESET!" 200
) else (
  echo [SKIP] Solo se ejecuta si definiste NEW_PASSWORD_STRONG y RECOVERY_CODE.
)

echo.
echo [FIN] Script completado. Revisa los codigos HTTP y cuerpos anteriores.
exit /b 0


:postJson
set "LABEL=%~1"
set "URL=%~2"
set "BODY=%~3"
set "EXPECT=%~4"

set "TMP_FILE=%NP_QA_TMP%\resp_!RANDOM!.json"

for /f "usebackq delims=" %%S in (`curl -s -o "!TMP_FILE!" -w "%%{http_code}" -X POST "!URL!" -H "Content-Type: application/json" -H "X-Device-Fingerprint: !DEVICE_FP!" -d "!BODY!"`) do set "STATUS=%%S"

if defined EXPECT (
  if "!STATUS!"=="!EXPECT!" (
    echo [OK]   !LABEL! -> HTTP !STATUS!
  ) else (
    echo [WARN] !LABEL! -> HTTP !STATUS! (esperado !EXPECT!)
  )
) else (
  echo [INFO] !LABEL! -> HTTP !STATUS!
)

if exist "!TMP_FILE!" (
  type "!TMP_FILE!"
  del "!TMP_FILE!" >nul 2>&1
) else (
  echo (Sin cuerpo de respuesta)
)
echo.
exit /b 0
