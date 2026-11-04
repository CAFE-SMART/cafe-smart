# Café Smart

App móvil Android que digitaliza la compra, el almacenamiento y la venta de café. Da a los dueños de estos negocios información clara y confiable para decidir mejor y reducir pérdidas.

## Problema
- Hoy el proceso se lleva en cuadernos físicos.
- La merma y la utilidad se calculan a mano.
- Hay poca trazabilidad de los lotes y errores frecuentes.

## Flujo del MVP
Inicio → Compra → Inventario → Secado → Gastos → Venta → Resultado financiero

## Tecnologías
- Kotlin + Jetpack Compose
- Navigation Compose
- Arquitectura MVVM (ViewModel + StateFlow)
- Room (almacenamiento local) — por incorporar
- Firebase Authentication — por incorporar
- JUnit (pruebas unitarias)

## Estructura del proyecto
```
app/src/main/java/.../
├── ui/          # Pantallas, tema y componentes (Compose)
├── viewmodel/   # Estado y lógica de cada pantalla
└── data/        # Modelos y repositorios
```

## Navegación (Navigation Compose)
Todo el grafo vive en `navigation/AppNavHost.kt` y los nombres de ruta en `navigation/Routes.kt`.
Cada ruta corresponde a una pantalla del Figma (pestaña "Flujos"). Por ahora son *placeholders* que se
reemplazan por la pantalla real dentro de su `composable(...)`.

**Flujo base:** `splash` → `login` → `home` → `inventory`
(el splash y el login se sacan de la pila: "atrás" no vuelve a ellos).

**Orden de pantallas (igual que el prototipo del Figma):**
- Inventario: `inventory` (resumen) → `inventory_type` (calidades de un tipo, p. ej. Verde) → `inventory_list` (sublotes del grupo) → `inventory_detail/{batchId}` → `inventory_batch_expenses/{batchId}` o `sale_batch_step1/{batchId}` → `sale_batch_step2/{batchId}` → `sale_done`.
- Secado: `drying_step1` → `2` → `3` → `4` → `5` → `drying_step6` (sin gastos) **o** `5` → `drying_step7` (gastos) → `drying_step6`. El paso 6 siempre es el final.
- Se entra a Secado desde `inventory_type` (botón "Iniciar secado") o desde `settings` ("Proceso de secado"); a la lista de gastos (`expenses`) desde `settings`; a "Registrar gasto" (`expense_new`) desde `home`.
- `home` y `settings` tienen "Salir" / "Cerrar sesión", que vuelve a `login` y borra la pila.

| Módulo | Ruta | Pantalla |
|---|---|---|
| Cuenta | `splash` | Pantalla de carga (pantalla inicial) |
| Cuenta | `login` | Iniciar sesión |
| Cuenta | `register_step1` | Crear cuenta · Paso 1 |
| Cuenta | `register_step2` | Crear cuenta · Paso 2 |
| Inicio | `home` | Panel principal |
| Compras | `purchase_step1` | Paso 1 · Productor |
| Compras | `purchase_step2` | Paso 2 · Café |
| Compras | `purchase_step3` | Paso 3 · Confirmar |
| Compras | `purchase_done` | Compra registrada |
| Inventario | `inventory` | Resumen (Todos) por tipo |
| Inventario | `inventory_type` | Lista · Verde (calidades de un tipo de café) |
| Inventario | `inventory_list` | Lista de sublotes de un grupo |
| Inventario | `inventory_detail/{batchId}` | Detalle del sublote |
| Inventario | `inventory_batch_expenses/{batchId}` | Gastos del sublote |
| Ventas | `sale_batch_step1/{batchId}` | Vender sublote · Paso 1 · Cliente |
| Ventas | `sale_batch_step2/{batchId}` | Vender sublote · Paso 2 · Confirmar venta |
| Ventas | `sale_step1` | Paso 1 · Cliente |
| Ventas | `sale_step2` | Paso 2 · Café |
| Ventas | `sale_step3` | Paso 3 · Confirmar |
| Ventas | `sale_done` | Venta registrada |
| Secado | `drying_step1` | Paso 1 · Elegir café |
| Secado | `drying_step2` | Paso 2 · Confirmar inicio |
| Secado | `drying_step3` | Paso 3 · Iniciado |
| Secado | `drying_step4` | Paso 4 · En curso |
| Secado | `drying_step5` | Paso 5 · Finalizar |
| Secado | `drying_step6` | Paso 6 · Secado registrado |
| Secado | `drying_step7` | Paso 7 · Gastos del secado |
| Gastos | `expenses` | Lista general |
| Gastos | `expense_new` | Registrar gasto |
| Gastos | `expense_link_batches` | Asociar a sublotes |
| Ajustes | `settings` | Ajustes |

