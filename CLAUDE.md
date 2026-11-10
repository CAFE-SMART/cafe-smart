# Café Smart · guía para la IA

App Android (Kotlin + Jetpack Compose) que digitaliza compra, inventario, secado, gastos y venta de café. Proyecto de la materia Aplicaciones Móviles. Equipo de 4. **Empieza leyendo [`README.md`](README.md)** (rutas, pruebas y flujo de Git) y [`docs/modelo-de-datos.md`](docs/modelo-de-datos.md). El estado de las tareas vive en Trello (revísalo antes de confiar en él) y la fuente de verdad del código es el propio repo.

## Reglas de oro
- Responde en **español**, con frases cortas y sin relleno.
- **Sin coautoría de Claude** en commits ni en PRs (nada de `Co-Authored-By` ni "Generated with Claude Code"). El autor es siempre el usuario.
- **Nunca** `git add .` ni `git add -A`: añade solo archivos concretos. `.idea/` y `.claude/worktrees/` no se tocan.
- **No subas, comentes, apruebes, fusiones ni borres ramas** sin que el usuario lo pida en ese momento.
- **Las decisiones del equipo se preguntan**, no se inventan (reglas de negocio, rangos, textos que ve el usuario).
- No copies secretos: `app/google-services.json` y las credenciales de prueba están fuera del repo.
- Una sola rama `main`, siempre compilable (trunk-based). Está protegida: todo entra por PR con 1 aprobación y el check `build` en verde.
- Los mensajes de error que hablan de dinero dicen "pesos" **solo** en los de mínimo y máximo.

## Cómo se corre y se prueba
- Android Studio reciente y JDK 17. Pon tu `app/google-services.json` (ver el README).
- Todas las pruebas y el APK: `./gradlew testDebugUnitTest assembleDebug`.
- Una clase: `./gradlew :app:testDebugUnitTest --tests "*NombreDeLaClase"`.
- Para probar un PR sin tocar tu carpeta: worktree temporal, copiar `local.properties` y `app/google-services.json.example` como `google-services.json`.
- Las pruebas de `androidTest` no corren en el CI; necesitan emulador.

## Dónde está cada cosa
- `app/src/main/java/com/cafesmart/app/`: `ui/` (pantallas Compose), `viewmodel/` (estado y reglas puras), `data/` (modelo, repositorios, datos de ejemplo), `navigation/` (`Routes.kt` y `AppNavHost.kt`).
- `docs/modelo-de-datos.md`: entidades, relaciones y reglas del modelo.
- `README.md`: rutas, pruebas y flujo de Git.
- `.claude/skills/auditar-pr/`: cómo auditar y corregir un PR de este repo.
- `docs/skills-globales/`: skills útiles en cualquier proyecto (se copian a `~/.claude/skills/`).

## Datos que se confunden
- **Capacidad de bodega 0** = "sin configurar" (estado propio). **Kg en 0 con capacidad** = bodega vacía (normal).
- Umbrales de bodega: menos de 80 % normal; 80 a menos de 90 % amarillo sin mensajes; 90 % o más rojo. Nunca bloquea una compra.
- Utilidad = ingresos − costo de compra − gastos (los gastos **restan**), en pesos enteros.
- Un bulto = 40,7 kg (valor inicial; se configurará en Ajustes).
- Firebase Auth solo guarda la cuenta (correo, contraseña y nombre). Los datos del negocio (nombre, tipo, teléfono, capacidad de bodega, peso del bulto) viven en la entidad `Negocio` de Room (`data/local/`, clave = uid de Firebase; se guarda al registrarse y se edita en Ajustes → Información de empresa). Room es local: si se reinstala la app se pierde y se vuelve a llenar en esa pantalla.
