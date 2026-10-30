import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.intellij.platform)
}

group = "br.com.weslleycampos.simpanel"
version = "0.1.0"

kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        local(providers.gradleProperty("androidStudioPath"))
    }
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test)
    testRuntimeOnly(libs.junit.launcher)
}

intellijPlatform {
    buildSearchableOptions = false
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "262"
            untilBuild = "262.*"
        }
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
