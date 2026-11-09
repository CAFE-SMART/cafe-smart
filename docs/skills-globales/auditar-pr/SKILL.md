---
name: auditar-pr
description: Audita cualquier Pull Request de GitHub (Java/Maven, Node, Python, Android, etc.) en solo lectura y entrega un veredicto en español: alcance real del diff, conflictos con main, compilación y pruebas en un worktree temporal, CI, criterios de aceptación del ticket, configuración del repo y afirmaciones de la documentación. Úsala cuando el usuario pida "auditar", "revisar", "evaluar" o "analizar" uno o varios PRs, o "qué PR reviso primero".
---

# Auditar un PR (genérico)

La auditoría es **solo lectura**. No se modifica el árbol de trabajo del usuario, ni se hace commit, push, comentario, merge o borrado de ramas sin que el usuario lo pida después. Corregir los hallazgos es una fase aparte (ver el final).

## Reglas fijas
- Responder en **español**, con viñetas y lenguaje llano; explicar los hallazgos para alguien que no domina el tema.
- **Sin firma ni coautoría de Claude** en commits, PRs ni comentarios (sin `Co-Authored-By` ni "Generated with Claude Code"). El autor es el usuario.
- Nunca `git add .` ni `git add -A`: añadir solo archivos concretos.
- Un resultado de revisión automática (subagente, `/code-review`, clasificador) es **una pista, no un hecho**. Verificar cada hallazgo leyendo el código antes de presentarlo, y decir qué se ejecutó y qué no.
- En zsh, usar `"${B}:ruta"` con llaves en `git show`, y no empezar un `echo` con `=====`.
- No afirmar que algo "funciona" o "está en verde" sin haberlo visto en GitHub o ejecutado.

## Pasos

### 1. Ubicar los PRs y elegir el orden
- `gh pr list --state open --json number,title,author,headRefName,changedFiles,additions,reviewDecision,mergeStateStatus,statusCheckRollup`
- Si hay varios, **recomendar cuál auditar primero** y por qué: el que desbloquea a los demás (por ejemplo, el que trae el workflow de CI), el que aún no tiene revisión, el de menor riesgo de choque.
- Leer la tarea o rúbrica que el usuario pegue; si pasa un enlace de Jira/Linear y hay conector, leer el ticket y sus criterios de aceptación.

### 2. Actualizar y fijar el alcance real
- `git fetch --prune origin`. **Comparar siempre contra `origin/main`**, nunca contra un `main` local que pudo quedar atrás (da diffs inflados y falsos hallazgos).
- Confirmar el alcance: `gh pr view <n> --json files` y `git diff --stat origin/main...origin/<rama>`. Las cifras de GitHub y las locales deben coincidir; si no, resolver la discrepancia antes de seguir.
- Comprobar la rama activa (`git branch --show-current`) antes de lanzar cualquier revisión. Una revisión automática lanzada con otra rama activa **revisa el código equivocado**. Pasarle siempre el diff explícito `origin/main...origin/<rama>` y descartar hallazgos de archivos que no están en el PR.

### 3. Obtener los cambios para poder trabajarlos
- Si el usuario pidió "bajar los cambios": `gh pr checkout <n>` con el árbol limpio (`git status -s`). Si no, trabajar con `git show origin/<rama>:ruta` o un worktree.
- Si el PR es de otra persona, **no empujar a su rama sin confirmación**.

### 4. Simular la fusión
- `git merge-tree --write-tree --name-only origin/main origin/<rama>`. Con varios PRs abiertos, simular también entre sí. Explicar cada conflicto: por qué choca y cómo se resolvería.

### 5. Leer todo lo que cambia
- Leer cada archivo del diff completo, no solo los trozos. Tests incluidos.
- Buscar: validaciones faltantes (rangos, nulos, duplicados, espacios), errores que devuelven un código HTTP equivocado (500 en vez de 400/404/409), concurrencia, acoplamiento entre capas, código duplicado, secretos o credenciales, `TODO` o constantes de demo que llegarían a `main`.
- Si el cambio es de infraestructura (Docker/CI): usuario no-root, capas cacheables, puertos, variables de entorno requeridas, HEALTHCHECK, y **que el CI pruebe que el contenedor realmente arranca** y no solo que la imagen se construye.

