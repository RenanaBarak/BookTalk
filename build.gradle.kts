// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // SafeArgs plugin for Navigation component
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.7.7")
        // Google Services plugin
        classpath("com.google.gms:google-services:4.4.0")
    }
}

plugins {
    // אלה כאן לצורך הגדרה כללית, בפועל תשתמש בהם בקובץ build.gradle.kts של המודול
    id("com.android.application") version "8.10.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
}

