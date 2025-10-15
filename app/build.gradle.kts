plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.githubvitalyredb.gpstraceeme"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.githubvitalyredb.gpstraceeme"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity:1.8.2")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Добавляем библиотеку Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.0.1") // Проверьте на последнюю версию
    // библиотека для OkHttp для сетевых запросов
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    // библиотека для преобразования объектов в JSON
    implementation("com.google.code.gson:gson:2.10.1")
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") // Проверьте на последнюю версию
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")


}