---
name: auditar-pr
description: Audita un Pull Request del repo CAFE-SMART/cafe-smart sin tocar el árbol de trabajo (conflictos con main, compilación, pruebas, choque de nombres, rutas, tema/Figma, tarea y rúbrica) y entrega un veredicto en español. Úsala cuando el usuario pida "auditar", "revisar" o "analizar" un PR, o diga "ya sabes cómo trabajarlo".
---

# Auditar un PR de Café Smart

Es la versión de este repo. La genérica para otros proyectos está en `docs/skills-globales/auditar-pr/`.

Flujo acordado con el equipo. La auditoría es **solo lectura**: no se modifica el árbol de trabajo ni se hace commit/push/comentario en GitHub salvo que el usuario lo pida después.

## Reglas fijas
- Responder en **español**, con viñetas y de forma breve.
- Los archivos `.idea/` sin seguir se ignoran y **no se tocan**. Nunca `git add .` ni `git add -A`: añadir solo archivos concretos.
- **Sin coautoría de Claude** en commits ni en descripciones de PR.
- No hacer push, ni comentar en el PR, ni borrar ramas sin confirmación explícita del usuario.
- En zsh, escribir `"${B}:ruta"` (con llaves) en `git show`; `$B:ruta` se interpreta mal.

## Pasos
1. **Ubicar el PR:** `gh pr list` / `gh pr view <n>` (título, autor, archivos, descripción). Leer la tarea o rúbrica que pegue el usuario.
2. **Si hay varios PRs abiertos:** recomienda el orden y audita **uno a la vez**. **Actualizar:** `git fetch origin`. Comparar contra `origin/main`.
3. **Simular la fusión sin tocar nada:** `git merge-tree --write-tree --name-only origin/main origin/<rama>`. Si hay varios PR abiertos, simular también entre sí. Para cada conflicto: por qué choca, qué hizo `main` y qué hizo la rama, y cómo se resolvería.
4. **Leer todo lo que cambia:** `git diff --stat origin/main...origin/<rama>` y leer cada archivo con `git show`. Si hay un diagrama o imagen, guardarlo en el scratchpad y mirarlo con Read; comprobar que coincide con el código.
5. **Compilar y probar sin ensuciar el repo:** crear un worktree temporal en el scratchpad (`git worktree add --detach <scratchpad>/wt origin/<rama>`), copiar `local.properties` y poner `app/google-services.json.example` como `app/google-services.json` (ambos están en .gitignore; el ejemplo basta para compilar y probar), correr `./gradlew assembleDebug testDebugUnitTest --rerun-tasks`, contar pruebas por clase desde `app/build/test-results/testDebugUnitTest/*.xml` y **borrar el worktree** (`git worktree remove --force` + `prune`). Revisar también `gh pr checks <n>` (CI). Si el CI falló por una descarga de Gradle (red), no es del PR: `gh run rerun <id> --failed`.
6. **Comprobar que no rompe lo que ya existe en main:**
   - Dos clases/pantallas/componentes con el mismo nombre (aunque estén en paquetes distintos).
   - Rutas de `Routes.kt` sin pantalla real (los placeholders son esperados, pero hay que listarlos).
   - Cambios en `ui/theme` (`Theme.kt`, `Type.kt`, `Color.kt`) que alteren el diseño del Figma.
   - Navegación: que la rama use `AppNavHost`/`Routes` y no una navegación propia paralela.
   - Dependencias entre paquetes al revés (por ejemplo, el modelo importando del formulario).
   - Constantes de demo o `TODO` que quedarían activos en `main` (`MODO_DEMOSTRACION`, `CORREO_SOPORTE`).
7. **Contrastar con la tarea y la rúbrica** (entregable pedido, qué falta, qué se añadió fuera de alcance).

## Formato del informe
- **Veredicto** (aprobar / aprobar con ajustes / no aprobar) en la primera línea.
- **Verificación:** conflictos, compilación, número de pruebas, CI.
- **Contra la tarea:** tabla pedido → estado.
- **Lo bueno.**
- **Observaciones** ordenadas de más a menos importante, cada una con archivo y por qué importa.
- **Recomendación** y qué quedó pendiente de decidir con el equipo.
- Terminar preguntando si se publica el resumen como comentario en el PR (no publicarlo antes).

## Si después piden corregir las observaciones
1. Crear una rama local desde la del PR: `git checkout -b fix/<tema> origin/<rama>`.
2. Cambiar, compilar y correr toda la suite antes de subir; ampliar las pruebas.
3. Si hay decisiones que son del equipo (reglas de negocio, rangos), **preguntar** en vez de inventarlas.
4. Commit con mensaje claro y solo los archivos tocados; verificar que el push es fast-forward (`git merge-base --is-ancestor origin/<rama> HEAD`).
5. Push solo cuando el usuario lo confirme (`git push origin HEAD:<rama-del-pr>`), avisar al autor de que haga `git pull`, y borrar la rama local cuando ya no haga falta.
