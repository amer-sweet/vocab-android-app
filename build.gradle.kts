// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.0" apply false // Example version, adjust as needed
    id("com.android.library") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false // Example version, adjust as needed
    id("com.google.devtools.ksp") version "1.9.0-1.0.13" apply false // Example version for KSP (if using)
    // Or use kapt if preferred: id("org.jetbrains.kotlin.kapt") version "1.9.0" apply false
}

