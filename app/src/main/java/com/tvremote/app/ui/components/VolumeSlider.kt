package com.tvremote.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvremote.app.ui.theme.RemoteColors
import kotlin.math.roundToInt

@Composable
fun VolumeSlider(
    percent: Int,
    onChange: (Int) -> Unit,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var widthPx by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepButton(label = "\u2212", onClick = { onStep(-5) })

        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(RemoteColors.BtnBg, RoundedCornerShape(2.dp))
                    .pointerInput(Unit) {
                        widthPx = size.width.toFloat()
                        detectDragGestures { change, _ ->
                            val pct = (change.position.x / widthPx * 100f).roundToInt()
                            onChange(pct.coerceIn(0, 100))
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percent / 100f)
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(RemoteColors.SliderFill2, RemoteColors.SliderFill1)
                            ),
                            RoundedCornerShape(2.dp)
                        )
                )
            }

            val handleOffsetDp: Dp = with(density) { (widthPx * percent / 100f).toDp() }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .offset(x = handleOffsetDp - 12.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "$percent", fontSize = 9.sp, color = Color(0xFF0A2B26))
            }
        }

        StepButton(label = "+", onClick = { onStep(5) })
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(RemoteColors.BtnBg, RoundedCornerShape(5.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = RemoteColors.IconDim, fontSize = 13.sp)
    }
}
