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
                // `generateAsync = true` (required by the JS WebWorkerDriver) makes every
                // platform's generated Queries API suspend-based - this is what provides the
                // `.synchronous()` schema adapter (for the sync Android/Native drivers) and the
                // `.awaitAsList()`/`.await()` extensions the repositories call.
                implementation(libs.sqlDelight.async.extensions)
                // Query<T>.asFlow() - lets a repository be the single source of truth for a cache
                // table (repo decides cache-vs-network, exposes one Flow; see MovieDetailCacheRepository).
                implementation(libs.sqlDelight.coroutines.extensions)
                // QueryPagingSource - bridges the paged queries in MyDatabase.sq straight to a
                // PagingSource for the Favorites/Watchlist/Lists grids. No published `android`
                // Gradle-module variant exists for this artifact (confirmed against
                // sqldelight/sqldelight's source), but Gradle's KMP variant matching falls back to
                // the `-jvm` artifact for the Android compile classpath and it works - see the
                // paging-3-remote-mediator plan for how this was verified.
                implementation(libs.sqlDelight.paging3.extensions)
                implementation(libs.paging.common)
                implementation(libs.paging.compose)
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
                // Flow<PagingData<T>>.asSnapshot() - lets ScreenModel tests assert on paged data
                // without a Compose test harness.
                implementation(libs.paging.testing)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.appcompat)
                implementation(libs.androidx.browser)
                implementation(libs.androidx.activityCompose)
                implementation(libs.compose.uitooling)
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.sqlDelight.driver.android)
                implementation(libs.androidx.work)
                implementation(libs.androidx.window)
                implementation(libs.androidx.ui.tooling.preview.android)
                implementation(libs.androidx.splashscreen)
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
                // Explicit rather than relying on it arriving transitively via desktopMain -
                // this is exactly the dependency whose absence from the test source set's
                // classpath caused JetBrains/compose-multiplatform#1352 (skiko native library
                // not found under uiTestJUnit4/jvmTest). currentOs resolves per the machine
                // actually running Gradle, so this picks the Linux build on CI automatically.
                implementation(compose.desktop.currentOs)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(compose.html.core)
                implementation(libs.sqlDelight.driver.js)
                // WebWorkerDriver's bundled SQL.js worker needs all three to resolve/bundle
                // correctly at build time. https://sqldelight.github.io/sqldelight/2.3.2/js_sqlite/
                // sqldelight-sqljs-worker's own version must match the sqlDelight plugin version
                // (libs.versions.toml's sqlDelight = "2.3.2") - it's the npm package that ships
                // the actual sqljs.worker.js DatabaseDriverFactory.kt imports by URL; without an
                // explicit npm() declaration here it's absent from yarn.lock and webpack fails
                // with "Module not found: Can't resolve '@cashapp/sqldelight-sqljs-worker/sqljs.worker.js'".
                implementation(npm("@cashapp/sqldelight-sqljs-worker", "2.3.2"))
                implementation(npm("sql.js", "1.6.2"))
                implementation(devNpm("copy-webpack-plugin", "9.1.0"))
                // okio's FileSystem companion (pulled in via Coil) calls os.tmpdir() at init time -
                // a real code path, not dead code like sql.js's require("path")/require("fs") - so
                // it needs an actual polyfill, not resolve.fallback: { os: false }. See
                // webpack.config.d/sql-js-node-polyfill-fallback.js for where this gets wired in.
                implementation(devNpm("os-browserify", "0.3.0"))
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

// Real release signing, read from env vars so the keystore itself never touches the repo (CI
// injects these from GitHub Actions secrets; a local release build needs them exported too).
// Falls back to the debug key when any of the four are missing, so the existing
// scroll-performance benchmarking workflow (`assembleRelease`/`installRelease` with no secrets
// present, see the doFirst warning below) keeps working unchanged.
val androidReleaseKeystorePath: String? = System.getenv("ANDROID_RELEASE_KEYSTORE_PATH")
val androidReleaseKeystorePassword: String? = System.getenv("ANDROID_RELEASE_KEYSTORE_PASSWORD")
val androidReleaseKeyAlias: String? = System.getenv("ANDROID_RELEASE_KEY_ALIAS")
val androidReleaseKeyPassword: String? = System.getenv("ANDROID_RELEASE_KEY_PASSWORD")
val hasAndroidReleaseSigningConfig =
    listOf(
        androidReleaseKeystorePath,
        androidReleaseKeystorePassword,
        androidReleaseKeyAlias,
        androidReleaseKeyPassword,
    ).all { !it.isNullOrBlank() }

