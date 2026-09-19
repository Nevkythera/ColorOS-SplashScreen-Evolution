pluginManagement {
    repositories {
        // 沙箱内唯一可达的公共仓库（腾讯云 nexuser maven-public 代理）
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
    }
}

rootProject.name = "CSE"
include(":app")
