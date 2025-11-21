plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("androidx.navigation.safeargs.kotlin")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.appmanagement"

    // compileSdk 36 để dùng được các thư viện AndroidX mới
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.appmanagement"

        // chỉ hỗ trợ từ Android 13 trở lên
        minSdk = 33

        // targetSdk 34 để tránh các ràng buộc mới của Android 15
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

        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // thư viện trong version catalog
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // BCrypt để băm mật khẩu/PIN
    implementation("at.favre.lib:bcrypt:0.10.2")

    // EncryptedSharedPreferences cho dữ liệu nhạy cảm nhỏ
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Room (cơ sở dữ liệu cục bộ)
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // ViewModel + LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.1")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.1")

    // Glide (load ảnh)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Firebase: dùng BoM để đồng bộ phiên bản
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))

    // Firebase Analytics (tuỳ chọn)
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Firebase Auth (đăng nhập, có FirebaseUser)
    implementation("com.google.firebase:firebase-auth-ktx")

    // Firestore (sau này lưu User/Task)
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Hỗ trợ dùng .await() với Task của Firebase trong coroutine
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // Đăng nhập Google (nút Sign in with Google)
    implementation("com.google.android.gms:play-services-auth:21.2.0")
}
