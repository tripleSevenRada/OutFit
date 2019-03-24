package radim.outfit.core.persistence

import radim.outfit.ViewResultsParcel

const val staticId = 1

//TODO

fun ViewResultsParcel.getEntity() = ViewResultsParcelEntity(
        staticId,
        this.title,
        this.messages,
        this.buffer.toList())

fun ViewResultsParcelEntity.getParcel() = ViewResultsParcel(
        this.title,
        this.messages,
        this.buffer.toTypedArray(),
        mapOf(Pair("",""))
        )
