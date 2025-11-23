plugins {
    alias(libs.plugins.android.application)
    // Ce plugin active les services Google (nécessaire pour Firebase)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

android {
    namespace = "com.example.safecity"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.safecity"
        minSdk = 24
        targetSdk = 34
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

    // --- Dépendances de base Android ---
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.crashlytics)

    // --- Tests ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // --- UI et Utils ---
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.fragment:fragment:1.8.9")

    // --- Maps et Localisation ---
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // --- Images (Glide) ---
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // --- FIREBASE (Ajouté) ---
    // Import de la plateforme Firebase (BOM) - gère les versions automatiquement
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))

    // La base de données Firestore
    implementation("com.google.firebase:firebase-firestore")

    // Authentification Firebase
    implementation("com.google.firebase:firebase-auth")
}