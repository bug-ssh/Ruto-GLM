import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.rikka.refine)
    alias(libs.plugins.google.devtools.ksp)
}

val keystoreDir: String = "$rootDir/keystore"

val keystoreProps: Properties = Properties()
for (name in arrayOf("r0s.properties", "debug.properties")) {
    val f: File = file("$keystoreDir/$name")
    if (!f.exists()) continue
    keystoreProps.load(f.inputStream())
    break
}

android {
    namespace = "com.rosan.ruto"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rosan.ruto"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.01"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    signingConfigs {
        val storeFileVal: File = file("$keystoreDir/${keystoreProps.getProperty("storeFile") ?: "release.jks"}")
        val storePasswordVal: String = keystoreProps.getProperty("storePassword") ?: ""
        val keyAliasVal: String = keystoreProps.getProperty("keyAlias") ?: ""
        val keyPasswordVal: String = keystoreProps.getProperty("keyPassword") ?: ""
        getByName("debug") {
            this.storeFile = storeFileVal
            this.storePassword = storePasswordVal
            this.keyAlias = keyAliasVal
            this.keyPassword = keyPasswordVal
        }
        create("release") {
            this.storeFile = storeFileVal
            this.storePassword = storePasswordVal
            this.keyAlias = keyAliasVal
            this.keyPassword = keyPasswordVal
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        aidl = true
        compose = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    implementation(project(":ext"))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    compileOnly(project(":hidden-api"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.compose.materialIcons)

    implementation(libs.androidx.compose.navigation)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    ksp(libs.androidx.room.compiler)

    implementation(libs.lsposed.hiddenapibypass)

    annotationProcessor(libs.rikka.refine.annotation.processor)
    compileOnly(libs.rikka.refine.annotation)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    implementation(libs.rikka.shizuku.api)
    implementation(libs.rikka.shizuku.provider)
    implementation(libs.iamr0s.androidAppProcess)

    implementation(platform(libs.langchain4j.bom))
    implementation(libs.langchain4j.kotlin)
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation(libs.langchain4j.openai)
    implementation(libs.langchain4j.google.ai.gemini)

    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation("com.halilibo.compose-richtext:richtext-ui:1.0.0-alpha03")
    implementation("com.halilibo.compose-richtext:richtext-ui-material3:1.0.0-alpha03")
    implementation("com.halilibo.compose-richtext:richtext-commonmark:1.0.0-alpha03")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
