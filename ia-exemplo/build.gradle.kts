plugins {
    alias(libs.plugins.android.application)
}

// App mínimo que implementa o plugue de IA (docs/plugue-ia.md). Serve de teste e de exemplo para a IRIS e a Eloy.
android {
    namespace = "com.brendon978.iaexemplo"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.brendon978.iaexemplo"
        minSdk = 35
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
