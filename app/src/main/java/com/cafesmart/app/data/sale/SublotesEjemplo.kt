package com.cafesmart.app.data.sale

import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.sample.lotesEjemplo

// Sublotes con cafe para vender: los del inventario de ejemplo, sin los agotados.
// Temporal: se reemplazan por los de Room cuando se incorpore la base de datos.
fun sublotesParaVenderEjemplo(): List<SubloteConLote> =
    lotesEjemplo().flatMap { lote -> lote.sublotesConExistencia.map { SubloteConLote(it, lote.lote) } }
