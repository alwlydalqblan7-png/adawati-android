package com.taifdigital.adawati

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { AdawatiApp() } }
}
private val Navy = Color(0xFF1C3F66)

@Composable
fun AdawatiApp(model: ToolsModel = viewModel()) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = lightColorScheme(primary = Navy, secondary = Color(0xFF3BB273), background = Color(0xFFF5F7FA))) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BackHandler(model.screen != "home" || model.busy) { if (!model.busy) model.navigate("home") }
                    if (model.screen != "home") TextButton(onClick = { model.navigate("home") }, enabled = !model.busy) { Text("رجوع إلى الأدوات") }
                    if (model.busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(if (model.progress > 0) "تمت معالجة ${model.progress} صفحة…" else "جارٍ العمل…") }
                    when (model.screen) {
                        "home" -> Home(model)
                        "scan", "pdf" -> PdfScreen(model)
                        "compress" -> CompressScreen(model)
                        "qr" -> QrScreen(model)
                        "files" -> FilesScreen(model)
                        "about" -> AboutScreen()
                    }
                    if (model.message.isNotBlank()) Text(model.message)
                }
            }
        }
    }
}
@Composable private fun Heading(title: String, description: String) { Text(title, style = MaterialTheme.typography.headlineMedium, color = Navy); Text(description) }
@Composable private fun Home(model: ToolsModel) {
    Heading("أدواتي", "أدوات يومية عربية تعمل على هاتفك")
    val tools = listOf(
        Triple("scan", "مسح المستندات", Icons.Outlined.DocumentScanner),
        Triple("pdf", "الصور إلى PDF", Icons.Outlined.PictureAsPdf),
        Triple("compress", "ضغط الصور", Icons.Outlined.Compress),
        Triple("qr", "أدوات QR", Icons.Outlined.QrCode2),
        Triple("files", "ملفاتي", Icons.Outlined.Folder),
        Triple("about", "المساعدة والخصوصية", Icons.Outlined.Security)
    )
    tools.forEach { (id, title, icon) ->
        ElevatedCard(
            onClick = { model.navigate(id) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Navy.copy(alpha = 0.10f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(28.dp))
                    }
                }
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            }
        }
    }
    Text("Taif Digital • ${BuildConfig.VERSION_NAME}", color = Navy)
}

