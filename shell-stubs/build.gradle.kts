plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

sourceSets {
    named("main") {
        java.srcDirs("src/main/java")
        kotlin.srcDirs("src/main/kotlin")
    }
}

dependencies {
    compileOnly("org.robolectric:android-all:16-robolectric-13921718")
}
