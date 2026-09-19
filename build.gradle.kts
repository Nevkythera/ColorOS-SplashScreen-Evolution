plugins {
    id("com.android.application") version "8.13.0" apply false
    // Kotlin 2.2.0 —— 与参考项目 MCGA 对齐。
    //
    // 升级原因（踩坑记录）：haze 1.7.1 与 colorpicker-compose 1.1.2 均使用
    // Kotlin 2.2 编译，其 metadata 二进制版本为 2.2.0；而 Kotlin 2.0.21 编译
    // 器只认 2.0.0，会直接报：
    //   "Module was compiled with an incompatible version of Kotlin.
    //    The binary version of its metadata is 2.2.0, expected version is 2.0.0."
    // 高版本编译器可向下读取低版本 metadata（material3 1.4.0-alpha16 等
    // 用 Kotlin 2.0 编译的库不受影响），因此升到 2.2.0 是最小改动方案。
    id("org.jetbrains.kotlin.android") version "2.2.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0" apply false
}
