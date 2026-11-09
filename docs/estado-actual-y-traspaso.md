# Estado actual y traspaso · Café Smart

> Foto del **9 de noviembre de 2026**. `main` está en el commit `7dd859b` (PRs #1 a #15 fusionados, ninguno abierto).
> Lo del tablero (Trello) envejece rápido: revísalo antes de confiar en él. Lo del código se verificó ese día.

## En cinco líneas
- App Android con Kotlin y Jetpack Compose, arquitectura MVVM, sin servidor propio.
- El flujo largo (compra → inventario → secado → gastos → venta) ya tiene **forma**, pero buena parte usa datos de ejemplo o pantallas vacías.
- Lo real hoy: inicio de sesión y registro con Firebase, compra (en memoria), resumen de inventario, gastos (en memoria) y Ajustes.
- Falta la base de datos local (Room). Cuando llegue, Compras, Inventario, Gastos e Inicio dejan de usar memoria y datos de ejemplo.
- Hay 293 pruebas unitarias en 19 archivos y todas pasan. El CI las corre en cada PR.

## Qué funciona
| Módulo | Rutas | Estado | Datos |
|---|---|---|---|
| Cuenta | `splash`, `login`, `register_step1`, `register_step2`, `register_success` | Real | Firebase Auth (correo, contraseña y nombre) |
| Inicio | `home` | Real, con datos de ejemplo | `data/sample/InventarioEjemplo.kt` |
| Compras | `purchase_step1` a `purchase_step3`, `purchase_done` | Real | En memoria (se pierde al cerrar la app) |
| Inventario | `inventory` (resumen por tipo) | Real, con datos de ejemplo | `data/sample/InventarioEjemplo.kt` |
| Gastos | `expenses`, `expense_new`, `expense_link_batches` | Real | `GastosRepository` en memoria |
| Ajustes | `settings` | Real: perfil y "Cerrar sesión" | Sesión de Firebase |

Cálculos ya hechos y probados (en `viewmodel/`): ocupación de bodega, utilidad por venta, merma del secado, validaciones de compra, de gasto y de perfil. Ojo: `ProfitCalculator` y `ExpenseRules` están probadas pero aún no se muestran en ninguna pantalla.

## Qué falta
Son pantallas vacías (`NavPlaceholder`): solo muestran el título y botones para navegar.
- Inventario: `inventory_type/{tipo}`, `inventory_list`, `inventory_detail/{batchId}`, `inventory_batch_expenses/{batchId}`.
- Ventas: `sale_step1` a `sale_step3`, `sale_done`, `sale_batch_step1/{batchId}`, `sale_batch_step2/{batchId}`.
- Secado: `drying_step1` a `drying_step7`.

Tampoco existe todavía:
- **Room** (no está en las dependencias). Entidades, DAOs y datos de ejemplo cargados al crear la base.
- Entidad `Negocio` en Room: una fila, con el `uid` de Firebase como id.
- Ajustes completos: capacidad de bodega, peso por bulto y límites de dinero editables. Hoy la capacidad es un dato de ejemplo (`CAPACIDAD_EJEMPLO_KG`) y el bulto es la constante `KG_POR_BULTO`.
- Aviso de bodega en el paso 3 de Compras (la lógica de ocupación existe; falta mostrarla ahí).
- Sincronización y modo sin conexión: **fuera del MVP** según el documento de requisitos.

## Cómo se corre y se prueba
1. Android Studio reciente, JDK 17 y Gradle sincronizado.
2. Pon `app/google-services.json` (el real; el repo solo trae `google-services.json.example`).
3. Como `MODO_DEMOSTRACION` vale `false`, para entrar necesitas **una cuenta real de Firebase** (créala desde "Crear cuenta").
4. Pruebas y APK: `./gradlew testDebugUnitTest assembleDebug`.
5. Reporte HTML: `app/build/reports/tests/testDebugUnitTest/index.html`.

Las 5 clases de `androidTest` compilan, pero **no corren en el CI**: necesitan emulador.

## Producción y GitHub
- `main` es el único tronco y siempre debe compilar (trunk-based). Ramas cortas con prefijo `feat/` o `fix/`.
- `main` está protegida con rulesets: nadie borra ni fuerza `push`; todo entra por PR con **1 aprobación** y el check `build` en verde; una aprobación vieja se descarta si el PR cambia.
- Solo la persona con rol Admin (el líder, Andrés) puede saltarse la revisión, y solo en modo "pull request".
- El CI (`.github/workflows/ci.yml`) corre en cada PR hacia `main`: JDK 17, copia `google-services.json.example` como `google-services.json`, ejecuta `./gradlew testDebugUnitTest assembleDebug`, escribe una tabla en el resumen y sube el artefacto `reporte-pruebas-junit` (14 días).
- Fallo conocido y pasajero: Gradle a veces no alcanza a descargar su distribución. Se repite con `gh run rerun <id> --failed`.
- Cómo auditar un PR de un compañero: `.claude/skills/auditar-pr/SKILL.md`.

## Reglas de oro
1. **Sin coautoría de Claude** en commits ni PRs.
2. Nada de `git add .`: añade archivos concretos. No subas `.idea/` ni `.claude/worktrees/`.
3. No subas, apruebes, fusiones ni borres ramas sin que el usuario lo pida en ese momento.
4. Una cosa a la vez: un PR por tarea, y un commit por tarea si van en la misma rama.
5. Las reglas de negocio se preguntan. No se inventan rangos, textos ni estados.
6. "pesos" solo en los mensajes de mínimo y máximo de dinero.
7. Nunca copies secretos al repo, al vault ni a la memoria.

## Datos de negocio que se confunden
- **Capacidad 0** es "bodega sin configurar" (estado `SIN_CAPACIDAD`). **Kg en 0 con capacidad** es una bodega vacía y es normal.
- Umbrales de ocupación (parámetros, no fijos en la pantalla): menos de 80 % normal; de 80 % a menos de 90 % amarillo, solo visual, sin mensajes; 90 % o más rojo. En Compras el aviso sale en el paso 3; en Inventario se ve en rojo. **Nunca bloquea una compra.**
- **Utilidad** = ingresos − costo de compra − gastos. Los gastos **restan**. Pesos enteros, redondeo documentado en `ProfitCalculator`.
- **Bulto** = 40,7 kg (valor inicial, configurable después).
- Nombres de límites: `<CAMPO>_<MINIMO|MAXIMO>_<UNIDAD>` (por ejemplo `VALOR_GASTO_MINIMO_COP`).
- Tipos de gasto: Transporte, Comida, Secado, Cargue, Descargue y Otros (constantes fáciles de cambiar).
- Firebase Auth solo guarda la cuenta. Nombre del negocio, tipo, teléfono, capacidad y peso por bulto van en `Negocio` (Room).
- Según el ERS: un negocio, una sola bodega, almacenamiento local en Room.

## Problemas conocidos y deuda
- **Registro pierde datos del negocio**: `AppNavHost.kt` tiene un `TODO(Negocio)`. Se guardan cuando exista la entidad.
- **La sesión no se reutiliza** al reabrir la app y el interruptor "Recordar" se ignora (RF-G-04).
- `MODO_DEMOSTRACION = false` deja ramas de demo en el código, y `CORREO_SOPORTE` está vacío (el diálogo de soporte no abre ningún correo).
- Hay un botón "Ver datos de ejemplo" que se quita cuando Inventario lea de Room.
- **Límites de dinero sin unificar.** `ExpenseRules` (mínimo 1.000 y máximo 50.000.000 pesos) está hecha y probada, pero **ningún código la usa todavía**: el formulario de Gastos usa `GastosValidation`, que solo pide un entero positivo, sin mínimo ni máximo. Compras tiene los suyos en `viewmodel/purchase/PurchaseRules.kt` (precio de 1.000 a 100.000 por kg, peso máximo 99.999 kg). El criterio de usar `ExpenseRules` ya está en la tarjeta "Gastos en Room" de Amelia.
- El texto "Capacidad alta" aparece sobre el amarillo en Inventario. El usuario lo aceptó; confirma con el equipo si molesta.
- Al Figma le faltan los marcos de inicio de sesión y de modo sin conexión.
- Perfil real y tipo de negocio dependen de `Negocio` (Room).
- Las clases de `androidTest` no se ejecutan en el CI.

## Tablero (foto del 9 de noviembre)
Tablero "Aplicaciones Moviles" de Trello. Total 38 tarjetas: 8 por hacer, 6 en progreso, 0 en revisión, 24 terminadas.
- **Andrés**: Ajustes con perfil y cerrar sesión (en progreso; falta la parte que depende de Room).
- **Carlos**: Room base de datos (en progreso). Por hacer: Compras conectadas a Room, datos de ejemplo (seed), Inventario conectado a Room, Detalle del sublote.
- **Amelia**: Pantalla Gastos del sublote (en progreso). Por hacer: Gastos en Room, Inicio con inventario y gastos reales.
- **Katherine**: `SaleRules`/`SaleViewModel`, Ventas paso 1 y paso 2 (en progreso). Por hacer: Ventas paso 3 y venta registrada, clientes de ejemplo y hoja "Cancelar venta".
- Cada quien mueve **sus** tarjetas. La tarjeta de Room de Carlos incluye el criterio de la entidad `Negocio`.

## Historial de PRs (todos fusionados)
- #1 y #2 (Andrés): tema y componentes base. #4 (Carlos): navegación. #5 (Amelia): splash e Inicio. #6 (Katherine): compra, pasos 1 a 3.
- #8 y #9 (Andrés): merma, CI y pruebas de login. #10 (Carlos): modelo de datos.
- #11 (Andrés): capacidad de bodega, utilidad, reglas de gasto, reporte de pruebas en el CI y Ajustes.
- #12 y #15 (Amelia): Gastos. #13 (Carlos): login con Firebase. #14 (Carlos): pantalla de Inventario.

## Calendario del curso
- Revisiones: 19 de noviembre y 3 de diciembre. Entrega 2: 10 de diciembre. Final: 17 de diciembre.
- Clase de Firebase: 26 de noviembre. Clase de bases de datos locales: 19 de noviembre.

## Cómo trabaja el usuario (Andrés)
- Habla español y quiere respuestas **cortas y claras**.
- Primero implementa y prueba; **no hace commit ni PR hasta que él lo pide**. Luego: "haz los commits", "sube y abre PR".
- Trabaja en su carpeta principal de Android Studio, no en un worktree.
- Pide que le digas **exactamente cómo probar** en el emulador.
- Es el líder: audita los PRs del equipo uno a uno y prefiere corregir en la rama del compañero solo si él lo confirma.
- Los números y las decisiones deben quedar en la documentación, no solo en el chat.

## Dónde mirar
- `README.md`: rutas, pruebas y flujo de Git.
- `docs/modelo-de-datos.md`: entidades y reglas del modelo.
- `CLAUDE.md`: reglas para la IA.
- Fuera del repo: Google Doc del equipo (requisitos y credenciales), tablero de Trello, archivo de Figma, vault de notas y memoria de Claude.

## Lo que no se pudo verificar al escribir esto
- Si la exposición del 5 de noviembre se dio y cómo salió.
- El inicio de sesión real con Firebase en un celular (las pruebas usan dobles).
- La fidelidad visual contra el Figma.
- Que las pruebas de `androidTest` pasen.
- El estado del tablero después del 9 de noviembre.
