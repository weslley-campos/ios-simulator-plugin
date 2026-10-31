import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.intellij.platform)
}

group = "br.com.weslleycampos.simpanel"
version = "0.1.0"

// Builds with JDK 25 but emits Java 21 bytecode: Android Studio runs on Java 21 before 2026.1.
kotlin {
    jvmToolchain(25)
}

// Set on the tasks, as the IntelliJ Platform plugin sets the target to the IDE's Java otherwise.
// The API version keeps to the Kotlin library that Android Studio 2025.1 bundles, 2.1.
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        apiVersion = KotlinVersion.KOTLIN_2_1
        freeCompilerArgs.add("-Xjdk-release=21")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Builds against the Android Studio at androidStudioPath when it's installed there. Elsewhere,
// such as on CI, it downloads the release that androidStudioVersion names.
val androidStudioPath = providers.gradleProperty("androidStudioPath").orNull
    ?.takeIf { it.isNotBlank() && file(it).isDirectory }

dependencies {
    intellijPlatform {
        if (androidStudioPath != null) {
            local(androidStudioPath)
        } else {
            androidStudio(providers.gradleProperty("androidStudioVersion"))
        }
    }
    // Compiles the serializers against the oldest supported IDE's library; the IDE provides it at run time.
    compileOnly(libs.kotlinx.serialization.json)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test)
    testRuntimeOnly(libs.junit.launcher)
}

intellijPlatform {
    buildSearchableOptions = false
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
            untilBuild = provider { null }
        }
    }
    // The Android Studio releases verifyPlugin checks the plugin against, as a comma-separated
    // verifyAndroidStudioVersions in gradle.properties.
    pluginVerification {
        // Not INTERNAL_API_USAGES: on 2025.1 it flags the bridges Kotlin generates to
        // ToolWindowFactory's default methods, which the plugin never calls or overrides.
        failureLevel = listOf(
            VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS,
            VerifyPluginTask.FailureLevel.INVALID_PLUGIN,
        )
        ides {
            providers.gradleProperty("verifyAndroidStudioVersions").get()
                .split(",").map(String::trim).filter(String::isNotEmpty)
                .forEach { create(IntelliJPlatformType.AndroidStudio, it) }
        }
    }
    // CI passes these as secrets for release tags; signPlugin and publishPlugin need them.
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

val helperDir = layout.projectDirectory.dir("../helper")

val buildHelper = tasks.register<Exec>("buildHelper") {
    description = "Builds the Swift helper the plugin starts for each simulator."
    workingDir(helperDir)
    commandLine("swift", "build", "-c", "release", "--product", "simpanel-helper")
    inputs.file(helperDir.file("Package.swift"))
    inputs.dir(helperDir.dir("Sources"))
    outputs.file(helperDir.file(".build/release/simpanel-helper"))
}

tasks.withType<PrepareSandboxTask>().configureEach {
    from(buildHelper) {
        into(pluginName.map { "$it/bin" })
    }
    // The sync deletes what it didn't copy: the plugins installed in the sandbox IDE, such as
    // Kotlin Multiplatform, whose frameworks it breaks halfway before failing on their symlinks.
    // Only this plugin and the frontend folder the task makes for split mode are its own.
    preserve {
        include("**")
        exclude("${pluginName.get()}/**", "frontend/**")
    }
}

tasks {
    test {
        dependsOn(buildHelper)
        useJUnitPlatform()
        systemProperty("simpanel.helper", helperDir.file(".build/release/simpanel-helper").asFile.path)
        environment("SIMPANEL_UDID", providers.environmentVariable("SIMPANEL_UDID").getOrElse(""))
    }
    runIde {
        systemProperty("disable.android.first.run", "true")
        systemProperty("idea.trust.all.projects", "true")
        systemProperty("jb.consents.confirmation.enabled", "false")
        systemProperty("ide.show.tips.on.startup.default.value", "false")
    }
}
