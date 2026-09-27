package com.taifdigital.adawati

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AdawatiApp() }
    }
}

private val Navy = Color(0xFF1C3F66)
private val Sky = Color(0xFF4FB7FF)
private val Green = Color(0xFF3BB273)
private val Bg = Color(0xFFF5F7FA)

data class ToolItem(val icon: String, val title: String, val subtitle: String, val accent: Color)

@Composable
fun AdawatiApp() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = lightColorScheme(primary = Navy, secondary = Sky, tertiary = Green, background = Bg)) {
            HomeScreen()
        }
    }
}

@Composable
fun HomeScreen() {
    val tools = listOf(
        ToolItem("📄", "مسح مستند", "صوّر أوراقك وحوّلها إلى PDF", Navy),
        ToolItem("📑", "صورة إلى PDF", "اجمع صورك في ملف PDF", Sky),
        ToolItem("▦", "أدوات QR", "قراءة وإنشاء رموز QR", Green),
        ToolItem("🖼️", "ضغط الصور", "قلّل الحجم مع الحفاظ على الجودة", Navy)
    )
    Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 28.dp)) {
            Text("أدواتي", fontSize = 32.sp, color = Navy)
            Spacer(Modifier.height(4.dp))
            Text("كل أدواتك... بمكان واحد", fontSize = 16.sp, color = Color.DarkGray)
            Spacer(Modifier.height(26.dp))
            tools.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { tool -> ToolCard(tool, Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.weight(1f))
            Text("Taif Digital", modifier = Modifier.align(Alignment.CenterHorizontally), color = Navy, fontSize = 13.sp)
        }
    }
}

@Composable
fun ToolCard(tool: ToolItem, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(158.dp), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
            Text(tool.icon, fontSize = 30.sp)
            Spacer(Modifier.height(10.dp))
            Text(tool.title, fontSize = 19.sp, color = tool.accent)
            Spacer(Modifier.height(4.dp))
            Text(tool.subtitle, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
