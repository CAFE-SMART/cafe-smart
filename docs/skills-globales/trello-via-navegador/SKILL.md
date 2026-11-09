---
name: trello-via-navegador
description: Leer y editar tarjetas de Trello (descripciones, listas, criterios de aceptación, fechas, miembros) usando el navegador integrado cuando no hay conector de Trello. Úsala cuando el usuario pida revisar el tablero, comparar tarjetas con PRs, agregar un criterio a una tarjeta o mover tarjetas.
---

# Trello por el navegador integrado

No hay API key ni conector: se usa la sesión que el usuario ya tiene abierta. **Nunca** copies cookies, tokens ni el parámetro `dsc` a archivos, chat, memoria o commits.

## Preparación
1. Abre el tablero en el navegador integrado (`mcp__Claude_Browser__navigate`). Si pide iniciar sesión, pídele al usuario que lo haga en el panel.
2. Si el navegador integrado falla, prueba Claude in Chrome. Su extensión a veces aparece como "no conectada"; vuelve a intentar o avisa.
3. Ejecuta JavaScript en la página con `javascript_tool`. Las llamadas van al mismo origen (`/1/...`), así usan la sesión.

## Leer (sin riesgo)
```js
// listas con sus tarjetas abiertas
await fetch('/1/boards/<idDelTablero>/lists?cards=open&card_fields=name,idList,due,idMembers,desc')
  .then(r => r.json())
```
El id corto del tablero sale de la URL (`/b/<id>/...`). Para los nombres de los miembros: `/1/boards/<id>/members?fields=fullName,username`.

## Escribir (pide permiso antes)
Editar una tarjeta es un cambio visible para el equipo. Muestra al usuario el texto exacto y espera su sí.
```js
// el cliente web de Trello manda el token "dsc" que está en la cookie del mismo nombre
const dsc = document.cookie.split('; ').find(c => c.startsWith('dsc='))?.slice(4);
const r = await fetch('/1/cards/<idTarjeta>', {
  method: 'PUT',
  headers: {'Content-Type': 'application/x-www-form-urlencoded'},
  body: new URLSearchParams({desc: '<texto nuevo>', dsc})
});
r.ok   // devuelve solo esto, no el cuerpo ni la cookie
```
Mover de lista: mismo `PUT` con `idList`. Fecha: `due`. Esto funcionó el 9 de noviembre de 2026; si Trello cambia el cliente, el campo `dsc` puede cambiar (por verificar).

## Trampas
- Si el resultado de `javascript_tool` contiene algo parecido a una cookie o a una cadena de consulta, la herramienta **bloquea la salida**. Devuelve booleanos, contadores o JSON corto (solo `name`, `id`, `ok`).
- Antes de cambiar una descripción, léela y **conserva lo que ya tiene**; agrega al final (por ejemplo, "Criterios de aceptación").
- Después de escribir, vuelve a leer la tarjeta y confirma el cambio.
- Cuando compares el tablero con GitHub, usa la fecha de la lectura: el tablero cambia a diario.

## Reglas con el usuario
- Cada persona mueve **sus** tarjetas. No muevas las de otros, ni las del usuario, salvo que lo pida en ese momento (a veces prefiere moverlas él para que el equipo vea el avance).
- Agrega criterios a una tarjeta solo si el usuario lo pide, y di en el chat qué agregaste.