@Composable private fun PdfScreen(model: ToolsModel) {
    val context = LocalContext.current
    var mode by rememberSaveable { mutableStateOf("natural") }
    var cameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    var editor by remember { mutableStateOf<Int?>(null) }
    var clear by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { model.import(it) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        cameraPath?.let { path ->
            val file = File(path)
            if (ok && file.exists()) {
                if (model.pages.size < DocumentEngine.MAX_PAGES) { model.pages.add(file); model.output = null; model.persist() }
                else { file.delete(); model.message = "الحد الأقصى 30 صفحة" }
            } else { file.delete(); model.message = "تم إلغاء التصوير" }
        }
        cameraPath = null
    }
    val launchCamera = {
        try {
            val file = DocumentEngine.newFile(context, "drafts", "jpg")
            cameraPath = file.path
            camera.launch(FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file))
        } catch (_: Exception) { cameraPath?.let { File(it).delete() }; cameraPath = null; model.message = "تعذر فتح الكاميرا. يمكنك اختيار صور من الهاتف." }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else model.message = "لم يُسمح بالكاميرا. فعّل الإذن من إعدادات التطبيق أو اختر صورًا من الهاتف."
    }
    Heading(if (model.screen == "scan") "مسح المستندات" else "الصور إلى PDF", "أضف الصفحات ورتّبها وقصّها قبل الحفظ. حتى 30 صفحة. المسودة مشتركة بين الأداتين.")
    if (model.screen == "scan") Button(enabled = !model.busy && model.pages.size < 30, onClick = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchCamera() else permission.launch(Manifest.permission.CAMERA)
    }, modifier = Modifier.fillMaxWidth()) { Text("تصوير صفحة") }
    OutlinedButton(enabled = !model.busy, onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text("إضافة صور من الهاتف") }
    model.pages.toList().forEachIndexed { i, file ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Thumbnail(file, Modifier.size(64.dp)); Text("الصفحة ${i+1}", Modifier.padding(12.dp)) }
                Row { TextButton(enabled = !model.busy, onClick = { editor = i }) { Text("قص وتدوير") }; TextButton(enabled = !model.busy, onClick = { model.remove(i) }) { Text("حذف") } }
                Row { TextButton(enabled = !model.busy && i > 0, onClick = { model.move(i,-1) }) { Text("تقديم") }; TextButton(enabled = !model.busy && i < model.pages.lastIndex, onClick = { model.move(i,1) }) { Text("تأخير") } }
            }
        }
    }
    if (model.pages.isNotEmpty()) {
        if (model.screen == "scan") {
            Text("نمط المسح")
            listOf("natural" to "طبيعي", "text" to "تحسين النص", "bw" to "أبيض وأسود").forEach { (id, name) ->
                FilterChip(selected = mode == id, enabled = !model.busy, onClick = { mode = id; model.output = null }, label = { Text(name) })
            }
        }
        Button(enabled = !model.busy, onClick = { model.makePdf(if (model.screen == "scan") mode else "natural") }, modifier = Modifier.fillMaxWidth()) { Text("إنشاء PDF وحفظه في ملفاتي") }
        TextButton(enabled = !model.busy, onClick = { clear = true }) { Text("بدء مسودة جديدة") }
    }
    model.output?.let { FileActions(it, model) }
    if (clear) AlertDialog(onDismissRequest = { clear = false }, title = { Text("حذف المسودة؟") }, text = { Text("لن تُحذف ملفات PDF التي أنشأتها.") }, confirmButton = { TextButton(onClick = { model.clearDraft(); clear = false }) { Text("حذف المسودة") } }, dismissButton = { TextButton(onClick = { clear = false }) { Text("إلغاء") } })
    editor?.let { i -> if (i in model.pages.indices) EditDialog(model.pages[i], model.busy, { editor = null }) { rotation, l, t, r, b -> model.edit(i, rotation,l,t,r,b) { editor = null } } }
}

@Composable private fun CompressScreen(model: ToolsModel) {
    var quality by rememberSaveable { mutableFloatStateOf(75f) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) model.import(listOf(it), true) }
    Heading("ضغط الصور", "تصدير JPEG بخلفية بيضاء. الضغط قد يقلل التفاصيل؛ الصورة الأصلية تبقى دون تغيير.")
    Button(enabled = !model.busy, onClick = { picker.launch("image/*") }) { Text("اختيار صورة") }
    model.selected?.let { file ->
        Thumbnail(file, Modifier.fillMaxWidth().height(180.dp))
        Text("الحجم الأصلي: ${file.length()/1024} كيلوبايت")
        Text("الجودة: ${quality.toInt()}٪")
        Slider(value = quality, enabled = !model.busy, onValueChange = { quality = it; model.output = null }, valueRange = 30f..95f)
        Button(enabled = !model.busy, onClick = { model.compress(quality.toInt()) }) { Text("ضغط وحفظ") }
    }
    model.output?.let { FileActions(it, model) }
}

