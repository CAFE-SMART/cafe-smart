package com.cafesmart.app.navigation

/**
 * Nombres de ruta de todas las pantallas de Café Smart.
 * Convención: snake_case, en inglés, "<modulo>_<pantalla>".
 */
object Routes {

    // ── Cuenta (autenticación) ──────────────────────────────
    const val SPLASH = "splash"                    // Cuenta · Pantalla de carga
    const val LOGIN = "login"                      // Cuenta · Iniciar sesión
    const val REGISTER_STEP1 = "register_step1"    // Cuenta · Crear cuenta · Paso 1
    const val REGISTER_STEP2 = "register_step2"    // Cuenta · Crear cuenta · Paso 2

    // ── Inicio ──────────────────────────────────────────────
    const val HOME = "home"                        // Inicio · Panel principal

    // ── Compras ─────────────────────────────────────────────
    const val PURCHASE_STEP1 = "purchase_step1"    // Paso 1 · Productor
    const val PURCHASE_STEP2 = "purchase_step2"    // Paso 2 · Café
    const val PURCHASE_STEP3 = "purchase_step3"    // Paso 3 · Confirmar
    const val PURCHASE_DONE = "purchase_done"      // Compra registrada

    // ── Inventario ──────────────────────────────────────────
    const val INVENTORY = "inventory"              // Inventario · Resumen (Todos)
    const val INVENTORY_TYPE = "inventory_type"    // Inventario · Lista · Verde (calidades de un tipo de café)
    const val INVENTORY_LIST = "inventory_list"    // Inventario · Detalle · Lista de sublotes (de un grupo)
    const val ARG_BATCH_ID = "batchId"
    const val INVENTORY_DETAIL = "inventory_detail/{$ARG_BATCH_ID}"                  // Detalle del sublote
    const val INVENTORY_BATCH_EXPENSES = "inventory_batch_expenses/{$ARG_BATCH_ID}"  // Gastos del sublote

    // ── Ventas ──────────────────────────────────────────────
    const val SALE_STEP1 = "sale_step1"            // Paso 1 · Cliente
    const val SALE_STEP2 = "sale_step2"            // Paso 2 · Café
    const val SALE_STEP3 = "sale_step3"            // Paso 3 · Confirmar
    const val SALE_DONE = "sale_done"              // Venta registrada

    // ── Venta desde un sublote (2 pasos, botón "Vender sublote") ──
    const val SALE_BATCH_STEP1 = "sale_batch_step1/{$ARG_BATCH_ID}"  // Paso 1 · Cliente
    const val SALE_BATCH_STEP2 = "sale_batch_step2/{$ARG_BATCH_ID}"  // Paso 2 · Confirmar venta

    // ── Secado ──────────────────────────────────────────────
    const val DRYING_STEP1 = "drying_step1"        // Elegir café
    const val DRYING_STEP2 = "drying_step2"        // Confirmar inicio
    const val DRYING_STEP3 = "drying_step3"        // Iniciado
    const val DRYING_STEP4 = "drying_step4"        // En curso
    const val DRYING_STEP5 = "drying_step5"        // Finalizar
    const val DRYING_STEP6 = "drying_step6"        // Secado registrado
    const val DRYING_STEP7 = "drying_step7"        // Gastos del secado (opcional: 5 → 7 → 6)

    // ── Gastos ──────────────────────────────────────────────
    const val EXPENSES = "expenses"                        // Lista general
    const val EXPENSE_NEW = "expense_new"                  // Registrar gasto
    const val EXPENSE_LINK_BATCHES = "expense_link_batches" // Asociar a sublotes

    // ── Ajustes ─────────────────────────────────────────────
    const val SETTINGS = "settings"                // Cuenta · Ajustes

    // ── Helpers para rutas con argumentos ───────────────────
    fun inventoryDetail(batchId: String) = "inventory_detail/$batchId"
    fun inventoryBatchExpenses(batchId: String) = "inventory_batch_expenses/$batchId"
    fun saleBatchStep1(batchId: String) = "sale_batch_step1/$batchId"
    fun saleBatchStep2(batchId: String) = "sale_batch_step2/$batchId"
}