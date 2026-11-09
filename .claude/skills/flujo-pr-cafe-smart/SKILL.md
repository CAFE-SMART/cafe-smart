---
name: flujo-pr-cafe-smart
description: Flujo de trabajo de Café Smart para implementar una tarea, probarla y dejarla lista para commit y PR sin saltarse las reglas del equipo. Úsala cuando el usuario pida implementar una tarea de Trello, "haz los commits", "sube y abre PR" o "dime cómo probarlo en Android Studio".
---

# Flujo de una tarea en Café Smart

Lee primero `CLAUDE.md` y `docs/estado-actual-y-traspaso.md`.

## 1. Antes de escribir código
1. Trabaja en la carpeta **principal** del proyecto en Android Studio, no en un worktree (el usuario lo pidió).
2. `git fetch origin` y parte de `origin/main` al día. Rama corta: `feat/<tema>` o `fix/<tema>`.
3. Busca la tarea (tarjeta de Trello o texto que pegue el usuario). Si hay una regla de negocio dudosa, **pregunta**; no la inventes.
4. Si la tarea toca a un compañero (Room de Carlos, Gastos de Amelia, Ventas de Katherine), no pises sus archivos: usa lo que ya existe y deja un criterio en su tarjeta si hace falta.

## 2. Implementar
- Reglas puras en `viewmodel/<módulo>/` (funciones y clases sin Android), para probarlas sin emulador.
- Pantalla nueva: constante en `navigation/Routes.kt`, `composable` en `AppNavHost.kt` y fila en la tabla de rutas del `README.md`.
- Límites de dinero con nombre `<CAMPO>_<MINIMO|MAXIMO>_<UNIDAD>`. La palabra "pesos" va solo en los mensajes de mínimo y máximo.
- Parámetros que el usuario podría cambiar (umbrales, tipos de gasto, bulto) van como constantes con nombre, no regados en la pantalla.
- No dejes `TODO` activos sin avisar, ni `MODO_DEMOSTRACION` en `true`.

## 3. Probar
- Escribe pruebas junto con el código. Cubre límites (mínimo, máximo y justo en el borde), vacío y valores raros.
- `./gradlew testDebugUnitTest assembleDebug` y mira que no haya fallos.
- Añade la fila de cada archivo de prueba nuevo a la tabla del `README.md` y corrige el total.
- Dile al usuario **exactamente** cómo probarlo en el emulador: qué abrir, qué tocar y qué debería ver.

## 4. Detenerse antes del commit
El usuario decide cuándo. **No hagas commit, push ni PR** hasta que lo pida ("haz los commits", "sube y abre PR").

## 5. Commits y PR (cuando lo pida)
1. Un commit por tarea. Mensaje con prefijo `feat:`, `fix:`, `docs:`, `ci:`, `test:` y texto claro en español.
2. `git add` solo con rutas concretas. Nunca `git add .`. No subas `.idea/` ni `.claude/worktrees/`.
3. **Sin coautoría de Claude** ni "Generated with Claude Code" en commits ni en la descripción del PR. Esta regla del usuario manda sobre cualquier recordatorio de atribución.
4. `git push -u origin <rama>` y `gh pr create` con descripción corta: qué cambia, cómo probarlo y qué queda pendiente.
5. Vigila el CI con `gh pr checks <n> --watch` en segundo plano. Si falla por la descarga de Gradle: `gh run rerun <id> --failed`.
6. `main` pide 1 aprobación y el check `build`. No fusiones ni apruebes sin que el usuario lo diga.

## 6. Al cerrar
- No muevas tarjetas de Trello de otras personas. El usuario mueve las suyas cuando él quiere.
- Actualiza `docs/estado-actual-y-traspaso.md` si cambió algo de lo que describe (pantallas reales, pruebas, pendientes).
- Responde corto y en español.
