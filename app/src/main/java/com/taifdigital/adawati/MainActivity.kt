package com.taifdigital.adawati

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { AdawatiApp() } }
}
private val Navy=Color(0xFF1C3F66); private val Sky=Color(0xFF4FB7FF); private val Green=Color(0xFF3BB273); private val Bg=Color(0xFFF5F7FA)
data class ToolItem(val id:String,val icon:String,val title:String,val subtitle:String,val accent:Color)

@Composable fun AdawatiApp(){
 var active by remember{mutableStateOf<ToolItem?>(null)}
 CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl){
  MaterialTheme(colorScheme=lightColorScheme(primary=Navy,secondary=Sky,tertiary=Green,background=Bg)){
   if(active==null) HomeScreen{active=it} else if(active!!.id=="pdf") ImageToPdfScreen{active=null} else Placeholder(active!!){active=null}
  }
 }
}
@Composable fun HomeScreen(open:(ToolItem)->Unit){
 val tools=listOf(ToolItem("scan","📄","مسح مستند","صوّر أوراقك وحوّلها إلى PDF",Navy),ToolItem("pdf","📑","صورة إلى PDF","اجمع صورك في ملف PDF",Sky),ToolItem("qr","▦","أدوات QR","قراءة وإنشاء رموز QR",Green),ToolItem("compress","🖼️","ضغط الصور","قلّل الحجم مع الحفاظ على الجودة",Navy))
 Surface(Modifier.fillMaxSize(),color=Bg){Column(Modifier.fillMaxSize().padding(20.dp)){Text("أدواتي",fontSize=32.sp,color=Navy);Text("كل أدواتك... بمكان واحد",color=Color.DarkGray);Spacer(Modifier.height(26.dp));tools.chunked(2).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){row.forEach{t->Card(onClick={open(t)},modifier=Modifier.weight(1f).height(158.dp),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.Center){Text(t.icon,fontSize=30.sp);Text(t.title,fontSize=19.sp,color=t.accent);Text(t.subtitle,fontSize=12.sp,color=Color.Gray)}}};};Spacer(Modifier.height(12.dp))};Spacer(Modifier.weight(1f));Text("Taif Digital",Modifier.align(Alignment.CenterHorizontally),color=Navy)}}
}
@Composable fun ImageToPdfScreen(back:()->Unit){
 BackHandler{back()};val context=LocalContext.current;var selected by remember{mutableStateOf<List<Uri>>(emptyList())};var message by remember{mutableStateOf("")};var output by remember{mutableStateOf<File?>(null)}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()){selected=it;message="";output=null}
 Surface(Modifier.fillMaxSize(),color=Bg){Column(Modifier.fillMaxSize().padding(20.dp)){TextButton(onClick=back){Text("← رجوع")};Text("📑 صورة إلى PDF",fontSize=28.sp,color=Sky);Text("اختر صورة أو عدة صور ثم أنشئ ملف PDF.",color=Color.DarkGray);Spacer(Modifier.height(24.dp));Button(onClick={picker.launch("image/*")},modifier=Modifier.fillMaxWidth()){Text("اختيار الصور")};if(selected.isNotEmpty()){Spacer(Modifier.height(12.dp));Text("تم اختيار "+selected.size+" صورة",color=Green);Spacer(Modifier.height(12.dp));Button(onClick={try{output=createPdf(context,selected);message="تم إنشاء PDF بنجاح ✅"}catch(e:Exception){message="تعذر إنشاء الملف: "+(e.message?:"خطأ غير معروف")}},modifier=Modifier.fillMaxWidth()){Text("إنشاء PDF")}};if(message.isNotEmpty()){Spacer(Modifier.height(16.dp));Text(message,color=if(output!=null)Green else Color.Red)};output?.let{file->Spacer(Modifier.height(12.dp));OutlinedButton(onClick={sharePdf(context,file)},modifier=Modifier.fillMaxWidth()){Text("مشاركة ملف PDF")}}}}
}
fun createPdf(context:Context,uris:List<Uri>):File{
 val pdf=PdfDocument()
 try{uris.forEachIndexed{index,uri->val bitmap=context.contentResolver.openInputStream(uri).use{BitmapFactory.decodeStream(it)}?:throw IllegalArgumentException("تعذر قراءة إحدى الصور");val pw=1240;val ph=1754;val page=pdf.startPage(PdfDocument.PageInfo.Builder(pw,ph,index+1).create());val scale=minOf(pw.toFloat()/bitmap.width,ph.toFloat()/bitmap.height);val w=(bitmap.width*scale).toInt();val h=(bitmap.height*scale).toInt();val l=(pw-w)/2f;val t=(ph-h)/2f;page.canvas.drawBitmap(bitmap,null,android.graphics.RectF(l,t,l+w,t+h),null);pdf.finishPage(page);bitmap.recycle()};val dir=File(context.cacheDir,"pdfs").apply{mkdirs()};val out=File(dir,"Adawati-"+System.currentTimeMillis()+".pdf");out.outputStream().use{pdf.writeTo(it)};return out}finally{pdf.close()}
}
fun sharePdf(context:Context,file:File){val uri=FileProvider.getUriForFile(context,context.packageName+".fileprovider",file);val intent=Intent(Intent.ACTION_SEND).apply{type="application/pdf";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)};context.startActivity(Intent.createChooser(intent,"مشاركة PDF"))}
@Composable fun Placeholder(tool:ToolItem,back:()->Unit){BackHandler{back()};Surface(Modifier.fillMaxSize(),color=Bg){Column(Modifier.fillMaxSize().padding(20.dp)){TextButton(onClick=back){Text("← رجوع")};Text(tool.icon+" "+tool.title,fontSize=28.sp,color=tool.accent);Spacer(Modifier.height(16.dp));Text("هذه الأداة قيد التفعيل.",color=Color.DarkGray)}}}