Reglas: nombres en `snake_case`; los argumentos van entre llaves (`{batchId}`) y se arman con los
helpers de `Routes` (p. ej. `Routes.inventoryDetail("12")`). Los modales y diálogos no son rutas.
Si agregas una pantalla, añade su constante en `Routes.kt`, su `composable` en `AppNavHost.kt` y su fila aquí.

## Cómo ejecutarlo
1. Clona el repositorio:
   ```
   git clone https://github.com/CAFE-SMART/cafe-smart.git
   ```
2. Ábrelo en Android Studio (versión reciente).
3. Espera a que Gradle sincronice.
4. Ejecuta en un emulador o en un celular Android.

## Configuración de Firebase
`google-services.json` no está en el repositorio. Las credenciales están en el Google Doc del equipo (pestaña "Móvil — Credenciales y Firebase"); descárgalo de ahí y colócalo en `app/`. Como referencia de su estructura está `app/google-services.json.example` (quítale el `.example` al nombre).

## Pruebas
El proyecto usa **JUnit 4** para las pruebas unitarias. Están en `app/src/test/java/com/cafesmart/app/` y no necesitan emulador ni celular.

### Cómo correrlas en Android Studio
1. Abre la carpeta `app/src/test/java/` en el panel *Project*.
2. Clic derecho sobre una clase de prueba (por ejemplo `WarehouseCapacityTest`) → **Run 'WarehouseCapacityTest'**.
3. Para correr todas: clic derecho sobre la carpeta `app/src/test/java` → **Run 'Tests in ...'**.
4. El resultado aparece abajo, en la pestaña *Run* (verde = pasa, rojo = falla).

### Cómo correrlas en la terminal
Desde la raíz del proyecto (necesitas el `google-services.json`; ver *Configuración de Firebase*):
```
./gradlew testDebugUnitTest
```
Para una sola clase:
```
./gradlew :app:testDebugUnitTest --tests "*WarehouseCapacityTest"
```
El reporte en HTML queda en `app/build/reports/tests/testDebugUnitTest/index.html` (ábrelo en el navegador) y los resultados en XML en `app/build/test-results/testDebugUnitTest/`.

### Reporte en GitHub Actions
En cada pull request el workflow `CI` corre las pruebas y guarda el reporte:
- En la pestaña *Summary* de la ejecución hay una tabla con las pruebas, fallos y errores de cada archivo.
- Al final de esa página, en *Artifacts*, se descarga `reporte-pruebas-junit` (el HTML y los XML; se conserva 14 días).

### Archivos de prueba
| Archivo | Qué prueba | Pruebas |
|---|---|---|
| `data/model/ModeloDatosTest.kt` | Cálculos del modelo de datos (merma del secado, valor en bodega, costos) | 12 |
| `data/model/ReglasModeloTest.kt` | Validaciones de las entidades (sublotes, gastos, repartos) | 15 |
| `purchase/PurchaseRulesTest.kt` | Reglas del registro de compra (productores, pesos, precios) | 21 |
| `purchase/PurchaseViewModelTest.kt` | Estado y pasos del flujo de compra | 24 |
| `ui/auth/LoginValidationTest.kt` | Validaciones de inicio de sesión (correo y contraseña) | 15 |
| `viewmodel/drying/MermaCalculatorTest.kt` | Porcentaje de merma del secado | 7 |
| `viewmodel/inventory/WarehouseCapacityTest.kt` | Ocupación de la bodega y sus estados (normal, advertencia, crítica, sin capacidad) | 36 |
| `viewmodel/finance/ProfitCalculatorTest.kt` | Utilidad de una venta (ingresos − costo de compra − gastos) | 25 |
| `viewmodel/finance/ExpenseRulesTest.kt` | Validaciones de un gasto (concepto, tipo, valor, fecha, descripción) | 45 |
| `ExampleUnitTest.kt` | Prueba de ejemplo de Android Studio | 1 |

Total: 201 pruebas. Al agregar un archivo de prueba, añade su fila a esta tabla.

## Flujo de trabajo en Git (trunk-based)
- Un solo tronco: `main`, siempre compilable.
- Ramas cortas (1-2 días) con prefijo `feat/` o `fix/`.
- Commits pequeños con mensaje claro.
- Pull de `main` antes de abrir cada PR.
- PR pequeño, revisado por otra persona.

## Equipo
- Andrés Mauricio Peña López
- Katherine López Unás
- Luz Amelia Ibargüen Ibarra
- Carlos Manuel Arango Guerrero

## Contexto académico
Proyecto del curso Desarrollo de Aplicaciones para Dispositivos Móviles.
