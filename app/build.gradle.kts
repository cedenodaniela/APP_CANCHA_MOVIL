import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.canchapp.movil"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.canchapp.movil"
        minSdk = 26 // Android 8.0 (RNF-06)
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Por defecto, 10.0.2.2: el localhost de la computadora visto desde el emulador.
            // En un celular físico se usa `adb reverse tcp:8080 tcp:8080` y, en local.properties
            // (no se versiona), canchapp.api.url=http://localhost:8080/api/v1/
            buildConfigField("String", "API_URL", "\"${urlApiDebug()}\"")
        }
        release {
            // Se reemplaza por la URL HTTPS del backend desplegado (RNF-02).
            buildConfigField("String", "API_URL", "\"https://api.canchapp.example/api/v1/\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    debugImplementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // Fotos de canchas (RF-04): Coil las carga con el mismo OkHttp de la API, que lleva el JWT.
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    // Compose trae una versión vieja de Espresso que falla en Android 17 (InputManager.getInstance).
    androidTestImplementation(libs.androidx.test.espresso)
}

/**
 * URL de la API en debug. Se lee de `canchapp.api.url` en local.properties, que no se versiona,
 * para que cada quien apunte a su backend sin cambiar este archivo. Sin esa clave, el emulador.
 */
fun urlApiDebug(): String {
    val propiedades = Properties()
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.reader(Charsets.UTF_8).use { propiedades.load(it) }
    val url = propiedades.getProperty("canchapp.api.url")?.trim().orEmpty()
    return url.ifEmpty { "http://10.0.2.2:8080/api/v1/" }.let { if (it.endsWith("/")) it else "$it/" }
}
