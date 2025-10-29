plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("androidx.navigation.safeargs.kotlin")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.appmanagement"

    // phải là 36 để satisfy androidx.activity:1.11.0, core-ktx:1.17.0
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.appmanagement"

        // bạn đang build cho Android 13+ only, vẫn ok
        minSdk = 33

        // GIỮ targetSdk thấp (34) để:
        // - tránh bị bắt tuân theo policy 16KB page size của Android 15+
        // - không dính lại lỗi libsqlcipher.so (mà ta đã gỡ)
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
    // Version Catalog deps
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // BCrypt (hash password/PIN -> không native page-size issue)
    implementation("at.favre.lib:bcrypt:0.10.2")

    // ĐÃ GỠ SQLCipher để tránh libsqlcipher.so 16KB warning
    // implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    // implementation("androidx.sqlite:sqlite:2.4.0")

    // EncryptedSharedPreferences cho data nhạy cảm nhỏ
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Room (DB local, không mã hóa full-file, không kéo lib native nguy hiểm)
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
}
