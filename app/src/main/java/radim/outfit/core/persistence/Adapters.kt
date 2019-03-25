package radim.outfit.core.persistence

import radim.outfit.ViewResultsParcel

const val staticId = 1

fun ViewResultsParcel.getEntity() = ViewResultsParcelEntity(
        staticId,
        this.title,
        this.messages,
        this.buffer.toList(),
        this.fileNameToCourseName)

fun ViewResultsParcelEntity.getParcel() = ViewResultsParcel(
        this.title,
        this.messages,
        this.buffer.toTypedArray(),
        this.filenameToCoursename
        )
