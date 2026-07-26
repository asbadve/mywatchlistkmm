import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.android.application)
    alias(libs.plugins.buildConfig)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.sqlDelight)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ktlint)
    jacoco
}

ktlint {
    // Generated sources (compose resource accessors, BuildConfig) are not ours to lint.
    filter {
        exclude { it.file.path.contains("/generated/") }
    }
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    tasks.create("testClasses")
    kotlin.applyDefaultHierarchyTemplate()
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    jvm("desktop")

    js {
        browser()
        binaries.executable()
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        all {
            languageSettings {
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(libs.lifecycle.viewmodel.compose)
                implementation(libs.material3.adaptive.navigation.suite)
                implementation(libs.adaptive)
                implementation(libs.adaptive.layout)
                implementation(libs.adaptive.navigation)
                implementation(libs.adaptive.navigation3)
                implementation(libs.material.icons.core)
                implementation(libs.material3.window.size.class1)
                implementation(libs.components.resources)
                api(compose.runtime)
                implementation(compose.material3)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)
//                implementation(libs.compose.ui.tooling.preview)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor)
                implementation(libs.napier)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.ktor.core)
                implementation(libs.ktor.client.logging)
                implementation(libs.composeIcons.featherIcons)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.multiplatformSettings)
                implementation(libs.koin.core)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.navigation3.runtime)
                implementation(libs.navigation3.ui)
                implementation(libs.lifecycle.viewmodel.navigation3)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
                implementation(libs.lifecycle.runtime.compose)
                implementation(libs.lifecycle.viewmodel.compose)
                implementation(libs.ktor.client.mock)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.appcompat)
                implementation(libs.androidx.activityCompose)
                implementation(libs.compose.uitooling)
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.sqlDelight.driver.android)
                implementation(libs.androidx.window)
                implementation(libs.androidx.ui.tooling.preview.android)
            }
        }
        val androidUnitTest by getting {
            dependencies {
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
                implementation(compose.desktop.common)
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.sqlDelight.driver.sqlite)
                implementation(libs.androidx.ui.tooling.preview.desktop)
            }
        }

        val desktopTest by getting {
            dependencies {
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(compose.html.core)
                implementation(libs.sqlDelight.driver.js)
                implementation(libs.okio.fakefilesystem)
            }
        }

        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by getting {
            dependsOn(commonMain)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                implementation(libs.ktor.client.darwin)
                implementation(libs.sqlDelight.driver.native)
            }
        }
    }
}

android {
    namespace = "com.ajinkyabadve.kmmmywatchlist"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        targetSdk = 34

        applicationId = "com.ajinkyabadve.kmmmywatchlist.androidApp"
        versionCode = 1
        versionName = "1.0.0"
    }
    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/resources")
        resources.srcDirs("src/commonMain/resources")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation(libs.androidx.material3.window.size.class1.android)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.ajinkyabadve.kmmmywatchlist.desktopApp"
            packageVersion = "1.0.0"
        }
    }
}

buildConfig {
    // BuildConfig configuration here.
    // https://github.com/gmazzo/gradle-buildconfig-plugin#usage-in-kts
    packageName = "kotlinproject.composeapp" // in lowercase! this is due to known issue
    // This plugin's generated field is inserted as raw Kotlin source (KotlinPoet's %L, no
    // auto-quoting) - the provider must supply the literal quote characters itself so the
    // generated `= <value>` is always valid Kotlin, whether or not the property is set.
    buildConfigField(
        "String",
        "TMDB_API_KEY",
        provider { "\"${(project.properties["MY_WATCH_LIST_TMDB_API_KEY"] as? String).orEmpty()}\"" },
    )
}

sqldelight {
    databases {
//        create("MyDatabase") { //todo
//            // Database configuration here.
//            // https://cashapp.github.io/sqldelight
//            packageName.set("com.ajinkyabadve.kmmmywatchlist.db")
//        }
    }
}

jacoco {
    toolVersion = "0.8.12"
}

// Discovered fresh on every run instead of a maintained list, so a newly added composable file
// is excluded automatically. Base names (no extension) of commonMain source files whose text
// contains @Composable. Kotlin compiles a file's top-level declarations to <BaseName>Kt.class
// (plus $-nested lambda classes) - distinct from any ScreenModel/repository class living in the
// same package, which stays covered. UI is verified manually (see run-app skill), not via unit
// tests, so it's excluded here the same way generated BuildConfig is.
val composableSourceFileBaseNames: Provider<Set<String>> =
    providers.provider {
        fileTree("src/commonMain/kotlin") { include("**/*.kt") }
            .filter { it.readText().contains("@Composable") }
            .map { it.nameWithoutExtension }
            .toSet()
    }

// desktopTest runs commonTest + desktopMain against the JVM/desktop target, so it's the one
// target JaCoCo (a JVM bytecode coverage tool) can instrument directly - no Android/iOS/JS
// equivalent is set up.
tasks.register<JacocoReport>("desktopTestCoverage") {
    dependsOn("desktopTest")
    group = "verification"
    description = "Generates a JaCoCo coverage report from the desktopTest task."

    executionData.setFrom(layout.buildDirectory.file("jacoco/desktopTest.exec"))
    sourceDirectories.setFrom(
        files(
            "src/commonMain/kotlin",
            "src/desktopMain/kotlin",
        ),
    )
    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("classes/kotlin/desktop/main")) {
            // Generated, not hand-written - excluded from coverage the same way ktlint excludes it.
            exclude("kotlinproject/composeapp/BuildConfig*")
            // Compose Multiplatform's generated resource accessors (Res.string, Res.drawable, etc).
            exclude("mywatchlist/composeapp/generated/**")
            // Compose-compiler-generated holder for composable lambdas - not our code.
            exclude("**/ComposableSingletons\$*.class")
            exclude {
                val name = it.file.name
                composableSourceFileBaseNames.get().any { baseName ->
                    name == "${baseName}Kt.class" || name.startsWith("${baseName}Kt$")
                }
            }
        },
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
