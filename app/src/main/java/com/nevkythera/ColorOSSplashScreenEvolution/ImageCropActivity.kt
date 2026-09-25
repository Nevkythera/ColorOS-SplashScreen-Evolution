package com.Nevkythera.ColorOSSplashScreenEvolution

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.theme.AppTheme
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.CropImageView
import com.Nevkythera.ColorOSSplashScreenEvolution.util.SplashImageStore
import com.Nevkythera.ColorOSSplashScreenEvolution.util.SplashMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 媒体裁切页：源文件由设置页预先拷到应用缓存，这里以**文件路径**传入。
 *
 * 取景框即整屏（与启动遮罩 centerCrop 的显示范围一致），所见即所得：
 *  - 静态图片：确认时直接 [CropImageView.bake] 烘成 PNG 保存；
 *  - GIF / 视频：确认时**原样保存媒体**并回传一份归一化裁切变换 [SplashMedia.Transform]，
 *    由 SystemUI 侧按同参数重放（保留动画 / 视频播放）。
 */
class ImageCropActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sourcePath = intent?.getStringExtra(EXTRA_SOURCE_PATH)
        val displayUri = intent?.getStringExtra(EXTRA_DISPLAY_URI).orEmpty()
        val kind = intent?.getStringExtra(EXTRA_KIND) ?: SplashMedia.KIND_IMAGE
        if (sourcePath == null) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        setContent {
            AppTheme {
                CropScreen(sourcePath, displayUri, kind)
            }
        }
    }

    @Composable
    private fun CropScreen(sourcePath: String, displayUri: String, kind: String) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var drawable by remember { mutableStateOf<Drawable?>(null) }
        var loading by remember { mutableStateOf(true) }
        var busy by remember { mutableStateOf(false) }
        var cropView by remember { mutableStateOf<CropImageView?>(null) }

        LaunchedEffect(sourcePath) {
            drawable = withContext(Dispatchers.IO) { loadPreview(context, sourcePath, kind) }
            loading = false
            if (drawable == null) {
                Toast.makeText(context, "无法加载媒体", Toast.LENGTH_SHORT).show()
                setResult(RESULT_CANCELED)
                finish()
            }
        }

        LaunchedEffect(drawable) {
            drawable?.let { cropView?.setDrawable(it) }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx -> CropImageView(ctx).also { cropView = it } }
            )

            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    setResult(RESULT_CANCELED)
                    finish()
                }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        enabled = drawable != null && !busy,
                        onClick = { cropView?.rotate() }
                    ) {
                        Text(stringResource(R.string.crop_rotate))
                    }
                    TextButton(
                        enabled = drawable != null && !busy,
                        onClick = {
                            val view = cropView ?: return@TextButton
                            busy = true
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    save(view, context, sourcePath, kind)
                                }
                                busy = false
                                if (result) {
                                    setResult(
                                        RESULT_OK,
                                        Intent().apply {
                                            if (displayUri.isNotBlank()) setData(Uri.parse(displayUri))
                                            putExtra(EXTRA_RESULT_KIND, kind)
                                            putExtra(
                                                EXTRA_RESULT_TRANSFORM,
                                                if (kind == SplashMedia.KIND_IMAGE) ""
                                                else view.transform().encode()
                                            )
                                        }
                                    )
                                    finish()
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.splash_image_save_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.dialog_confirm))
                    }
                }
            }
        }
    }

    /** 静态图：烘成 PNG；GIF / 视频：原样拷贝。 */
    private fun save(
        view: CropImageView,
        context: Context,
        sourcePath: String,
        kind: String
    ): Boolean = runCatching {
        if (kind == SplashMedia.KIND_IMAGE) {
            val bmp = view.bake()
            FileOutputStream(SplashImageStore.imageFile(context)).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } else {
            File(sourcePath).inputStream().use { input ->
                SplashImageStore.imageFile(context).outputStream().use { input.copyTo(it) }
            }
        }
        true
    }.getOrDefault(false)

    private fun loadPreview(context: Context, path: String, kind: String): Drawable? =
        runCatching {
            if (kind == SplashMedia.KIND_VIDEO) {
                val mmr = MediaMetadataRetriever()
                try {
                    mmr.setDataSource(path)
                    val rot = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                        ?.toIntOrNull() ?: 0
                    val frame = mmr.getFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: return null
                    val oriented = if (rot == 90 || rot == 270) rotateBitmap(frame, rot) else frame
                    BitmapDrawable(context.resources, oriented)
                } finally {
                    runCatching { mmr.release() }
                }
            } else {
                ImageDecoder.decodeDrawable(ImageDecoder.createSource(File(path))) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            }
        }.getOrNull()

    private fun rotateBitmap(src: Bitmap, degrees: Int): Bitmap {
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    companion object {
        private const val EXTRA_SOURCE_PATH = "source_path"
        private const val EXTRA_DISPLAY_URI = "display_uri"
        private const val EXTRA_KIND = "kind"

        /** 回传：媒体类型与归一化裁切变换。 */
        const val EXTRA_RESULT_KIND = "result_kind"
        const val EXTRA_RESULT_TRANSFORM = "result_transform"

        fun intent(context: Context, sourcePath: String, displayUri: String, kind: String): Intent =
            Intent(context, ImageCropActivity::class.java)
                .putExtra(EXTRA_SOURCE_PATH, sourcePath)
                .putExtra(EXTRA_DISPLAY_URI, displayUri)
                .putExtra(EXTRA_KIND, kind)
    }
}
