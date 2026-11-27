# Propuesta para Andrés: «Tu ganancia de hoy $ 0» sin ventas conectadas

**Estado:** propuesta. No se modificó el código del Resumen financiero.

## Qué pasa

En el Xiaomi (app aislada `com.cafesmart.app.qa.ciclo3`), con un gasto registrado de $ 75.000, el Resumen muestra:

- «Tu ganancia de hoy **$ 0**» y «Hoy todavía no registras ventas.»
- «Gastos (de ese café) − $ 0».
- «Gastos registrados **$ 75.000**».

Evidencia: `xiaomi-componentes/G05_resumen_con_gasto.png` y `G06_grafica_mes_a_mes.png`.

## Causa

- `resumirVentas` (`data/finanzas/FinanzasRepository.kt`) calcula `utilidad` **solo con las ventas**: ingresos − costo del café − gastos asociados a esas ventas. Es la regla aprobada (los gastos restan en la utilidad de lo vendido).
- `OrigenFinancieroRoom` hoy solo conecta `gastos()`. `ventas()`, `compras()` y `mermas()` devuelven lista vacía hasta que Katherine (Ventas #43, Secado #44-#46) y Carlos (Compras) las conecten.
- Sin ventas, `utilidad` siempre vale 0. La tarjeta muestra «$ 0» con el mismo peso visual que una ganancia real de cero, aunque el dato **no se puede calcular todavía**.

## Corrección propuesta (requiere el visto bueno de Andrés)

1. Contrato (`OrigenFinanciero`, en `FinanzasRepository.kt`): agregar
   `val ventasConectadas: Boolean get() = false`.
   Es un valor por defecto, así que no rompe a quien implemente la interfaz. `OrigenFinancieroRoom` lo pondrá en `true` cuando conecte las ventas.
2. `ResumenFinanciero`: agregar `val utilidadDisponible: Boolean = true`, que `FinanzasLocal.resumen()` llena con `origen.ventasConectadas`. El valor por defecto `true` no cambia las pruebas actuales.
3. `ResumenFinancieroScreen` (tarjeta «Tu ganancia»): si `!utilidadDisponible`, mostrar «—» en vez de «$ 0» y el texto «La ganancia se calculará cuando se conecten las ventas.» El color queda neutro (`TextPrimary`), sin verde ni rojo.
4. `mensajeDeUtilidad`: sin cambios. Solo se usa cuando la utilidad está disponible.

**Afecta a:**
- `FinanzasRepository.kt`, `ResumenFinancieroScreen.kt` y `ResumenFinancieroViewModel.kt`, de Andrés.
- `OrigenFinancieroRoom.kt`, de Amelia.
- Las pruebas `ResumenFinancieroTest` y `HistorialFinancieroTest`. Siguen pasando con los valores por defecto; habría que agregar un caso con `ventasConectadas = false`.

**Alternativa sin tocar contratos:** ocultar la tarjeta «Tu ganancia» mientras `resumen.ventas == 0`. Es menos precisa: no distingue «no vendiste hoy» de «las ventas no están conectadas».

**Texto propuesto que ve el usuario:** «La ganancia se calculará cuando se conecten las ventas.» Falta aprobarlo.
