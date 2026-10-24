# Café Smart

App móvil Android que digitaliza la compra, el almacenamiento y la venta de café. Da a los dueños de estos negocios información clara y confiable para decidir mejor y reducir pérdidas.

## Problema
- Hoy el proceso se lleva en cuadernos físicos.
- La merma y la utilidad se calculan a mano.
- Hay poca trazabilidad de los lotes y errores frecuentes.

## Flujo del MVP
Inicio → Compra → Inventario → Gastos → Venta → Resultado financiero

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

## Cómo ejecutarlo
1. Clona el repositorio:
   ```
   git clone https://github.com/CAFE-SMART/cafe-smart.git
   ```
2. Ábrelo en Android Studio (versión reciente).
3. Espera a que Gradle sincronice.
4. Ejecuta en un emulador o en un celular Android.

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