// Auto-incrementing release version: release.yml's `version` job derives this from the pushed
// git tag (`v1.2.3` -> "1.2.3") and exports it as RELEASE_VERSION_NAME, so tagging a release is
// the only version bump a release needs - nothing to hand-edit here beforehand. Falls back to a
// stable default for ordinary local builds, where no such tag/env var exists.
val releaseVersionName: String = System.getenv("RELEASE_VERSION_NAME") ?: "1.0.0"

// Android's versionCode must strictly increase with every Play-installable build - derived from
// the same semver string so a tag can never silently produce a lower or equal code than the last
// release. Any pre-release suffix (e.g. "1.2.3-rc1") is dropped before parsing; a component this
// app's own tags won't produce (non-numeric, or more than 3 dot-separated parts) falls back to 0
// rather than failing the build outright.
val releaseVersionCode: Int =
    releaseVersionName
        .substringBefore('-')
        .split(".")
        .map { it.toIntOrNull() ?: 0 }
        .let { (it.getOrElse(0) { 1 }) * 1_000_000 + (it.getOrElse(1) { 0 }) * 1_000 + it.getOrElse(2) { 0 } }

android {
    namespace = "com.ajinkyabadve.kmmmywatchlist"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        targetSdk = 34

        applicationId = "com.ajinkyabadve.kmmmywatchlist.androidApp"
        versionCode = releaseVersionCode
        versionName = releaseVersionName
    }
    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/resources")
        resources.srcDirs("src/commonMain/resources")
    }
    signingConfigs {
        if (hasAndroidReleaseSigningConfig) {
            create("release") {
                storeFile = file(androidReleaseKeystorePath!!)
                storePassword = androidReleaseKeystorePassword
                keyAlias = androidReleaseKeyAlias
                keyPassword = androidReleaseKeyPassword
            }
        }
    }
    buildTypes {
        release {
            // The detail-screen-scroll-jank skill's own benchmark methodology already assumed "R8
            // optimization enabled" for a valid release-build measurement - this was previously
            // false (AGP's default), so every benchmark run against `assembleRelease`/
            // `installRelease` before 2026-09-12 measured an unminified build despite that.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig =
                if (hasAndroidReleaseSigningConfig) {
                    signingConfigs.getByName("release")
                } else {
                    // Debug-key fallback purely so `assembleRelease`/`installRelease` still produce
                    // an installable APK for local scroll-performance benchmarking when no release
                    // signing secrets are exported - Compose's own guidance is that Lazy layout
                    // performance can only be measured reliably in a non-debuggable build (debug
                    // builds carry extra composer/slot-table tracking that debug=true always
                    // installs regardless of minification). Not a real release credential - do not
                    // ship an APK signed this way.
                    signingConfigs.getByName("debug")
                }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Prints once when a person actually invokes a Release-variant output task without the release
// signing secrets exported, so the debug-signing benchmark fallback above can't be ship-forgotten:
// the debug key is not a real release credential, and an APK built with it cannot be uploaded as a
// Play Store update to the existing app (Play enforces the original signing key). Deliberately
// only the outward-facing tasks, not every internal Release-suffixed task in the dependency graph
// (dozens of those run per build).
if (!hasAndroidReleaseSigningConfig) {
    setOf("assembleRelease", "bundleRelease", "installRelease").forEach { taskName ->
        tasks.matching { it.name == taskName }.configureEach {
            doFirst {
                logger.warn(
                    "\n[!] '$taskName' is signed with the DEBUG key (see composeApp/build.gradle.kts) - " +
                        "for local benchmarking only. Do NOT distribute this APK/bundle as a real release.\n",
                )
            }
        }
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
            packageName = "MyWatchList"
            // dmg/msi installers require a plain X.Y.Z version - strips any "-dev.N"/"-rc1"
            // pre-release suffix `releaseVersionName` (see its declaration above) can carry.
            packageVersion = releaseVersionName.substringBefore('-')
            // jlink's default (jdeps-based) module detection misses java.sql - confirmed
            // 2026-08-26: a packaged .app (createDistributable/Dmg) crashed with
            // NoClassDefFoundError: java/sql/DriverManager the first time a screen actually ran a
            // SQLite query (JdbcSqliteDriver, desktopMain's DatabaseDriverFactory.kt), even though
            // `./gradlew :composeApp:run` - which uses the full system JDK, not a jlinked runtime -
            // never showed the problem. jdeps apparently doesn't trace sqlite-jdbc's reflective/
            // ServiceLoader-based use of java.sql.DriverManager deeply enough to include it.
            modules("java.sql")

            macOS {
                iconFile.set(project.file("../icons/desktop/icon.icns"))
                // Without this it defaults to packageName ("MyWatchList"), which isn't reverse-DNS.
                // macOS keys the app's preferences, notification permissions and TCC grants off
                // this identifier, so it needs to be stable and namespaced from the first release.
                bundleID = "com.ajinkyabadve.kmmmywatchlist"
            }
            windows {
                iconFile.set(project.file("../icons/desktop/icon.ico"))
                // jpackage builds an MSI with no shortcuts at all unless asked, which is why the
                // installed app never showed up in the Start menu.
                menu = true
                menuGroup = "MyWatchList"
                shortcut = true
                // Must stay constant across releases - WiX uses it to recognise an install as an
                // upgrade of this app rather than a second side-by-side copy. Changing it strands
                // the previously installed version (and its Start menu entry) on users' machines.
                upgradeUuid = "9C6EA41A-9CAD-4BCE-84CF-89F9BEDA4F46"
            }
            linux {
                iconFile.set(project.file("../icons/desktop/icon.png"))
                // Same story as Windows: jpackage's `shortcut` defaults to false, so the .deb
                // installs the app under /opt with no .desktop entry and it never appears in the
                // desktop environment's application menu.
                shortcut = true
                menuGroup = "MyWatchList"
            }
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
        create("MyDatabase") {
            packageName.set("com.ajinkyabadve.kmmmywatchlist.db")
            // Required for the JS target's WebWorkerDriver (async by nature) - a database can
            // only be all-sync or all-async, so every platform's generated Queries API is
            // suspend-based (`awaitAsList()` etc.) rather than blocking (`executeAsList()`).
            // https://sqldelight.github.io/sqldelight/2.3.2/js_sqlite/
            generateAsync.set(true)
            // Default dialect (sqlite_3_18) predates SQLite's `ON CONFLICT ... DO UPDATE` syntax,
            // which trackedMedia's upsert needs (SQLite added it in 3.24, generalised in 3.35).
            dialect(libs.sqlDelight.dialect.sqlite338)
            // NOT YET ENABLED: `verifyMigrations.set(true)` would diff every numbered `.sqm` file
            // (see `LocalSchemaVersion`'s kdoc) against `MyDatabase.sq` at build time - exactly the
            // safety net a real migration needs. Tried 2026-09-12 with zero `.sqm` files present
            // (nothing to verify yet) and `verifyCommonMainMyDatabaseMigration` failed outright:
            // "Verifying a migration requires a database file to be present... use the generate
            // schema Gradle task" - no such task exists in this SQLDelight version's default Gradle
            // task graph. Turn this on (and resolve that task-graph gap) when the first real `.sqm`
            // migration is added - don't ship it disabled forever.
        }
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

// Gradle's default test worker heap (512m) isn't enough once the suite has this many
// runComposeUiTest bodies - each stands up its own Compose/Skiko rendering surface, and running
// them all in one worker JVM was OOM-ing (java.lang.OutOfMemoryError from the AWT threads)
// partway through the run.
tasks.named<Test>("desktopTest") {
    maxHeapSize = "2g"
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