@Composable private fun QrScreen(model: ToolsModel) {
    val context = LocalContext.current
    val scan = rememberLauncherForActivityResult(com.journeyapps.barcodescanner.ScanContract()) {
        if (it.contents != null) { model.qrText = it.contents; model.output = null; model.persist(); model.message = "تمت قراءة الرمز. راجع الرابط قبل فتحه." }
        else model.message = "لم تتم قراءة رمز. تحقق من إذن الكاميرا وحاول مجددًا."
    }
    Heading("أدوات QR", "إنشاء رموز للنص العربي والروابط، أو قراءة رمز بالكاميرا.")
    Button(enabled = !model.busy, onClick = {
        try { scan.launch(com.journeyapps.barcodescanner.ScanOptions().setDesiredBarcodeFormats(com.journeyapps.barcodescanner.ScanOptions.QR_CODE).setPrompt("وجّه الكاميرا نحو الرمز").setBeepEnabled(false)) }
        catch (_: Exception) { model.message = "تعذر تشغيل قارئ الكاميرا" }
    }) { Text("قراءة بالكاميرا") }
    OutlinedTextField(value = model.qrText, enabled = !model.busy, onValueChange = { model.qrText = it; model.output = null; model.persist() }, label = { Text("الرابط أو النص") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 8)
    TextButton(enabled = model.qrText.isNotEmpty(), onClick = { (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("QR", model.qrText)); model.message = "تم النسخ" }) { Text("نسخ النص") }
    Button(enabled = !model.busy && model.qrText.isNotBlank(), onClick = { model.makeQr() }) { Text("إنشاء وحفظ QR") }
    model.output?.let { Thumbnail(it, Modifier.fillMaxWidth().height(240.dp)); FileActions(it, model) }
}

@Composable private fun FilesScreen(model: ToolsModel) {
    var deleting by remember { mutableStateOf<File?>(null) }
    Heading("ملفاتي", "الملفات محفوظة داخل التطبيق. احفظ نسخة خارجية قبل إلغاء تثبيته أو مسح بياناته.")
    if (model.files.isEmpty()) Text("لا توجد ملفات بعد")
    model.files.forEach { file ->
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
            Text(file.name); Text("${file.length()/1024} كيلوبايت")
            FileActions(file, model)
            TextButton(enabled = !model.busy, onClick = { deleting = file }) { Text("حذف") }
        } }
    }
    deleting?.let { file -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("حذف الملف؟") }, text = { Text(file.name) }, confirmButton = { TextButton(onClick = { if (!file.delete()) model.message = "تعذر حذف الملف"; model.refresh(); deleting = null }) { Text("حذف") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("إلغاء") } }) }
}

@Composable private fun FileActions(file: File, model: ToolsModel) {
    val context = LocalContext.current
    val mime = when (file.extension) { "pdf" -> "application/pdf"; "png" -> "image/png"; else -> "image/jpeg" }
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mime)) { uri ->
        val source = pendingPath?.let(::File)
        if (uri != null && source != null) model.task {
            withContext(Dispatchers.IO) {
                require(source.exists()) { "الملف غير موجود" }
                context.contentResolver.openOutputStream(uri, "wt").use { stream ->
                    requireNotNull(stream) { "تعذر فتح مكان الحفظ" }; source.inputStream().use { it.copyTo(stream) }
                }
            }
            model.message = "تم حفظ النسخة في المكان الذي اخترته"
        }
        pendingPath = null
    }
    OutlinedButton(enabled = !model.busy, onClick = { pendingPath = file.path; save.launch(file.name) }, modifier = Modifier.fillMaxWidth()) { Text("حفظ نسخة باسم…") }
    Row {
        TextButton(enabled = !model.busy, onClick = { openFile(context, file, mime, false) { model.message = it } }) { Text("فتح") }
        TextButton(enabled = !model.busy, onClick = { openFile(context, file, mime, true) { model.message = it } }) { Text("مشاركة") }
    }
}
private fun openFile(context: Context, file: File, mime: String, share: Boolean, error: (String) -> Unit) {
    try {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(if (share) Intent.ACTION_SEND else Intent.ACTION_VIEW).apply {
            if (share) { type = mime; putExtra(Intent.EXTRA_STREAM, uri) } else setDataAndType(uri, mime)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(if (share) Intent.createChooser(intent, "مشاركة الملف") else intent)
    } catch (_: Exception) { error("تعذر فتح الملف. ثبّت تطبيقًا مناسبًا أو استخدم حفظ نسخة باسم.") }
}

@Composable private fun Thumbnail(file: File, modifier: Modifier) {
    val bitmap by produceState<Bitmap?>(null, file.path) {
        value = withContext(Dispatchers.IO) { runCatching { DocumentEngine.decode(file, 500) }.getOrNull() }
    }
    bitmap?.let { Image(it.asImageBitmap(), "معاينة الصورة", modifier) }
}

@Composable private fun EditDialog(file: File, busy: Boolean, dismiss: () -> Unit, save: (Int, Float, Float, Float, Float) -> Unit) {
    var horizontal by remember { mutableStateOf(0f..1f) }
    var vertical by remember { mutableStateOf(0f..1f) }
    var rotation by remember { mutableIntStateOf(0) }
    val bitmap by produceState<Bitmap?>(null, file.path) { value = withContext(Dispatchers.IO) { runCatching { DocumentEngine.decode(file, 600) }.getOrNull() } }
    val preview = remember(bitmap, horizontal, vertical, rotation) {
        bitmap?.let { source ->
            val x = (source.width * horizontal.start).toInt().coerceIn(0, source.width-1)
            val y = (source.height * vertical.start).toInt().coerceIn(0, source.height-1)
            val w = (source.width * (horizontal.endInclusive-horizontal.start)).toInt().coerceIn(1, source.width-x)
            val h = (source.height * (vertical.endInclusive-vertical.start)).toInt().coerceIn(1, source.height-y)
            Bitmap.createBitmap(source,x,y,w,h,Matrix().apply { postRotate(rotation.toFloat()) },true)
        }
    }
    AlertDialog(onDismissRequest = { if (!busy) dismiss() }, title = { Text("قص وتدوير الصفحة") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            preview?.let { image ->
                Box(Modifier.fillMaxWidth().height(200.dp)) {
                    Image(image.asImageBitmap(), "معاينة نتيجة القص والتدوير", Modifier.fillMaxSize())
                }
            }
            Text("حدود القص كنسبة من الصورة الأصلية؛ التدوير يُطبّق بعد القص.")
            Text("من اليسار ${ (horizontal.start*100).toInt()}٪ إلى ${(horizontal.endInclusive*100).toInt()}٪")
            RangeSlider(value = horizontal, enabled = !busy, onValueChange = { if (it.endInclusive-it.start >= .1f) horizontal = it })
            Text("من الأعلى ${(vertical.start*100).toInt()}٪ إلى ${(vertical.endInclusive*100).toInt()}٪")
            RangeSlider(value = vertical, enabled = !busy, onValueChange = { if (it.endInclusive-it.start >= .1f) vertical = it })
            TextButton(enabled = !busy, onClick = { rotation = (rotation+90)%360 }) { Text("تدوير: $rotation درجة") }
            Text("المعاينة تعرض النتيجة؛ لن تتغير الصورة الأصلية في هاتفك.")
        }
    }, confirmButton = { TextButton(enabled = !busy && bitmap != null, onClick = { save(rotation, horizontal.start, vertical.start, horizontal.endInclusive, vertical.endInclusive) }) { Text("تطبيق") } }, dismissButton = { TextButton(enabled = !busy, onClick = dismiss) { Text("إلغاء") } })
}

