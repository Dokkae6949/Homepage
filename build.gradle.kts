import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.JavaVersion.VERSION_21
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import kotlin.io.path.Path

plugins {
    kotlin("jvm") version "2.2.20"

    id("gg.jte.gradle") version "3.2.1"
    id("com.gradleup.shadow") version "9.3.0"
}

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

jte {
    sourceDirectory.set(Path("src/main/kte"))
    targetDirectory.set(Path("${layout.buildDirectory.get()}/classes/jte"))

    precompile()
}

sourceSets.main {
    resources.srcDir("${layout.buildDirectory.get()}/classes/jte")
}

repositories {
    mavenCentral()
}

tasks {
    withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            allWarningsAsErrors = false
            jvmTarget.set(JVM_21)
            freeCompilerArgs.add("-Xjvm-default=all")
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }

    named<ShadowJar>("shadowJar") {
        manifest {
            attributes("Main-Class" to "at.dokkae.homepage.HomepageKt")
        }

        dependsOn("precompileJte")

        from("${layout.buildDirectory.get()}/classes/jte")

        archiveFileName.set("app.jar")

        exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    }

    java {
        sourceCompatibility = VERSION_21
        targetCompatibility = VERSION_21
    }
}

dependencies {
    implementation(platform("org.http4k:http4k-bom:6.23.1.0"))
    implementation("org.http4k:http4k-client-okhttp")
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-server-jetty")
    implementation("org.http4k:http4k-template-jte")
    implementation("org.http4k:http4k-web-htmx")
    implementation("gg.jte:jte-kotlin:3.2.1")
    testImplementation("org.http4k:http4k-testing-approval")
    testImplementation("org.http4k:http4k-testing-hamkrest")
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:6.0.0")
    testImplementation("org.junit.platform:junit-platform-launcher:6.0.0")
}

