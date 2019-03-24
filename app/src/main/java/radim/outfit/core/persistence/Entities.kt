package radim.outfit.core.persistence

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.text.SpannableString

@Entity(tableName = "parcel_entities_representing_state")
data class ViewResultsParcelEntity(
        // (autoGenerate = true) not, I want to replace a single table at every insert
        @PrimaryKey val pid: Int,
        @ColumnInfo(name = "title") val title: String,
        @ColumnInfo(name = "messages") val messages: List<SpannableString>,
        @ColumnInfo(name = "buffer") val buffer: List<String>
        //@ColumnInfo(name = "filename_to_coursename") val filenameToCoursename: Map<String, String>
)