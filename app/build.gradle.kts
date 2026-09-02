import java.time.LocalDate
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

fun fetchGitCommitHash(): String {
    return try {
        val process =
            ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
        process.inputStream.bufferedReader().readText().trim()
    } catch (_: Exception) {
        "unknown"
    }
}

val gitCommit = fetchGitCommitHash()
val buildDate: String? = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
val epochTime = (System.currentTimeMillis() / 1000).toInt()

android {
    namespace = "com.avantgardelabs.healthyhabitstracker"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.avantgardelabs.healthyhabitstracker"
        minSdk = 26
        targetSdk = 37
        versionCode = epochTime
        versionName = "$epochTime.0"

        buildConfigField("String", "GIT_COMMIT", "\"$gitCommit\"")
        buildConfigField("String", "BUILD_DATE", "\"$buildDate\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

val copyLicense =
    tasks.register<Copy>("copyLicense") {
        from(rootProject.file("LICENSE"))
        into(file("src/main/assets"))
    }

tasks.named("preBuild") {
    dependsOn(copyLicense)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.play.review.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
