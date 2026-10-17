package app.traveldiary.data.cities

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.zip.ZipInputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

sealed interface CityImportState {
    data object Idle : CityImportState

    data class Downloading(val dataset: CityDataset, val bytesRead: Long, val totalBytes: Long) : CityImportState {
        val progress: Float get() = if (totalBytes <= 0) 0f else (bytesRead.toFloat() / totalBytes).coerceIn(0f, 1f)
    }

    data class Importing(val dataset: CityDataset, val citiesRead: Int) : CityImportState

    data class Failed(val dataset: CityDataset, val networkError: Boolean) : CityImportState
}

data class InstalledCities(val dataset: CityDataset?, val cityCount: Int, val importedAt: Long)

/**
 * Offline city search on top of a local copy of GeoNames. The built-in list is imported from the
 * APK assets on first launch; bigger lists are downloaded from GeoNames only when the user asks.
 */
class CityRepository(
    private val context: Context,
    private val database: CityDatabase,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val dao = database.cityDao()
    private var job: Job? = null

    private val _importState = MutableStateFlow<CityImportState>(CityImportState.Idle)
    val importState: StateFlow<CityImportState> = _importState.asStateFlow()

    /** The installed city list, or null until the first import has finished. */
    val installed: Flow<InstalledCities?> = dao.observeDataset().map { dataset ->
        dataset?.let { InstalledCities(CityDataset.fromId(it.datasetId), it.cityCount, it.importedAt) }
    }

    private val regionNames: Map<String, String> by lazy {
        context.assets.open(REGIONS_ASSET).bufferedReader().useLines(GeoNamesParser::parseRegionNames)
    }

    /** Imports the built-in list on first launch, or when an app update ships a newer one. */
    fun ensureBuiltInInstalled() = launchImport(CityDataset.BUILT_IN) {
        val current = dao.getDataset()?.datasetId
        val upToDate = current != null && (current == CityDataset.BUILT_IN.id || !current.startsWith(BUILT_IN_PREFIX))
        if (!upToDate) importBuiltIn()
    }

    /** Switches to [dataset], downloading it first if needed. Ignored while another import runs. */
    fun install(dataset: CityDataset) = launchImport(dataset) {
        if (dataset == CityDataset.BUILT_IN) importBuiltIn() else downloadAndImport(dataset)
    }

    fun cancel() {
        job?.cancel()
    }

    fun dismissError() {
        if (_importState.value is CityImportState.Failed) _importState.value = CityImportState.Idle
    }

    /**
     * Cities matching [query], optionally limited to one country. An empty query with a country
     * lists the country's largest cities.
     */
    suspend fun search(query: String, countryCode: String?, limit: Int = 50): List<CityEntity> {
        val normalized = SearchNormalizer.normalize(query)
        val prefix = "$normalized%"
        val wordPrefix = "% $normalized%"
        return when {
            normalized.isEmpty() && countryCode == null -> emptyList()
            normalized.isEmpty() -> dao.largestInCountry(countryCode!!, limit)
            countryCode == null -> dao.search(normalized, prefix, wordPrefix, limit)
            else -> dao.searchInCountry(normalized, prefix, wordPrefix, countryCode, limit)
        }
    }

    suspend fun getCity(id: Long): CityEntity? = dao.getCity(id)

    private fun launchImport(dataset: CityDataset, block: suspend () -> Unit) {
        if (job?.isActive == true) return
        job = scope.launch(ioDispatcher) {
            try {
                block()
                _importState.value = CityImportState.Idle
            } catch (e: CancellationException) {
                _importState.value = CityImportState.Idle
                throw e
            } catch (e: IOException) {
                Log.w(TAG, "Could not install ${dataset.id}", e)
                _importState.value = CityImportState.Failed(dataset, networkError = true)
            } catch (e: Exception) {
                Log.w(TAG, "Could not install ${dataset.id}", e)
                _importState.value = CityImportState.Failed(dataset, networkError = false)
            }
        }
    }

    internal suspend fun importBuiltIn() {
        readBuiltIn { lines -> importLines(CityDataset.BUILT_IN, lines) }
    }

    private suspend fun downloadAndImport(dataset: CityDataset) {
        val file = File(context.cacheDir, "${dataset.geonamesFile}.zip")
        try {
            download(dataset, file)
            ZipInputStream(file.inputStream().buffered()).use { zip ->
                val entryName = "${dataset.geonamesFile}.txt"
                while (true) {
                    val entry = zip.nextEntry ?: error("$entryName is missing from the download")
                    if (entry.name == entryName) break
                }
                val downloaded = zip.bufferedReader().lineSequence()
                // Keep every built-in place (capitals of tiny countries, ...) in the bigger list too.
                readBuiltIn { builtIn -> importLines(dataset, downloaded + builtIn) }
            }
        } finally {
            file.delete()
        }
    }

    private suspend fun download(dataset: CityDataset, target: File) {
        val connection = URI(checkNotNull(dataset.url)).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MILLIS
        connection.readTimeout = TIMEOUT_MILLIS
        try {
            val status = connection.responseCode
            if (status != HttpURLConnection.HTTP_OK) throw IOException("GeoNames replied $status")
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: dataset.downloadBytes
            _importState.value = CityImportState.Downloading(dataset, 0, total)
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        bytesRead += count
                        _importState.value = CityImportState.Downloading(dataset, bytesRead, total)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    /** Replaces the city table in one transaction, so a failed or cancelled import changes nothing. */
    private suspend fun importLines(dataset: CityDataset, lines: Sequence<String>) {
        _importState.value = CityImportState.Importing(dataset, 0)
        val parser = GeoNamesParser(regionNames)
        database.withTransaction {
            dao.deleteAll()
            var read = 0
            lines.mapNotNull(parser::parse).chunked(BATCH_SIZE).forEach { batch ->
                currentCoroutineContext().ensureActive()
                dao.insertAll(batch)
                read += batch.size
                _importState.value = CityImportState.Importing(dataset, read)
            }
            val count = dao.count()
            check(count > 0) { "No cities found in ${dataset.id}" }
            dao.setDataset(DatasetEntity(datasetId = dataset.id, cityCount = count, importedAt = System.currentTimeMillis()))
        }
    }

    private inline fun <T> readBuiltIn(block: (Sequence<String>) -> T): T =
        context.assets.open(BUILT_IN_ASSET).bufferedReader().use { block(it.lineSequence()) }

    private companion object {
        const val TAG = "CityRepository"
        const val BUILT_IN_ASSET = "data/cities.tsv"
        const val REGIONS_ASSET = "data/admin1.tsv"
        const val BUILT_IN_PREFIX = "builtin"
        const val BATCH_SIZE = 500
        const val TIMEOUT_MILLIS = 30_000
    }
}
