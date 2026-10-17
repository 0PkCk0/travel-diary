package app.traveldiary.data.cities

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cities")
data class CityEntity(
    /** GeoNames id. */
    @PrimaryKey val id: Long,
    val name: String,
    /** State, region or province, when known. */
    val region: String?,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val population: Long,
    /** The name without accents or case, see [SearchNormalizer]: "Zürich" -> "zurich". */
    val searchName: String,
    /** GeoNames' own ASCII spelling when it differs: "Zürich" -> "zuerich". */
    val searchAscii: String? = null,
)

/** Which city list is installed (always a single row). */
@Entity(tableName = "dataset")
data class DatasetEntity(
    @PrimaryKey val id: Int = 0,
    val datasetId: String,
    val cityCount: Int,
    val importedAt: Long,
)

@Dao
interface CityDao {

    /**
     * Cities whose name starts with the query or has a word starting with it.
     * Exact matches come first, then names starting with the query, then the most populated.
     */
    // CASE rather than boolean ORDER BY terms: a NULL searchAscii must count as "no match",
    // not sort after every non-match.
    @Query(
        """
        SELECT * FROM cities
        WHERE searchName LIKE :prefix OR searchName LIKE :wordPrefix
           OR searchAscii LIKE :prefix OR searchAscii LIKE :wordPrefix
        ORDER BY CASE
                     WHEN searchName = :query OR searchAscii = :query THEN 0
                     WHEN searchName LIKE :prefix OR searchAscii LIKE :prefix THEN 1
                     ELSE 2
                 END,
                 population DESC
        LIMIT :limit
        """,
    )
    suspend fun search(query: String, prefix: String, wordPrefix: String, limit: Int): List<CityEntity>

    @Query(
        """
        SELECT * FROM cities
        WHERE countryCode = :countryCode
          AND (searchName LIKE :prefix OR searchName LIKE :wordPrefix
               OR searchAscii LIKE :prefix OR searchAscii LIKE :wordPrefix)
        ORDER BY CASE
                     WHEN searchName = :query OR searchAscii = :query THEN 0
                     WHEN searchName LIKE :prefix OR searchAscii LIKE :prefix THEN 1
                     ELSE 2
                 END,
                 population DESC
        LIMIT :limit
        """,
    )
    suspend fun searchInCountry(
        query: String,
        prefix: String,
        wordPrefix: String,
        countryCode: String,
        limit: Int,
    ): List<CityEntity>

    @Query("SELECT * FROM cities WHERE countryCode = :countryCode ORDER BY population DESC LIMIT :limit")
    suspend fun largestInCountry(countryCode: String, limit: Int): List<CityEntity>

    @Query("SELECT * FROM cities WHERE id = :id")
    suspend fun getCity(id: Long): CityEntity?

    @Query("SELECT COUNT(*) FROM cities")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cities: List<CityEntity>)

    @Query("DELETE FROM cities")
    suspend fun deleteAll()

    @Query("SELECT * FROM dataset WHERE id = 0")
    fun observeDataset(): Flow<DatasetEntity?>

    @Query("SELECT * FROM dataset WHERE id = 0")
    suspend fun getDataset(): DatasetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setDataset(dataset: DatasetEntity)
}

/** Reference data only: it can always be rebuilt, so schema changes simply recreate it. */
@Database(entities = [CityEntity::class, DatasetEntity::class], version = 1, exportSchema = false)
abstract class CityDatabase : RoomDatabase() {
    abstract fun cityDao(): CityDao

    companion object {
        fun create(context: Context): CityDatabase =
            Room.databaseBuilder(context, CityDatabase::class.java, "cities.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
