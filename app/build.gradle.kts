plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.Nevkythera.ColorOSSplashScreenEvolution"
    compileSdk = 36
    // 离线沙箱内只安装了 36.0.0，显式指定避免 AGP 回落到默认的 35.0.0
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.Nevkythera.ColorOSSplashScreenEvolution"
        minSdk = 35
        targetSdk = 36
        versionCode = 60
        versionName = "0.4_pre-release"
    }

    buildTypes {
        release {
            // 开启 R8 收缩 + 混淆：把 material3/compose 等依赖的未用代码裁掉，
            // 体积从 ~43MB 压到 10MB 级别。proguard-rules.pro 已保留 libxposed 入口类。
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // LSPosed (libxposed) API 102 —— 仅编译期依赖，运行期由框架注入实现
    compileOnly("io.github.libxposed:api:102.0.0")
    // 模块设置界面侧的框架服务桥接：用于判定"模块是否已激活"
    // （XposedServiceHelper 绑定成功 = 已激活）、读写远程偏好。
    //
    // 版本注意（踩坑记录）：
    //  - 102.0.0 的 AAR 元数据声明 minCompileSdk=37，而 AGP 8.13 上限是 36，
    //    直接依赖会报 "requires compile against version 37 or later"。
    //    已通过 gradle.properties 里的 android.experimental.disableCompileSdkChecks
    //    关闭该元数据校验——102 的字节码实际并未引用 API 37 的符号。
    //    若以后补装了 android-37 平台，可去掉该开关并改回 compileSdk 37。
    //  - 102 与 101 的 POM 都把 kotlin-stdlib 写死为 2.2.10（compile 作用域），
    //    其元数据版本 2.2.0 与工程使用的 Kotlin 2.0.21 编译器不兼容，
    //    会导致 "Module was compiled with an incompatible version of Kotlin"。
    //    因此显式 exclude —— 工程自身的 Kotlin 版本已由 kotlin-android 插件注入。
    implementation("io.github.libxposed:service:102.0.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    }

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.activity:activity-compose:1.10.1")

    // 应用内语言切换（AppCompatDelegate.setApplicationLocales）。
    //
    // ★ 只为 LocaleManager 提供 AppCompatDelegate / LocaleListCompat；
    //   本模块的 Activity 并不继承 AppCompatActivity（用的是 ComponentActivity +
    //   Compose 原生主题），所以 appcompat 不参与界面渲染，不会有主题冲突。
    //   若以后要换成纯 AndroidX 方案，可改用
    //   `androidx.core:core` 的 LocaleManagerCompat + 手动 recreate。
    implementation("androidx.appcompat:appcompat:1.7.1")

    // 图标取色（从图标提取主色做背景色）—— 与 RestoreSplashScreen 一致
    implementation("androidx.palette:palette-ktx:1.0.0")

    // 导航 + 共享元素过渡动画（MCGA 式页面跳转）
    implementation("androidx.navigation:navigation-compose:2.9.6")

    // 顶栏渐变模糊（MCGA 同款 Haze 库）
    implementation("dev.chrisbanes.haze:haze:1.7.1")

    // 背景自定义颜色选择器（MCGA 同款）
    implementation("com.github.skydoves:colorpicker-compose:1.1.2")

    // Jetpack Compose —— 不使用 compose-bom。
    //
    // 原因：compose-bom 2025.10.00 对 material3 施加了 `strictly 1.4.0` 约束，
    // 会把任何显式指定的 alpha 版本强制解析回 1.4.0；而 material3 1.4.0 正式版
    // 把 MaterialExpressiveTheme / MotionScheme / expressiveXxxColorScheme /
    // ExperimentalMaterial3ExpressiveApi 全部标为 internal，模块侧无法引用。
    // 因此这里放弃 BOM，逐项显式固定版本。
    val composeUi = "1.9.3"
    implementation("androidx.compose.material3:material3:1.4.0-alpha16")
    implementation("androidx.compose.ui:ui:$composeUi")
    implementation("androidx.compose.ui:ui-graphics:$composeUi")
    // MD3E 加载指示器的官方形状库（RoundedPolygon / Morph / MaterialShapes）。
    // material3 只是传递依赖它，这里显式声明以便直接 import 使用官方形状。
    implementation("androidx.graphics:graphics-shapes:1.0.1")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeUi")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeUi")
}
