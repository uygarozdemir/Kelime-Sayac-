package com.example

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.zip.ZipInputStream
import org.json.JSONArray
import org.json.JSONObject

data class DocumentStats(
    val fileName: String,
    val fileSizeFormatted: String,
    val fileType: String, // PDF, DOCX, TXT
    val wordCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val stats: DocumentStats) : UiState
    data class Error(val message: String) : UiState
}

/**
 * Saf metin istatistik yardımcıları. Android bağımlılığı yoktur, bu sayede
 * birim testlerinde (host JVM) doğrudan çağrılabilir.
 */
object TextStats {
    // Satır sonlarındaki heceleme tirelerini birleştirir (örn. "kelime-\nsayacı" -> "kelimesayacı").
    private val hyphenationRegex = Regex("(?<=\\p{L})-[ \t]*\r?\n[ \t]*(?=\\p{L})")
    private val whitespaceRegex = Regex("[\\s\\p{Z}]+")

    fun countWords(text: String): Int {
        val cleanedText = text.replace(hyphenationRegex, "")
        if (cleanedText.isBlank()) return 0
        return cleanedText.trim()
            .split(whitespaceRegex)
            .count { token -> token.any { it.isLetterOrDigit() } }
    }
}

class WordCounterViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _history = MutableStateFlow<List<DocumentStats>>(emptyList())
    val history: StateFlow<List<DocumentStats>> = _history.asStateFlow()

    private val sharedPrefs = application.getSharedPreferences("word_counter_prefs", Context.MODE_PRIVATE)

    init {
        try {
            PDFBoxResourceLoader.init(getApplication())
            Log.d("WordCounterViewModel", "PDFBoxResourceLoader init successful!")
        } catch (e: Throwable) {
            Log.e("WordCounterViewModel", "PDFBoxResourceLoader init failed", e)
        }
        loadHistory()
    }

    fun analyzeFile(uri: Uri) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val (fileName, fileSize) = queryFileMetadata(context, uri)
                val extension = getExtension(fileName)

                val text = withContext(Dispatchers.IO) {
                    when (extension.lowercase()) {
                        "pdf" -> extractTextFromPdf(context, uri)
                        "docx" -> extractTextFromDocx(context, uri)
                        "txt" -> extractTextFromTxt(context, uri)
                        else -> {
                            // Try parsing as text as a fallback
                            try {
                                extractTextFromTxt(context, uri)
                            } catch (e: Exception) {
                                throw IllegalArgumentException(
                                    context.getString(R.string.error_unsupported_format, extension)
                                )
                            }
                        }
                    }
                }

                if (text.trim().isEmpty() && extension.lowercase() == "pdf") {
                    throw IllegalStateException(context.getString(R.string.error_pdf_no_text))
                }

                val stats = calculateStats(fileName, fileSize, extension, text)
                _uiState.value = UiState.Success(stats)
                saveToHistory(stats)
            } catch (e: Throwable) {
                Log.e("WordCounter", "Error parsing file", e)
                _uiState.value = UiState.Error(
                    e.localizedMessage ?: e.message
                        ?: getApplication<Application>().getString(R.string.error_unexpected)
                )
            }
        }
    }

    /** Geçmişteki bir kaydı tekrar sonuç ekranında gösterir. */
    fun showHistoryItem(stats: DocumentStats) {
        _uiState.value = UiState.Success(stats)
    }

    private fun getExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "")
    }

    private fun queryFileMetadata(context: Context, uri: Uri): Pair<String, Long> {
        var name = context.getString(R.string.default_document_name)
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex)
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WordCounterViewModel", "Error querying metadata", e)
        }
        return Pair(name, size)
    }

    private fun extractTextFromPdf(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val memoryUsageSetting = MemoryUsageSetting.setupTempFileOnly()
            val document = PDDocument.load(inputStream, memoryUsageSetting)
            try {
                if (document.isEncrypted) {
                    throw Exception(context.getString(R.string.error_pdf_encrypted))
                }
                val stripper = PDFTextStripper()
                stripper.sortByPosition = true
                stripper.getText(document) ?: ""
            } finally {
                document.close()
            }
        } ?: throw Exception(context.getString(R.string.error_pdf_open))
    }

    private fun extractTextFromDocx(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val zipInputStream = ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry
            val sb = StringBuilder()
            var foundDocumentXml = false
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    foundDocumentXml = true
                    val factory = XmlPullParserFactory.newInstance()
                    factory.isNamespaceAware = true
                    val parser = factory.newPullParser()
                    parser.setInput(zipInputStream, "UTF-8")
                    var eventType = parser.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            when (parser.name) {
                                // Metin düğümü
                                "t" -> sb.append(parser.nextText()).append(" ")
                                // Sekme ve satır sonu kontrol düğümleri
                                "tab" -> sb.append("\t")
                                "br", "cr" -> sb.append("\n")
                            }
                        } else if (eventType == XmlPullParser.END_TAG && parser.name == "p") {
                            // Paragraf sonu
                            sb.append("\n")
                        }
                        eventType = parser.next()
                    }
                    break
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
            if (!foundDocumentXml) {
                throw Exception(context.getString(R.string.error_docx_invalid))
            }
            sb.toString()
        } ?: throw Exception(context.getString(R.string.error_docx_open))
    }

    private fun extractTextFromTxt(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } ?: throw Exception(context.getString(R.string.error_txt_open))
    }

    private fun calculateStats(fileName: String, fileSize: Long, extension: String, text: String): DocumentStats {
        return DocumentStats(
            fileName = fileName,
            fileSizeFormatted = formatFileSize(fileSize),
            fileType = extension.uppercase(),
            wordCount = TextStats.countWords(text)
        )
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    private fun saveToHistory(stats: DocumentStats) {
        val currentHistory = _history.value.toMutableList()
        currentHistory.removeAll { it.fileName == stats.fileName }
        currentHistory.add(0, stats)
        if (currentHistory.size > 20) {
            currentHistory.removeAt(currentHistory.lastIndex)
        }
        _history.value = currentHistory
        persistHistory(currentHistory)
    }

    fun deleteHistoryItem(stats: DocumentStats) {
        val currentHistory = _history.value.toMutableList()
        currentHistory.remove(stats)
        _history.value = currentHistory
        persistHistory(currentHistory)
    }

    fun clearHistory() {
        _history.value = emptyList()
        sharedPrefs.edit().remove("history_json").apply()
    }

    private fun persistHistory(list: List<DocumentStats>) {
        val jsonArray = JSONArray()
        for (item in list) {
            val json = JSONObject().apply {
                put("fileName", item.fileName)
                put("fileSizeFormatted", item.fileSizeFormatted)
                put("fileType", item.fileType)
                put("wordCount", item.wordCount)
                put("timestamp", item.timestamp)
            }
            jsonArray.put(json)
        }
        sharedPrefs.edit().putString("history_json", jsonArray.toString()).apply()
    }

    private fun loadHistory() {
        val jsonStr = sharedPrefs.getString("history_json", null)
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val jsonArray = JSONArray(jsonStr)
                val list = mutableListOf<DocumentStats>()
                for (i in 0 until jsonArray.length()) {
                    val json = jsonArray.getJSONObject(i)
                    list.add(
                        DocumentStats(
                            fileName = json.getString("fileName"),
                            fileSizeFormatted = json.getString("fileSizeFormatted"),
                            fileType = json.getString("fileType"),
                            wordCount = json.getInt("wordCount"),
                            timestamp = json.getLong("timestamp")
                        )
                    )
                }
                _history.value = list
            } catch (e: Exception) {
                Log.e("WordCounterViewModel", "Error deserializing history", e)
            }
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }
}
