// Archivo de build raíz: declara los plugins que usan los módulos.
plugins {
    alias(libs.plugins.android.application) apply false
    // AGP 9 trae Kotlin integrado; declararlo aquí solo fija la versión de KGP.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.roborazzi) apply false
}