### 6. Compilar y probar sin ensuciar el repo
- Detectar la herramienta: `pom.xml`/`mvnw` (Maven), `build.gradle` (Gradle), `package.json` (npm), `pyproject.toml` (pytest), etc.
- Crear un worktree temporal en el scratchpad (`git worktree add --detach <scratchpad>/wt origin/<rama>`), copiar los archivos ignorados que se necesiten (`.env`, `local.properties`), ejecutar el build y las pruebas, contar pruebas por clase y **borrar el worktree** (`git worktree remove --force` + `git worktree prune`).
- Revisar `gh pr checks <n>`. Que un PR no tenga ningún check no es "verde": es **sin verificar** (suele ser que el workflow aún no está en `main`).
- Si una herramienta de CI usa `services:` o `--network host`, no se puede probar en local: decirlo y verificarlo en GitHub tras el push.

### 7. Contrastar documentación y configuración con la realidad
- Toda afirmación del README o de la descripción que sea comprobable se **comprueba**: enlaces a ejecuciones (`gh run view <id>`), ramas citadas, y reglas de protección (`gh api repos/<owner>/<repo>/branches/main/protection` y `.../rulesets`; un 404 en la primera no basta, mirar también los rulesets).
- Contrastar el workflow con los criterios de aceptación. Una "optimización" (por ejemplo, limitar el disparo del CI a `main`) puede romper un criterio que dice "en cada push a cualquier rama".
- Anotar lo que es **configuración del repositorio** (no se arregla con código) y quién debe cambiarla.

### 8. Contrastar con la tarea
- Tabla *criterio de aceptación → estado* (cumple / parcial / falta / sin verificar), con la evidencia de cada fila. Marcar lo que el ticket deja **fuera de alcance** para no pedirlo.

## Formato del informe
1. **Veredicto** en la primera línea: aprobar / aprobar con ajustes / no aprobar.
2. **Verificación:** alcance del diff, conflictos, compilación, número de pruebas, CI (qué se ejecutó y qué no).
3. **Contra la tarea:** tabla criterio → estado.
4. **Lo bueno**, brevemente.
5. **Hallazgos** ordenados de más a menos grave, cada uno con archivo, escenario concreto de fallo y por qué importa. Separar *importantes* de *menores/opcionales*.
6. **Pendiente:** configuración externa, decisiones del equipo, y lo que no se pudo verificar.
7. Cerrar preguntando qué hacer a continuación (corregir, comentar en el PR, pedir cambios). No publicar nada antes.

## Si el usuario pide corregir los hallazgos
1. Trabajar en la rama del PR si es del propio usuario; si es de otra persona, usar una rama nueva desde la suya o pedir confirmación.
2. Cambiar, compilar y correr **toda** la suite; añadir pruebas para cada arreglo.
3. Las decisiones que son del equipo (reglas de negocio, rangos, tope de valores) se **preguntan**, no se inventan.
4. Commit con mensaje claro y solo los archivos tocados, sin firma de Claude. Antes del push, `git fetch` y verificar que es fast-forward; si el remoto trae commits nuevos, **inspeccionarlos** (pueden ser del propio usuario editando desde GitHub) y no forzar.
5. Tras el push, vigilar el CI con `gh pr checks <n> --watch` en segundo plano y reportar el resultado real.
6. Avisar de que un push puede invalidar aprobaciones previas si el repo descarta revisiones obsoletas.
7. No integrar (`gh pr merge`) ni borrar ramas remotas sin que el usuario lo pida; antes de borrar una rama local, comprobar `git branch --merged` y los commits únicos.
8. Al terminar, actualizar `main`, borrar las ramas locales ya integradas y revisar las remotas que queden: indicar cuáles son viejas y cuáles son evidencia que no conviene borrar.
