import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.ByteArrayOutputStream
import java.time.Clock
import java.time.Instant
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.wire)
}

apply(from = "${rootProject.projectDir}/signing.gradle")

wire {
    sourcePath {
        srcDir("src/main/protos")
    }

    kotlin {
        android = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.Jeaceny.tieba.post"
        minSdk = libs.versions.minSdk.get().toInt()
        //noinspection OldTargetApi
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 391033
        versionName = "4.0.0 Beta 5.2"
        // Configure custom runner to set up the Hilt test application.
        // The runner class lives under the namespace, which is independent of applicationId.
        testInstrumentationRunner = "com.huanchengfly.tieba.post.TbLiteTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        resourceConfigurations.addAll(listOf("en", "zh-rCN"))
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        val epochSecond = Instant.now(Clock.systemUTC()).epochSecond
        val gitVersionProvider = providers.of(GitVersionValueSource::class) {}
        val gitVersion = gitVersionProvider.get()

        all {
            // Apply signing config from signing.properties, see ../signing.gradle
            signingConfigs.findByName("config")?.let { signingConfig = it }
            // Replaced with buildConfigField#BUILD_GIT
            vcsInfo.include = false
            buildConfigField("String", "BUILD_GIT", "\"${gitVersion}\"")
            buildConfigField("long", "BUILD_TIME", "${epochSecond}L")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "compose-rules.pro",
                "proguard-rules.pro"
            )
            isDebuggable = false
            isJniDebuggable = false
            multiDexEnabled = true
        }

        // Variant for benchmark, auto-selected by Macrobenchmark
        create("benchmarkRelease") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            applicationIdSuffix = ".benchmark"
            isProfileable = true
            proguardFile("benchmark-rules.pro")
        }

        // Variant for composition tracing, auto-selected by benchmarkComposeTracing in Macrobenchmark
        create("composeTracing") {
            initWith(getByName("benchmarkRelease"))
            proguardFiles.removeIf { it.name == "compose-rules.pro" } // Keep tracing API
        }

        // Variant for CI, profileable
        create("ci") {
            initWith(getByName("benchmarkRelease"))
            applicationIdSuffix = ".ci"
        }
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += listOf(
            "META-INF/**",
            "kotlin-tooling-metadata.json", // Unneeded: See KT-48019
            "**.bin"
        )
    }

    lint {
        disable.addAll(listOf("LocalContextGetResourceValueCall", "UseKtx"))
        checkReleaseBuilds = false
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
    }

    sourceSets {
        getByName("composeTracing") {
            assets.directories.add("src/benchmarkRelease/assets")
        }
    }

    namespace = "com.huanchengfly.tieba.post"
}

// The **release** build ships under its own application id.
//
// AGP 9 dropped `applicationId` from the build-type DSL (it only exists on the variant now), so the id
// is set through the variant API. This changes the *installed* package only — `namespace` above stays
// com.huanchengfly.tieba.post, so the R class, the manifest's relative component names
// (".MainActivityV2"), the `${applicationId}` authorities and the launcher-alias ComponentNames
// (built from `context.packageName`) are all unaffected.
//
// Only `release` is touched on purpose: debug keeps com.huanchengfly.tieba.post (the two can be
// installed side by side), and the benchmark / composeTracing / ci variants keep the ids their
// `applicationIdSuffix` produces from the default config.
// ── Release versioning ────────────────────────────────────────────────────────────────────────────
//
// `version.properties` (project root) is the single source of truth for what a **release** APK
// reports: the name ships as-is, the code ships as-is and is then bumped once the APK has actually
// been packaged, so every packaged release gets its own increasing code. Debug and the
// benchmark/ci variants keep the `defaultConfig` values.
//
// The file is read through `providers.fileContents`, which makes it a tracked build input: after the
// bump below, the next build sees the new code instead of a cached one.
// Both readers inline their parsing, and the bump action below captures only plain locals: a lambda
// referencing a script-level function or val carries a reference to the build script itself, which the
// configuration cache cannot serialize — every AGP task that reads the variant's version then fails
// with "cannot serialize Gradle script object references".
val releaseVersionFile = rootProject.layout.projectDirectory.file("version.properties")

val releaseVersionName = providers.fileContents(releaseVersionFile).asText.map { text ->
    Properties().apply { load(text.reader()) }
        .getProperty("versionName")
        ?.trim()
        .orEmpty()
        .ifEmpty { "1.0" }
}

val releaseVersionCode = providers.fileContents(releaseVersionFile).asText.map { text ->
    Properties().apply { load(text.reader()) }
        .getProperty("versionCode")
        ?.trim()
        ?.toIntOrNull()
        ?: 1
}

