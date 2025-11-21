plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.safecity"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.safecity"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    // ✅ Bibliothèque Fragment AndroidX
    implementation("androidx.fragment:fragment:1.8.9") // Utilisez la dernière version stable
    // ✅ NÉCESSAIRE POUR LA CARTE : Google Maps SDK
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    // ✅ NÉCESSAIRE POUR LE GPS : Google Location Services (pour Tâche C3/B2)
    implementation("com.google.android.gms:play-services-location:21.0.1")
    // AJOUTER CECI pour gérer les images automatiquement :
    implementation("com.github.bumptech.glide:glide:4.16.0")
}