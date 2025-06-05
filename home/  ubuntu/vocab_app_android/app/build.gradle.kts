// Use KSP for Room (recommended)
// plugins {
//     id("com.google.devtools.ksp")
// }

// Or use Kapt if preferred
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Add Kapt plugin if using it for Room
    kotlin("kapt") version "1.9.0" // Ensure version matches kotlin plugin
    // Add navigation safe args plugin
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.example.vocabapp"
    compileSdk = 34 // Use a recent SDK version

    defaultConfig {
        applicationId = "com.example.vocabapp"
        minSdk = 24 // Example minimum SDK
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // For Room using KSP
        // ksp {
        //     arg("room.schemaLocation", "$projectDir/schemas")
        // }
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
    buildFeatures {
        viewBinding = true // Enable ViewBinding
    }
    // For Room using Kapt
    kapt {
        arguments {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }
    packaging {
         // Exclude duplicate license files often included by Google API clients
        resources.excludes.add("META-INF/LICENSE")
        resources.excludes.add("META-INF/LICENSE.txt")
        resources.excludes.add("META-INF/NOTICE")
        resources.excludes.add("META-INF/NOTICE.txt")
        resources.excludes.add("META-INF/DEPENDENCIES")
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0") // Example version
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.activity:activity-ktx:1.8.2") // For registerForActivityResult
    implementation("androidx.fragment:fragment-ktx:1.6.2") // For viewModels delegate

    // Navigation Component
    val nav_version = "2.7.7"
    implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
    implementation("androidx.navigation:navigation-ui-ktx:$nav_version")

    // Lifecycle Components (ViewModel, LiveData, Lifecycle Scope)
    val lifecycle_version = "2.7.0"
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycle_version")

    // Room Persistence Library
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version") // Coroutine support for Room
    // annotationProcessor("androidx.room:room-compiler:$room_version") // Use annotationProcessor for Java
    // KSP for Room (Kotlin)
    // ksp("androidx.room:room-compiler:$room_version")
    // Or Kapt for Room (Kotlin)
    kapt("androidx.room:room-compiler:$room_version")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Google Drive API (using Google API Client Library for Java)
    // Note: These can bring in a lot of dependencies, check for conflicts
    implementation("com.google.api-client:google-api-client-android:2.2.0") {
        exclude(group = "org.apache.httpcomponents")
    }
    implementation("com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0") {
        exclude(group = "org.apache.httpcomponents")
    }
    // Required for GoogleAccountCredential
    implementation("com.google.android.gms:play-services-auth:21.0.0") // Already included above, but good to note
    implementation("com.google.api.client:google-api-client-gson:2.2.0") // Use GSON factory

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Testing Dependencies (Optional but recommended)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

