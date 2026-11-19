plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.tesseraui.sample"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.tesseraui.sample"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.4.0"
    }
    buildFeatures { compose = true }
    kotlinOptions { jvmTarget = "17" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Robolectric 运行时会自己从 Maven 下载 Android 框架 jar，网络抖动就会失败。
        // 改为由 Gradle 解析（会被缓存），再让 Robolectric 离线读取
        unitTests.all { test ->
            test.dependsOn(robolectricJars)
            test.systemProperty("robolectric.offline", "true")
            test.systemProperty("robolectric.dependency.dir", robolectricJarsDir.get().asFile.absolutePath)
        }
    }
}

// 同一个模块的两个版本放在一个配置里会被 Gradle 合并成最高版本，所以各用一个配置
val robolectricRuntime: Configuration by configurations.creating { isTransitive = false }
val robolectricRuntime15: Configuration by configurations.creating { isTransitive = false }
val robolectricJarsDir = layout.buildDirectory.dir("robolectric-jars")
val robolectricJars = tasks.register<Sync>("robolectricJars") {
    from(robolectricRuntime, robolectricRuntime15)
    into(robolectricJarsDir)
}

dependencies {
    implementation(project(":tessera"))
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.9.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    // Robolectric 4.14.1 的 Android 14（测试用的 sdk = 34）与 Android 15（应用的 targetSdk）运行时
    robolectricRuntime("org.robolectric:android-all-instrumented:14-robolectric-10818077-i7")
    robolectricRuntime15("org.robolectric:android-all-instrumented:15-robolectric-12650502-i7")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
