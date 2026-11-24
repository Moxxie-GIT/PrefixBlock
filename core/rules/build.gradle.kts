import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Moteur de règles PrefixBlock : Kotlin/JVM pur, sans aucune dépendance Android.
// Toute la logique de décision (normalisation, validation, priorité) vit ici et se teste sur la JVM.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.libphonenumber)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
