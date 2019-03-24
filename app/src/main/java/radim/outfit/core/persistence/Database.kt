package radim.outfit.core.persistence

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters

@Database(entities = arrayOf(ViewResultsParcelEntity::class), version = 1)
@TypeConverters(Converters::class)
abstract class ParcelDatabase : RoomDatabase() {
    abstract fun parcelDao(): ViewResultsParcelEntityDAO
}
