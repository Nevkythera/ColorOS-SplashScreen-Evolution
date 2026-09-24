plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

sourceSets {
    named("main") {
        java.srcDirs("src/main/java")
        kotlin.srcDirs("src/main/kotlin")
    }
}

dependencies {
    // 完整的 framework class jar（含隐藏 API），Robolectric 发布，编译隐藏 API 的标准手段。
    // 之前用 dex2jar 从 framework.jar 转出的 class jar 不完整（跳过了解析不了的类），已弃用。
    compileOnly("org.robolectric:android-all:16-robolectric-13921718")
    // 桩只在编译期可见，不随 jar 产出（避免 SystemUI 进程里的类冲突）
    compileOnly(project(":shell-stubs"))
}

// 临时诊断：打印编译类路径
tasks.register("cp") {
    doLast {
        val files = configurations.getByName("compileClasspath").files
        println("CP-COUNT: ${files.size}")
        files.forEach { println("CP: ${it.name}  exists=${it.exists()}") }
    }
}
