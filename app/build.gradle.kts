import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ---------------------------------------------------------------- подпись
// Ключ ищется сначала по пути из окружения (CI), затем в корне проекта.
// Пароли никогда не хранятся в репозитории — только в секретах GitHub.
val releaseKeystore = file(
    System.getenv("KEYSTORE_FILE") ?: "${rootProject.projectDir}/release.keystore"
)
val releaseStorePassword = System.getenv("KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("KEY_ALIAS") ?: "modrinthclient"
val releaseKeyPassword = System.getenv("KEY_PASSWORD") ?: releaseStorePassword
val hasReleaseKey = releaseKeystore.exists() && !releaseStorePassword.isNullOrBlank()

android {
    namespace = "myxax.modrinth.client"
    compileSdk = 36

    defaultConfig {
        applicationId = "myxax.modrinth.client"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "1.1.5"
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = releaseKeystore
                storeType = "PKCS12"
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            // Подпись настоящим релизным ключом. Ключ и пароли приходят из
            // секретов GitHub Actions (см. .github/workflows/build.yml) либо
            // лежат рядом с проектом при локальной сборке.
            if (hasReleaseKey) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")

    implementation("androidx.compose.ui:ui:1.8.2")
    implementation("androidx.compose.ui:ui-graphics:1.8.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.8.2")
    implementation("androidx.compose.foundation:foundation:1.8.1")
    implementation("androidx.compose.material3:material3:1.4.0")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    debugImplementation("androidx.compose.ui:ui-tooling:1.8.2")
}
