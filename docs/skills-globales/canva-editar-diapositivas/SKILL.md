---
name: canva-editar-diapositivas
description: Revisar y editar una presentación de Canva con el conector de Canva (leer diapositivas, cambiar textos, insertar imágenes como código coloreado). Úsala cuando el usuario comparta un enlace de Canva y pida evaluar, corregir o mejorar diapositivas.
---

# Editar diapositivas de Canva

## Flujo
1. **Leer primero** con `read-design`: guarda los ids de página y de elementos. El usuario suele editar a mano entre pedidos; vuelve a leer antes de cada edición (una vez se intercambiaron etiquetas porque el diseño había cambiado).
2. **Evaluar** contra lo que el usuario necesita: completitud, orden, texto demasiado largo, ejemplos que no corresponden al tema de la materia. Entrega la lista corta y espera su sí antes de tocar nada.
3. **Editar** con `edit-design`. Es una transacción: inicia, aplica los cambios, **muestra la vista previa**, y haz `commit` solo cuando el usuario apruebe. Si no aprueba, cancela.
4. Después de confirmar, vuelve a leer la diapositiva y comprueba que el cambio quedó.

## Límites que se aprendieron
- No se puede pintar palabra por palabra dentro de un cuadro de texto. Para **código con colores** se genera una imagen:
  1. Arma un HTML con el código y los colores (fuente monoespaciada).
  2. Captura con Chrome sin cabeza al doble de escala (`--force-device-scale-factor=2` o dobla los px del CSS). Si el borde sale recortado, aumenta el margen.
  3. Recorta con Python al cuadro delimitador de los píxeles no vacíos (el `sips` desplazó el recorte).
  4. Sube el PNG con `create-upload-url` y colócalo en la diapositiva.
- Las diapositivas son apoyo de una exposición oral: pocas líneas, términos exactos y un solo ejemplo coherente con el tema (nada de ejemplos de otro proyecto si no vienen al caso).
- No guardes en archivos ni en el chat enlaces privados del usuario si no hace falta.
