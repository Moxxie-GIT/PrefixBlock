import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Moteur de règles PrefixBlock : Kotlin/JVM pur, sans aucune dépendance Android.
// Toute la logique de décision (normalisation, validation, priorité) vit ici et se teste sur la JVM.
plugins {
    alias(libs.plugins.kotlin.jvm)
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

    testImplementation(libs.junit)
}