@Composable private fun AboutScreen() {
    val context = LocalContext.current
    Heading("أدواتي • Taif Digital", "الإصدار ${BuildConfig.VERSION_NAME}")
    Text("ابدأ باختيار أداة، ثم صورة أو صفحات. الملفات الناتجة تُحفظ في «ملفاتي». استخدم «حفظ نسخة باسم» لحفظها خارج التطبيق، أو «مشاركة» لإرسالها.")
    Text("الخصوصية", style = MaterialTheme.typography.titleLarge)
    Text("تتم معالجة الصور والمستندات محليًا. هذا الإصدار لا يتصل بالإنترنت ولا يحتوي إعلانات أو أدوات تتبع أو حسابات مستخدمين. نطلب إذن الكاميرا عند استخدامها فقط. الصور التي تختارها تُنسخ إلى مساحة التطبيق للاحتفاظ بالمسودة.")
    Text("تبقى الملفات والمسودات داخل التطبيق حتى حذفها أو مسح بيانات التطبيق أو إلغاء تثبيته. النسخ التي تحفظها خارجه تخضع لإدارة المكان الذي تختاره. عند المشاركة تتعامل التطبيقات المستقبلة مع الملف وفق سياساتها. النسخ الاحتياطي التلقائي لبيانات التطبيق معطّل.")
    Text("لا توجد مشتريات أو اشتراكات في هذا الإصدار. قص الصفحات يدوي؛ لا يتضمن التعرف الضوئي على النصوص أو تصحيح المنظور التلقائي.")
    OutlinedButton(onClick = {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/alwlydalqblan7-png/adawati-android/issues"))) }
        catch (_: Exception) { }
    }) { Text("الدعم والإبلاغ عن مشكلة") }
    Text("لا ترفق صور مستندات شخصية في البلاغات العامة.")
}
