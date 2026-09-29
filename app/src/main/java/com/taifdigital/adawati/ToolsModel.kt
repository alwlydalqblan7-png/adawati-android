package com.taifdigital.adawati

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import java.io.File

class ToolsModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("draft_state", 0)
    var screen by mutableStateOf(prefs.getString("screen", "home") ?: "home")
    val pages = mutableStateListOf<File>()
    var selected by mutableStateOf<File?>(null)
    var output by mutableStateOf<File?>(null)
    var busy by mutableStateOf(false)
    var message by mutableStateOf("")
    var progress by mutableIntStateOf(0)
    var files by mutableStateOf<List<File>>(emptyList())
    var qrText by mutableStateOf(prefs.getString("qr", "") ?: "")
    init {
        prefs.getString("pages", "")!!.split('|').filter { it.isNotBlank() }.map(::File).filter { it.exists() }.forEach { pages.add(it) }
        selected = prefs.getString("selected", null)?.let(::File)?.takeIf { it.exists() }
        output = prefs.getString("output", null)?.let(::File)?.takeIf { it.exists() }
        refresh()
    }
    fun persist() { prefs.edit().putString("pages", pages.joinToString("|") { it.path }).putString("selected", selected?.path).putString("output", output?.path).putString("screen", screen).putString("qr", qrText).apply() }
    fun navigate(value: String) { if (!busy) { screen = value; message = ""; output = null; persist() } }
    fun refresh() { files = DocumentEngine.directory(getApplication(), "exports").listFiles()?.filter { it.isFile && it.length() > 0 }?.sortedByDescending { it.lastModified() } ?: emptyList() }
    fun task(work: suspend () -> Unit) {
        if (busy) return
        busy = true; message = ""; progress = 0
        viewModelScope.launch {
            try { work() }
            catch (e: CancellationException) { throw e }
            catch (_: OutOfMemoryError) { message = "الذاكرة غير كافية. جرّب صفحات أقل." }
            catch (e: Exception) { message = e.message ?: "تعذرت العملية، حاول مجددًا" }
            finally { busy = false; refresh(); persist() }
        }
    }
    fun import(uris: List<Uri>, single: Boolean = false) {
        if (uris.isEmpty()) return
        task {
            if (!single) require(pages.size + uris.size <= DocumentEngine.MAX_PAGES) { "الحد الأقصى 30 صفحة لكل ملف" }
            val imported = withContext(Dispatchers.IO) {
                val result = mutableListOf<File>()
                try { uris.forEach { result.add(DocumentEngine.importImage(getApplication(), it)) }; result }
                catch (e: Exception) { result.forEach { it.delete() }; throw e }
            }
            if (single) { selected?.delete(); selected = imported.first() } else pages.addAll(imported)
            output = null; message = "تمت إضافة الصور"
        }
    }
    fun makePdf(mode: String) { task {
        val snapshot = pages.toList()
        output = withContext(Dispatchers.Default) { DocumentEngine.pdf(getApplication(), snapshot, mode) { n -> viewModelScope.launch { progress = n } } }
        message = "تم حفظ PDF في ملفاتي. يمكنك حفظ نسخة خارج التطبيق."
    } }
    fun compress(quality: Int) { val source = selected ?: return; task {
        output = null
        output = withContext(Dispatchers.Default) { DocumentEngine.compress(getApplication(), source, quality) }
        message = "قبل: ${source.length()/1024} كيلوبايت • بعد: ${output!!.length()/1024} كيلوبايت"
    } }
    fun makeQr() { val content = qrText; task {
        output = withContext(Dispatchers.Default) { DocumentEngine.saveQr(getApplication(), content) }
        message = "تم حفظ رمز QR في ملفاتي"
    } }
    fun edit(index: Int, rotation: Int, left: Float, top: Float, right: Float, bottom: Float, done: () -> Unit) { task {
        val old = pages[index]
        val edited = withContext(Dispatchers.Default) { DocumentEngine.edit(getApplication(), old, rotation, left, top, right, bottom) }
        pages[index] = edited; old.delete(); output = null; done()
    } }
    fun remove(index: Int) { if (!busy) { pages.removeAt(index).delete(); output = null; persist() } }
    fun move(index: Int, step: Int) { if (!busy && index + step in pages.indices) { val item = pages.removeAt(index); pages.add(index+step, item); output = null; persist() } }
    fun clearDraft() { if (!busy) { pages.forEach { it.delete() }; pages.clear(); output = null; persist() } }
}
