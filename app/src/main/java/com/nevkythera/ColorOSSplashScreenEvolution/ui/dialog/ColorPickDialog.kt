package com.Nevkythera.ColorOSSplashScreenEvolution.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.AlphaTile
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.drawColorIndicator
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.Nevkythera.ColorOSSplashScreenEvolution.R
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.CapsuleShapes

/**
 * HSV 颜色选择对话框 —— 对应 MCGA 的 `ColorPickDialog`。
 *
 * 用于背景页「自定义颜色」的浅色 / 暗色取色。依赖 colorpicker-compose：
 *   - [HsvColorPicker]  主色板
 *   - [AlphaSlider]    透明度
 *   - [BrightnessSlider] 亮度
 *
 * 对话框外壳已提取到公共 [BaseDialog]（图标参数化），这里传入取色图标。
 *
 * @param initialColor 初始颜色（ARGB Int），对话框打开时预置。
 * @param onConfirmation 确认时回传选中的 ARGB 颜色。
 */
@Composable
fun ColorPickDialog(
    title: String,
    description: String? = null,
    initialColor: Int,
    onDismissRequest: () -> Unit,
    onConfirmation: (Int) -> Unit
) {
    val controller = rememberColorPickerController()
    var hexCode by remember { mutableStateOf("") }
    var textColor by remember { mutableStateOf(Color.Transparent) }

    BaseDialog(
        title = title,
        description = description,
        iconRes = R.drawable.format_color_fill,
        onDismissRequest = onDismissRequest,
        onConfirmation = {
            // 用 controller 的实时选中色兜底，避免用户未拖动就点确定时拿到透明色
            val finalColor = controller.selectedColor.value
            onConfirmation(finalColor.toArgb())
        }
    ) {
        Column {
            HsvColorPicker(
                modifier = Modifier
                    .height(240.dp)
                    .padding(12.dp),
                controller = controller,
                drawOnPosSelected = {
                    drawColorIndicator(
                        controller.selectedPoint.value,
                        controller.selectedColor.value,
                    )
                },
                onColorChanged = { colorEnvelope ->
                    hexCode = colorEnvelope.hexCode
                    textColor = colorEnvelope.color
                },
                initialColor = Color(initialColor),
            )

            AlphaSlider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
                    .height(35.dp)
                    .align(Alignment.CenterHorizontally),
                controller = controller,
            )

            BrightnessSlider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 12.dp)
                    .height(35.dp)
                    .align(Alignment.CenterHorizontally),
                controller = controller,
            )

            Text(
                text = "#$hexCode",
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            AlphaTile(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CapsuleShapes.single(16.dp))
                    .align(Alignment.CenterHorizontally),
                controller = controller,
            )
            Spacer(modifier = Modifier)
        }
    }
}
