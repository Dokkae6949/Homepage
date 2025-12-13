import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.github.klahap.dotenv.DotEnvBuilder
import org.flywaydb.gradle.task.FlywayMigrateTask
import org.gradle.api.JavaVersion.VERSION_21
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import kotlin.io.path.Path

val env = DotEnvBuilder.dotEnv {
    addFile("$rootDir/.env")
    addSystemEnv()
}

val envDbUrl: String = env["DB_URL"] ?: ""
val envDbMigration: String = env["DB_MIGRATIONS"] ?: "src/main/resources/db/migration"
val envDbUsername: String = env["DB_USERNAME"] ?: ""
val envDbPassword: String = env["DB_PASSWORD"] ?: ""

val generatedResourcesDirectory = "${layout.buildDirectory.get()}/generated-resources"
val generatedSourcesDirectory = "${layout.buildDirectory.get()}/generated-src"

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    alias(libs.plugins.dotenv.plugin)
    alias(libs.plugins.jte)
    alias(libs.plugins.flyway)
    alias(libs.plugins.jooq.codegen.gradle)
    alias(libs.plugins.taskinfo)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        classpath(libs.postgresql)
        classpath(libs.flyway.database.postgresql)
    }
}

sourceSets.main {
    resources.srcDir("$generatedResourcesDirectory/jte")
    kotlin.srcDir("$generatedSourcesDirectory/jooq")
}

tasks {
    withType<KotlinJvmCompile>().configureEach {
        dependsOn("jooqCodegen")

        compilerOptions {
            allWarningsAsErrors = false
            jvmTarget.set(JVM_21)
            freeCompilerArgs.add("-Xjvm-default=all")
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }

    withType<FlywayMigrateTask> {
        dependsOn("initDb")
    }

    named("precompileJte") {
        dependsOn("compileKotlin")
    }

    named<ShadowJar>("shadowJar") {
        manifest {
            attributes("Main-Class" to "at.dokkae.homepage.HomepageKt")
        }

        dependsOn("precompileJte")

        mustRunAfter("flywayMigrate", "jooqCodegen")

        from("$generatedResourcesDirectory/jte")

        archiveFileName.set("app.jar")

        exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    }

    register("buildDocker") {

    }

    java {
        sourceCompatibility = VERSION_21
        targetCompatibility = VERSION_21
    }
}

dependencies {
    implementation(platform(libs.http4k.bom))

    implementation(libs.dotenv)

    implementation(libs.bundles.http4k)
    implementation(libs.jte.kotlin)
    implementation(libs.bundles.database)

    testImplementation(libs.bundles.testing)

    jooqCodegen(libs.jooq.meta)
    jooqCodegen(libs.jooq.postgres)
}

// ========== JTE Templating ==========
jte {
    sourceDirectory.set(Path("src/main/kte"))
    targetDirectory.set(Path("$generatedResourcesDirectory/jte"))
    precompile()
}

// ========== FlyWay ==========
flyway {
    url = envDbUrl
    user = envDbUsername
    password = envDbPassword
    locations = arrayOf("filesystem:$envDbMigration")
    baselineOnMigrate = true
    validateMigrationNaming = true
}

tasks.register("initDb") {
    doFirst {
        println("Database Configuration:")
        println("  Raw URL from env: $envDbUrl")
        println("  Resolved URL: $envDbUrl")
        println("  Migrations: $envDbMigration")
        println("  Credentials:")
        println("    Username: $envDbUsername")
        println("    Password: ${"*".repeat(envDbPassword.length)}")
    }
}

tasks.named("flywayMigrate") {
    finalizedBy("jooqCodegen")
}

// ========== Jooq ==========
jooq {
    configuration {
        logging = org.jooq.meta.jaxb.Logging.WARN

        jdbc {
            driver = "org.postgresql.Driver"
            url = envDbUrl
            user = envDbUsername
            password = envDbPassword
        }

        generator {
            name = "org.jooq.codegen.KotlinGenerator"

            database {
                name = "org.jooq.meta.postgres.PostgresDatabase"
                inputSchema = "public"

                // SQLite specific configuration
                includes = ".*"
                excludes = """
                            flyway_.*|
                            pg_.*|
                            information_schema.*
                        """.trimMargin().replace("\n", "")
            }

            generate {
                // Recommended settings for Kotlin
                isDeprecated = false
                isRecords = true
                isImmutablePojos = true
                isFluentSetters = true
                isKotlinNotNullRecordAttributes = true
                isKotlinNotNullPojoAttributes = true
                isKotlinNotNullInterfaceAttributes = true
                isPojosAsKotlinDataClasses = true
            }

            target {
                packageName = "at.dokkae.homepage.generated.jooq"
                directory = "$generatedSourcesDirectory/jooq"
            }

            strategy {
                name = "org.jooq.codegen.DefaultGeneratorStrategy"
            }
        }
    }
}