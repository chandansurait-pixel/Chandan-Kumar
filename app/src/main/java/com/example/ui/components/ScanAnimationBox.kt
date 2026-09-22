package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.EmeraldLight

@Composable
fun ScanAnimationBox(
    statusText: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F172A))
            .border(2.dp, Brush.linearGradient(listOf(CyanAccent, EmeraldLight)), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Laser Scan line drawing
        Canvas(modifier = Modifier.fillMaxSize()) {
            val yPos = size.height * laserY

            // Gradient scanner beam
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        CyanAccent.copy(alpha = 0.25f),
                        CyanGlow.copy(alpha = 0.7f),
                        Color.White.copy(alpha = 0.9f)
                    ),
                    startY = (yPos - 40f).coerceAtLeast(0f),
                    endY = yPos
                ),
                topLeft = Offset(0f, (yPos - 40f).coerceAtLeast(0f)),
                size = androidx.compose.ui.geometry.Size(size.width, 40f)
            )

            // Sharp laser horizontal line
            drawLine(
                color = CyanGlow,
                start = Offset(0f, yPos),
                end = Offset(size.width, yPos),
                strokeWidth = 4f
            )
        }

        // Center Content Status
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            CircularProgressIndicator(
                color = EmeraldLight,
                strokeWidth = 3.dp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "TaxSnap AI Gemini Vision",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = CyanGlow
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
        }
    }
}
