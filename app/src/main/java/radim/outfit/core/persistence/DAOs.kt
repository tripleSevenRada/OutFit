package radim.outfit.core.persistence

import android.arch.persistence.room.*

@Dao
interface ViewResultsParcelEntityDAO{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun persist(parcel: ViewResultsParcelEntity)

    @Query("SELECT * FROM parcel_entities_representing_state")
    fun retrieve(): List<ViewResultsParcelEntity>
}


