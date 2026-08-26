// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  // Existing Android-only plugins
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false

  // NEW: Cross-platform desktop and compilation plugins
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.jetbrains.compose) apply false
}
