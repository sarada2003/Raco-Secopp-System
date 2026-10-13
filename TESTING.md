# Pruebas — RACO–SECOPP

Este documento resume la fase de pruebas del sistema: qué se probó, cómo correrlo y los
resultados obtenidos. Cubre pruebas unitarias, de seguridad y de rendimiento en backend, y
pruebas unitarias en frontend.

**Fuera de alcance (deliberado):** pruebas cronometradas/comparativas con usuarios reales de
la empresa — esas se hacen manualmente y no están automatizadas aquí. Como línea base previa a
esas pruebas, §9 registra una comparación cronometrada SECOP II vs. RACO ejecutada en el
navegador (no con usuarios reales).

Última corrida verificada: 2026-09-01 — **79/79** tests backend y **36/36** tests frontend en
verde, más los dos benchmarks de rendimiento con resultados reales (ver [§8](#8-estado-de-la-suite-de-pruebas)).

---

## 1. Cómo correr todo

### Backend

```bash
cd backend/sistemawebraco
./gradlew test
```

No necesita Postgres, ni `JWT_SECRET`/`OPENAI_API_KEY`/`SECOP_APP_TOKEN` en el entorno: usa
un perfil de pruebas propio (ver [§5](#5-perfil-de-pruebas-h2-por-qué-es-seguro)) con una base
H2 en memoria y secretos dummy. Corre offline, sin depender de las APIs externas reales.

Los benchmarks de rendimiento (§4) **no** corren con este comando — están excluidos a propósito.

### Frontend

```bash
cd frontend/racoSystemFrontend
ng test --watch=false --code-coverage
```

**Nota de entorno:** este equipo no tiene Google Chrome instalado, solo Microsoft Edge
(Chromium). El launcher `ChromeHeadless` de Karma funciona igual apuntándolo al binario de
Edge:

```bash
# Windows (Git Bash / bash)
export CHROME_BIN="/c/Program Files (x86)/Microsoft/Edge/Application/msedge.exe"
ng test --watch=false --browsers=ChromeHeadless --code-coverage
```

Si tienes Chrome real instalado, `ng test --watch=false` (sin `CHROME_BIN`) funciona tal cual.

### Benchmarks de rendimiento (opt-in, manual)

Requieren el backend arrancado con credenciales **reales** (no las dummy de test):

```bash
# Terminal 1
cd backend/sistemawebraco
./gradlew bootRun   # con tus variables de entorno reales (Postgres, OPENAI_API_KEY, SECOP_APP_TOKEN...)

# Terminal 2
cd backend/sistemawebraco
./gradlew test -PincludePerf --tests "SecopSearchPerformanceTest"
./gradlew test -PincludePerf --tests "ChatbotPerformanceTest"
```

Cada uno escribe una tabla de resultados en `backend/sistemawebraco/build/perf-results/`.

---

## 2. Pruebas unitarias (backend)

79 tests en 13 clases (81 incluyendo los 2 benchmarks opt-in), con JUnit 5 + Mockito +
AssertJ (ya incluidos en `spring-boot-starter-test`). Ningún test pega a una API externa real:

| Clase | Qué cubre |
|---|---|
| `SoqlQueryBuilderTest` | Armado de cláusulas SoQL (`equals`, `min`/`max`, `dateBetween`, `fullText`), y que el escape de comillas simples evita inyección SoQL (`O'Brien` → `O''Brien`). |
| `SecopServiceTest` | `buscarContratos`/`consultarSecop` arman la URL correcta ($q, $where, $order, paginación) contra un `WebClient` con `exchangeFunction` reemplazada por un stub — **sin red real**. Incluye el mapeo de cualquier error a 504. |
| `JwtUtilTest` | Generación, validación de token válido/expirado/con firma ajena/malformado/vacío, y extracción del subject. |
| `NotificacionServiceTest` | Ownership en `marcarLeida`/`eliminarSuscripcion` (404 si el id es de otro usuario), deduplicación y normalización de frecuencia en `crearSuscripcion`. |
| `InformeServiceTest` | Ownership en `obtener`, que un fallo al guardar el historial nunca se propaga, y el fallback ante JSON corrupto. |
| `AuthServiceTest` | Login (éxito, email inexistente, password incorrecta), registro (éxito, email duplicado), `obtenerUsuarioDesdeToken`, login con Google. |
| `SecopAiFunctionConfigTest` | La herramienta `buscarContratosSecop` que el chatbot invoca vía function-calling: formato de respuesta, mensaje de "sin resultados", manejo de timeout de SECOP y de errores inesperados sin propagar la excepción al modelo, que registra la búsqueda en el historial del usuario recibido por `ToolContext` (y no falla si no hay contexto), y que mide su tiempo en la métrica `chatbot.herramienta.secop`. |

## 3. Pruebas de seguridad (backend)

`@WebMvcTest` + `SecurityConfig` real (no mockeada) por cada controller — se ejercita la
cadena de filtros real (`JwtAuthenticationFilter`, `CustomUserDetailsService`) con un JWT
generado de verdad, sin levantar base de datos.

| Clase | Qué verifica |
|---|---|
| `SecopControllerSecurityTest` | 401 sin token / con token inválido; 200 con token válido. |
| `NotificacionControllerSecurityTest` | 401 sin token; que el id de usuario usado en **todas** las operaciones (incluida la verificación de dueño) sale siempre del JWT, nunca de un parámetro que el cliente controle — no hay forma de pedir "marca como leída la notificación de otro" porque no existe un campo de usuario en la petición. |
| `InformeControllerSecurityTest` | 404 al pedir el informe de otro usuario; 200 y uso correcto del id del token para el propio. |
| `ChatbotControllerSecurityTest` | 401 sin token; 400 con mensaje vacío o >2000 caracteres; 200 con mensaje válido. Para `/chat/stream` (SSE): 401 sin token, 400 con mensaje vacío, que los fragmentos llegan como eventos y cierran con `fin` (esto ejercita también el dispatch ASYNC, permitido en `SecurityConfig`), y que un timeout llega como evento `error` con el mensaje para el usuario. |
| `AuthControllerSecurityTest` | `/login` y `/register` son públicos; `/me` exige JWT. |

**Decisión de diseño: 404 en vez de 403 para violaciones de ownership.** El sistema no
devuelve `403 Forbidden` en ningún endpoint (verificado: cero ocurrencias de
`HttpStatus.FORBIDDEN` en todo `src/main/java`) — es deliberado, no una omisión. Cuando un
usuario autenticado pide un recurso que no es suyo (p. ej. `GET /api/informes/{id}` de otro
usuario), el sistema responde `404 Not Found` en vez de `403`, siguiendo la recomendación de
OWASP contra IDOR: un `403` le confirma al atacante que el recurso *existe* pero le está
vedado, mientras que un `404` no revela esa información. Por eso la cobertura de seguridad de
este proyecto se define como **401 (sin token / token inválido) + 404 (ownership, donde
aplica)** — no 401/403/404 — y no se agregó un `403` artificial en ningún punto solo para
completar esa tríada.

**IDOR:** se revisaron todos los endpoints que exponen recursos por id
(`notificaciones/{id}`, `notificaciones/suscripciones/{id}`, `informes/{id}`). Los tres ya
filtran por dueño en la propia consulta (`findByIdAndUsuarioId`) — no se encontró ningún
endpoint sin ese chequeo. Los tests de arriba dejan esa garantía como regresión.

**Defectos encontrados y corregidos al escribir estos tests:**

1. **`ChatbotController.chat` no validaba el body** — el DTO `ChatRequest` tiene
   `@NotBlank`/`@Size(max=2000)`, pero al parámetro le faltaba `@Valid`, así que esas
   anotaciones nunca se ejecutaban: un mensaje vacío o de cualquier longitud pasaba sin
   rechazo. Corregido agregando `@Valid` (`ChatbotController.java`).
2. **`JwtUtil.validateToken` no capturaba `IllegalArgumentException`** — jjwt lanza esa
   excepción (no `JwtException`) para un token nulo o vacío. Sin el catch, un header
   `Authorization: Bearer ` (sin nada después) tumbaba la petición con un 500 en vez del 401
   esperado. Corregido ampliando el catch (`JwtUtil.java`).

## 4. Pruebas de rendimiento (backend)

Sin herramienta externa (JMeter, etc.), dos mecanismos complementarios:

**a) Métricas siempre activas (Actuator + Micrometer, ya en el classpath):**
`management.endpoints.web.exposure.include=health,metrics` expone
`GET /actuator/metrics/http.server.requests`, con tiempos agregados (count, sum, max) por
endpoint automáticamente, sin código adicional. Útil para observar el sistema corriendo en
cualquier momento.

Para el chatbot hay además métricas propias que desglosan el tiempo por fase:

| Métrica | Qué mide |
|---|---|
| `chatbot.respuesta` (tags `modo`=`completa`/`stream`, `resultado`) | Tiempo total de la respuesta. |
| `chatbot.primer.fragmento` | En streaming: tiempo hasta el primer texto que ve el usuario. |
| `chatbot.herramienta.secop` (tag `resultado`) | Tiempo de la herramienta `buscarContratosSecop` (consulta a SECOP II). |

Ej.: `GET /actuator/metrics/chatbot.respuesta?tag=modo:stream` (requiere JWT).

**b) Benchmarks manuales (`SecopSearchPerformanceTest`, `ChatbotPerformanceTest`):**
tag JUnit `perf`, excluido del suite por defecto en `build.gradle` (pegan a datos.gov.co y a
OpenAI reales — lento, facturable y no determinista para un suite que debe poder correr
offline). Cada uno hace login real contra el backend corriendo, ejecuta N peticiones por
escenario y escribe promedio/mín/P95/máx en `build/perf-results/*.md`, listo para pegar en el
reporte de tesis. Ver [§1](#benchmarks-de-rendimiento-opt-in-manual) para correrlos.

**Conteo de tokens del chatbot:** `ChatbotService` ahora loguea, en cada interacción, los
tokens de prompt/completion/total (`Usage` de Spring AI) — **solo los conteos numéricos**,
nunca el mensaje del usuario ni la respuesta del modelo (los usuarios consultan contratos
reales por el chatbot; ese log no debía volverse un registro de esas conversaciones). Línea de
log: `Chatbot tokens - prompt=N, completion=N, total=N`. El reporte de `ChatbotPerformanceTest`
incluye una columna **"Tokens (prompt/completion/total)"** marcada explícitamente como
`Pendiente (ver log del backend)` en vez de dejarla vacía o con un placeholder ambiguo — se
completa a mano revisando la consola del backend durante la corrida, porque el endpoint HTTP no
expone ese dato.

**Nota sobre `/actuator/health` en los benchmarks:** esa ruta no está en la lista `permitAll`
de `SecurityConfig` (a propósito — no forma parte del alcance de rutas públicas), así que sin
token responde `401`, no `200`. `PerfSupport.asumirBackendDisponible()` trata cualquier
respuesta HTTP (incluido un 401) como "el backend está arriba"; solo aborta el benchmark si la
conexión falla de verdad (backend apagado). Verificado empíricamente en ambos sentidos: con el
backend arriba (401 en `/actuator/health`) el benchmark corre normalmente; con el backend caído
(conexión rechazada) el benchmark se aborta como `SKIPPED` (JUnit `Assumptions`), sin generar
falsos resultados ni fallos confusos.

## 5. Perfil de pruebas H2: por qué es seguro

`src/test/resources/application.properties` define la base H2 en memoria y los secretos dummy
usados por **todos** los tests backend. Gradle (y Maven) agregan `src/test/resources` al
classpath **solo de la tarea `test`** — nunca al de `bootJar` ni `bootRun`, que usan
exclusivamente `src/main/resources/application.properties` (con Postgres real y las variables
de entorno reales). No hay overlap de tareas entre ambos: no existe ningún escenario en el que
`bootRun` o el jar de producción terminen leyendo el `application.properties` de test.

## 6. Pruebas unitarias (frontend)

Angular 16 con Jasmine/Karma (ya preconfigurado por Angular CLI, sin `karma.conf.js` propio).
36 specs en total.

**Nuevos/reescritos con aserciones reales** (antes eran el stub por defecto de Angular CLI,
`expect(x).toBeTruthy()` sin cubrir comportamiento):

| Archivo | Qué cubre |
|---|---|
| `secop.service.spec.ts` | `obtenerContratos` (GET + query params, defaults) y `buscarContratos` (POST + body). |
| `notificacion.service.spec.ts` | Los 7 métodos del servicio: verbo HTTP y URL correctos para cada uno. |
| `auth.guard.spec.ts` | Permite el acceso si `isAuthenticated()` es true; bloquea y redirige a `/login` si es false. **Estaba roto** (importaba un guard funcional `authGuard` que no existe — el código real es la clase `AuthGuard`; no compilaba). |
| `auth.interceptor.spec.ts` | Agrega `Authorization: Bearer <token>` cuando hay token en `localStorage`; no lo agrega cuando no hay. |
| `auth.service.spec.ts` | `isAuthenticated()`, `loginManual()` (POST + body), `getUsuarioActual()` (header Authorization). |

**Specs preexistentes arregladas** (fallaban por proveedores de DI faltantes en el módulo de
test — típico de los stubs generados por Angular CLI que nunca se corrieron después de que el
componente creciera): `app.component.spec.ts` (aserción obsoleta sobre un template que ya no
existe), `navbar.component.spec.ts`, `main-layout.component.spec.ts`,
`login.component.spec.ts`, `register-form.component.spec.ts`,
`dashboard-usuario.component.spec.ts`. Siguen siendo smoke tests ("should create"); no se
amplió su cobertura de comportamiento porque no estaban en el alcance pedido.

---

## 7. Mapeo al informe de tesis

- **Reporte de pruebas unitarias** → §2 (backend) + §6 (frontend). Comandos: `./gradlew test`
  (con `--tests` para una clase puntual) y `ng test --watch=false --code-coverage` (el reporte
  de cobertura HTML queda en `frontend/racoSystemFrontend/coverage/`).
- **Reporte de pruebas de seguridad** → §3: tabla de endpoints verificados, hallazgo de IDOR
  (ninguno pendiente) y los dos defectos encontrados/corregidos con su explicación.
- **Reporte de pruebas de rendimiento** → §4: metodología (Actuator + benchmarks manuales),
  cómo generarlo, y la nota sobre tokens del chatbot. Los archivos en `build/perf-results/`
  generados al correr los benchmarks son el anexo con números reales.
- **Comparación de tiempos SECOP II vs. RACO** → §9: tres búsquedas cronometradas en el
  navegador (SECOP II directo vs. filtro y chat de RACO) y hallazgos de calidad del chatbot.

---

## 8. Estado de la suite de pruebas

Corrida verificada el **2026-09-01**. Base para los tres reportes de tesis (unitarias,
seguridad, rendimiento) — resumen ejecutivo, sin repetir el detalle de §2–§6.

### Unitarias + seguridad (backend) y unitarias (frontend)

| Suite | Resultado | Comando |
|---|---|---|
| Backend (unitarias + seguridad, 13 clases) | **79/79** ✅, 0 fallas, 0 errores | `./gradlew test` |
| Frontend (Jasmine/Karma, 16 specs) | **36/36** ✅ | `ng test --watch=false --browsers=ChromeHeadless` |

### Decisión de seguridad: ownership → 404, no 403

El sistema no implementa `403 Forbidden` en ningún endpoint — es una decisión de diseño
deliberada, no una omisión. Las violaciones de ownership (pedir un recurso que pertenece a
otro usuario) responden **404 Not Found**, siguiendo la recomendación de OWASP contra IDOR: un
403 le confirmaría a un atacante que el recurso *existe* pero le está vedado; un 404 no revela
esa información. Por eso la cobertura de seguridad del proyecto se define y se reporta como
**401 (sin token / token inválido) + 404 (ownership, donde aplica)** — nunca 401/403/404 — y no
se forzó un 403 artificial en ningún punto solo para completar esa tríada. Detalle completo en
[§3](#3-pruebas-de-seguridad-backend).

### Rendimiento (backend) — resultados reales de esta corrida

Ambos benchmarks corrieron contra el backend real (`./gradlew bootRun`, Postgres +
credenciales reales de OpenAI y SECOP), de forma **secuencial** (una petición a la vez, sin
herramienta de carga tipo JMeter ni concurrencia simulada), tal como está definido su alcance.

**`SecopSearchPerformanceTest`** — POST `/api/secop/buscar` contra datos.gov.co real, 10
muestras por escenario:

| Escenario | Muestras | Promedio (ms) | Mín (ms) | P95 (ms) | Máx (ms) |
|---|---|---|---|---|---|
| Sin filtros (listado general) | 10 | 262 | 174 | 899 | 899 |
| Por palabra clave | 10 | 758 | 536 | 1885 | 1885 |
| Por rango de valor | 10 | 205 | 167 | 252 | 252 |
| Combinada (palabra clave + entidad + valor) | 10 | 534 | 480 | 691 | 691 |

**`ChatbotPerformanceTest`** — POST `/api/chatbot/chat` contra gpt-4o-mini real (con y sin
tool-call a SECOP II), 3 muestras por mensaje. Esta tabla es la línea base **antes** del
streaming; el benchmark ahora mide también `/api/chatbot/chat/stream` (primer fragmento y
respuesta completa) — al volver a correrlo, reemplazar esta tabla por la nueva:

| Escenario | Muestras | Promedio (ms) | Mín (ms) | P95 (ms) | Máx (ms) | Tokens (prompt/completion/total) |
|---|---|---|---|---|---|---|
| Pregunta general (sin tool-call) | 3 | 3672 | 2985 | 4578 | 4578 | Pendiente (ver log del backend) |
| Búsqueda simple (con tool-call a SECOP) | 3 | 8507 | 8176 | 8965 | 8965 | Pendiente (ver log del backend) |
| Búsqueda con varios filtros (con tool-call a SECOP) | 3 | 5088 | 4443 | 6040 | 6040 | Pendiente (ver log del backend) |

La columna de tokens queda marcada explícitamente como **pendiente** a propósito — el endpoint
HTTP no expone ese dato; se completa a mano revisando el log del backend
(`Chatbot tokens - prompt=N, completion=N, total=N`) durante la corrida, antes de citar esta
tabla en el reporte de tesis.

Archivos fuente de estas tablas: `backend/sistemawebraco/build/perf-results/secop-search-performance.md`
y `.../chatbot-performance.md` (regenerados en cada corrida de los benchmarks).

---

## 9. Comparación cronometrada: SECOP II directo vs. RACO

Corrida el **2026-10-12** (≈23:44–23:58, hora Colombia) en Chrome, contra
community.secop.gov.co (buscador público de procesos) y RACO local (`localhost:4200` +
backend en `:8080`, usuario ya autenticado). Tres búsquedas distintas; cada una se hizo en
SECOP II, en el filtro de RACO (*Contratos SECOP*) y en el chat de RACO (*Chatbot IA*).

**Cómo se midió:**

- **Inicio:** página de búsqueda ya cargada y vacía. **Fin:** resultados visibles en pantalla.
  En RACO el fin se tomó con un observador en la página (`performance.now()`), que además da
  el tiempo de respuesta del sistema (clic en *Buscar* / *Enter* → resultados). En SECOP II el
  fin se tomó al verificar los resultados, así que esos tiempos tienen hasta ~1 s de holgura.
- Las acciones (clics, escritura) se ejecutaron de forma automatizada en el navegador, no las
  hizo una persona. La automatización escribe y hace clic más rápido que un usuario, así que **los tiempos absolutos
  subestiman los de un usuario real**; lo comparable es la diferencia entre las tres vías y
  el número de pasos que exige cada una.
- El reCAPTCHA que SECOP II exige antes de buscar **no se cuenta** (lo resolvió una persona
  antes de cronometrar). Un usuario real sí lo paga.
- Una sola muestra por fase.

### Resultados

| Prueba | Búsqueda | SECOP II directo | RACO — filtro | RACO — chat |
|---|---|---|---|---|
| 1 | Contratos de **tecnología** en **Antioquia** | **5,4 s** · 3 pasos · 5 resultados en la 1.ª página | **6,1 s** (sistema 3,3 s) · 4 pasos · 20 resultados | **9,1 s** (escribir 2,1 s + respuesta 7,0 s; primer texto a 3,4 s) · 5 contratos |
| 2 | Contratos de **obra** del **IDU** | **≈44 s** · 9 pasos · 2 resultados | **3,9 s** (sistema 0,55 s) · 5 pasos · 20 resultados | **8,7 s** (escribir 2,1 s + respuesta 6,6 s; primer texto a 2,1 s) · 5 contratos |
| 3 | **Suministro** en **Cundinamarca** por **más de $500 M** | **> 60 s, incompleta** · 36+ pasos · 10 encontrados en 170 revisados, quedaban más | **4,9 s** (sistema 0,85 s) · 6 pasos · 20 resultados, todos ≥ $500 M | **10,9 s** (escribir 2,1 s + respuesta 8,7 s; primer texto a 2,5 s) · 5 filas, 2 contratos distintos |

**Por qué SECOP II se dispara en las pruebas 2 y 3:**

- **Prueba 2 — entidad:** el campo *Datos de la entidad* no acepta texto libre; se llena con
  una ventana de búsqueda que para "Instituto de Desarrollo Urbano" devuelve **5 entidades
  homónimas**. Escoger la correcta exige reconocer el NIT (899999081). En RACO basta escribir
  el nombre en *Entidad*.
- **Prueba 3 — valor:** SECOP II **no tiene filtro por cuantía**. La búsqueda por descripción
  y región tarda 12,5 s, pero luego hay que revisar la columna *Cuantía* fila por fila,
  cargando 5 resultados más con cada *Ver más*. Tras 31 clics en *Ver más* (60 s, a velocidad
  de máquina) iban 170 procesos revisados y 10 de más de $500 M, y aún quedaban más. En RACO
  es un campo (*Valor mínimo*) y los 20 resultados ya vienen filtrados.
- En la prueba 1 (palabra clave + región, los dos filtros que SECOP II sí tiene en texto
  libre) SECOP II y el filtro de RACO quedan prácticamente empatados.

**Los conteos de resultados no son comparables entre columnas:** el buscador de SECOP II
muestra por defecto solo procesos publicados en los últimos 3 meses, y RACO consulta el
dataset de datos abiertos (`datos.gov.co`) con sus propios criterios y una página de 20.

### Hallazgos de calidad del chatbot (a corregir)

El chat respondió rápido en las tres pruebas (primer texto en 2–3,5 s), pero el contenido de
las respuestas tuvo problemas:

1. **Valores en $0:** en las pruebas 1 y 2 los 10 contratos listados tienen `Valor: $0 COP` y
   `Proveedor: No Definido`. Son procesos en borrador/aprobación sin cuantía, poco útiles como
   respuesta a "busca contratos de…".
2. **Relevancia (prueba 2):** se pidieron contratos *de obra* del IDU y los 5 devueltos son de
   *prestación de servicios*. El filtro de RACO, con la misma búsqueda, sí trajo procesos que
   mencionan obras.
3. **Relevancia (prueba 1):** uno de los 5 resultados coincide solo porque la entidad se llama
   "Distrito Especial de Ciencia, **Tecnología** e Innovación de Medellín".
4. **Duplicados (prueba 3):** de las 5 filas, 4 son el mismo proceso (`CO1.REQ.10379272`,
   ferretería de Funza). Solo hay 2 contratos distintos. El filtro sí cumplió (todos
   ≥ $500 M en Cundinamarca).

Con estos hallazgos, el chat sirve para hacer la consulta en lenguaje natural y en un solo
paso, pero hoy entrega menos resultados útiles que el filtro.
