import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

android {
    namespace = "com.example.myappbooking"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myappbooking"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "2.4.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true  // Enable BuildConfig generation
    }

    signingConfigs {
        create("release") {
            storeFile = file("myKeyStore.jks")
            storePassword = localProperties.getProperty("RELEASE_KEYSTORE_PASSWORD", "")
            keyAlias = "myKey"
            keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD", "")
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"${localProperties.getProperty("BASE_URL_DEBUG", "")}\"")
            buildConfigField("String", "IMG_BASE_URL", "\"${localProperties.getProperty("BASE_URL_IMG_DEBUG", "")}\"")
            applicationIdSuffix = ".dev"
        }

        release {
            buildConfigField("String", "BASE_URL", "\"${localProperties.getProperty("BASE_URL_RELEASE", "")}\"")
            buildConfigField("String", "IMG_BASE_URL", "\"${localProperties.getProperty("BASE_URL_IMG_RELEASE", "")}\"")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("androidx.fragment:fragment-ktx:1.5.5")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.kizitonwose.calendar:view:2.6.0")
    implementation("com.github.skydoves:powerspinner:1.2.7")
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.airbnb.android:lottie:6.1.0")

//    implementation("com.kizitonwose.calendar:compose:<latest-version>")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}