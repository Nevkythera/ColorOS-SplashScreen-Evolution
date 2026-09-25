package com.Nevkythera.ColorOSSplashScreenEvolution.util

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * 自定义启动遮罩背景图片的存储与跨进程读取。
 *
 * 模块的 Hook 运行在 SystemUI 进程，读不到本应用的私有文件，也无法使用本应用
 * 临时授权的 content URI。这里把图片落在应用私有目录，再用一个只读
 * [com.Nevkythera.ColorOSSplashScreenEvolution.data.SplashImageProvider] 把它
 * 开放给 SystemUI 读取 —— 不依赖 root，也不依赖框架的远程文件通道。
 */
object SplashImageStore {

    /** 私有目录里的固定文件名（无扩展名，解码时由 ImageDecoder 自行识别格式）。 */
    const val FILE_NAME = "splash_bg"

    /** ContentProvider authority 的后缀，完整值 = `<packageName>.splashimage`。 */
    const val AUTHORITY_SUFFIX = ".splashimage"

    /** Hook 侧要读取的 content URI。 */
    fun contentUri(packageName: String): Uri =
        Uri.parse("content://$packageName$AUTHORITY_SUFFIX/$FILE_NAME")

    /** 本应用私有目录里的图片文件。 */
    fun imageFile(context: Context): File = File(context.filesDir, FILE_NAME)
}
