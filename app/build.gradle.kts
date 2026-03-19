plugins {
    id("com.android.application")
}


android {
    namespace = "liege.counter"
    compileSdk = 34

    defaultConfig {
        val apiBaseUrl       = project.findProperty("API_BASE_URL")        as String? ?: "https://fallback-url.com/"
        val supabaseUrl      = project.findProperty("SUPABASE_URL")        as String? ?: "https://YOUR_PROJECT_REF.supabase.co/rest/v1/"
        val supabaseAnonKey  = project.findProperty("SUPABASE_ANON_KEY")   as String? ?: "YOUR_ANON_KEY_HERE"
        val versionJsonUrl   = project.findProperty("VERSION_JSON_URL")    as String? ?: ""
        buildConfigField("String", "API_BASE_URL",       "\"$apiBaseUrl\"")
        buildConfigField("String", "SUPABASE_URL",       "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY",  "\"$supabaseAnonKey\"")
        buildConfigField("String", "VERSION_JSON_URL",   "\"$versionJsonUrl\"")
        applicationId = "liege.counter"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(project.property("STORE_FILE") as String)
            storePassword = project.property("PASSWORD") as String
            keyAlias = project.property("KEY_ALIAS") as String
            keyPassword = project.property("PASSWORD") as String
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
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
    implementation("com.google.android.gms:play-services-auth:21.1.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}