androidComponents {
    onVariants { variant ->
        if (variant.buildType == "release") {
            variant.applicationId.set("com.Jeaceny.tieba.post")
            variant.outputs.forEach { output ->
                output.versionName.set(releaseVersionName)
                output.versionCode.set(releaseVersionCode)
            }
        }
    }
}

// Advance the stored code once a release APK has really been written. `doLast` runs only when the
// task executes, so an up-to-date rebuild never skips a code, and packaging the benchmark/ci variants
// (`packageBenchmarkRelease` / `packageCi`) never touches this file.
tasks.matching { it.name == "packageRelease" }.configureEach {
    // Captured by value — a real File plus literals — so the action serializes under the configuration
    // cache; touching `project` or script members inside `doLast` would not.
    val versionFile = rootProject.layout.projectDirectory.file("version.properties").asFile
    doLast {
        val properties = Properties()
        versionFile.inputStream().use { properties.load(it) }
        val name = properties.getProperty("versionName")?.trim().orEmpty().ifEmpty { "1.0" }
        val nextCode = (properties.getProperty("versionCode")?.trim()?.toIntOrNull() ?: 0) + 1
        versionFile.writeText(
            buildString {
                appendLine("# Release versioning — the single source of truth for what a release APK reports.")
                appendLine("#")
                appendLine("# versionName: shipped as-is by release builds.")
                appendLine("# versionCode: shipped as-is, then bumped by :app:packageRelease once an APK has actually")
                appendLine("#              been packaged, so every release APK gets its own increasing code.")
                appendLine("versionName=$name")
                appendLine("versionCode=$nextCode")
            }
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs.addAll(
            "-Xcontext-parameters",
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=androidx.compose.ui.ExperimentalMediaQueryApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ComponentOverrideApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }
}

configurations.configureEach {
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk7")
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")

    stabilityConfigurationFiles.addAll(
        rootProject.layout.projectDirectory.file("compose_stability_configuration.txt")
    )
}

dependencies {
    //Local Files
//    implementation fileTree(include: ["*.jar"], dir: "libs")

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.jetbrains.annotations)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.collections.immutable)
    // Required by Navigation Type-Safe
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.lottie)
    implementation(libs.lottie.compose)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui.compose)

    api(libs.wire.runtime)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.navigation.compose)

    // Compose Accompanist
    implementation(libs.accompanist.drawablepainter)
    implementation(project(":material-color-utilities"))
    implementation(project(":placeholder"))

    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.animation.graphics)

    // Material compose
    implementation(libs.bundles.compose.md3)
    implementation(libs.androidx.compose.material.iconsCore)
    // Optional - Add full set of material icons
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.runtime.tracing)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Miuix (HyperOS design language); calls are funneled through ui/miuix adapter layer
    implementation(libs.miuix.ui)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.icons)
    // Progressive blur top bars; library minSdk is 33, usage is gated by
    // isRuntimeShaderSupported() so API 24-32 devices fall back to an opaque bar
    implementation(libs.miuix.blur)
    // Continuous corners for every rounded surface in the app (cards, chips, thumbs, bars).
    implementation(libs.miuix.squircle)
    // Kyant0 AndroidLiquidGlass backdrop: liquid-glass bottom bar with rememberCombinedBackdrop
    // multi-layer capture (page + accent icon copy). AGSL-based; gated by
    // isRuntimeShaderSupported() so API 24-32 devices fall back to the flat bar.
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.viewModel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)

    //AndroidX
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.window)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.tracing)
    implementation(libs.bundles.paging3)

    // FastCSV
    implementation(libs.fastcsv)

    // WorkManager
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime)
    androidTestImplementation(libs.androidx.work.testing)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    androidTestImplementation(libs.androidx.room.testing)

    implementation(libs.google.gson)


    // Coil
    implementation(libs.bundles.coil3)

    // Image Viewer (Compose: pager + zoomable/telephoto, replaces imageviewer + subsampling-scale)
    implementation(libs.telephoto.zoomable.image.coil3)

    implementation(libs.squareup.okhttp3)
    implementation(libs.squareup.retrofit2)
    implementation(libs.squareup.retrofit2.wire)

    implementation(libs.liyujiang.oadi)

    implementation(libs.yalantis.ucrop)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)

    // UI Tests
    androidTestImplementation(libs.androidx.compose.ui.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Startup Profile & Baseline Profile
    // baselineProfile(project(":macrobenchmark"))
}

abstract class GitVersionValueSource : ValueSource<String, ValueSourceParameters.None> {
    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String = ByteArrayOutputStream().use { output ->
        execOperations.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            standardOutput = output
        }
        output.toString().trim()
    }
}