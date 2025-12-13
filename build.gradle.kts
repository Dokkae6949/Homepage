import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.github.klahap.dotenv.DotEnv
import io.github.klahap.dotenv.DotEnvBuilder
import org. gradle.api.JavaVersion.VERSION_21
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import kotlin.io.path.Path

// ====================================================================================================
// ENVIRONMENT CONFIGURATION
// ====================================================================================================

val env = if (File("${layout.projectDirectory.asFile.absolutePath}/.env").exists()) {
    DotEnvBuilder.dotEnv {
        addFile("${layout.projectDirectory}/.env")
        addSystemEnv()
    }
} else {
    DotEnvBuilder.dotEnv {
        addSystemEnv()
    }
}

val envDbUrl: String = env["DB_URL"] ?: ""
val envDbUsername: String = env["DB_USERNAME"] ?: ""
val envDbPassword: String = env["DB_PASSWORD"] ?: ""

// ====================================================================================================
// PLUGIN CONFIGURATION
// ====================================================================================================

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    alias(libs.plugins.dotenv.plugin)
    alias(libs.plugins.jte)
    alias(libs.plugins.tasktree)
    alias(libs.plugins.jooq.codegen.gradle)
    alias(libs.plugins.flyway)
}

// ====================================================================================================
// BASIC CONFIGURATION
// ====================================================================================================

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

java {
    sourceCompatibility = VERSION_21
    targetCompatibility = VERSION_21
}

repositories {
    mavenCentral()
}

// ====================================================================================================
// GENERATED CODE DIRECTORIES
// ====================================================================================================

val generatedResourcesDir = layout.buildDirectory.dir("generated-resources")
val generatedSourcesDir = layout.buildDirectory.dir("generated-src")
val migrationSourceDir = layout.projectDirectory.dir("src/main/resources/db/migration")
val jtwSourceDir = layout.projectDirectory.dir("src/main/kte")
val jteOutputDir = generatedResourcesDir.get().dir("jte")
val jooqOutputDir = generatedSourcesDir.get().dir("jooq")

sourceSets {
    main {
        resources.srcDir(jteOutputDir)
        kotlin.srcDir(jooqOutputDir)
    }
}

// ====================================================================================================
// DEPENDENCIES
// ====================================================================================================

dependencies {
    // HTTP4K
    implementation(platform(libs.http4k.bom))
    implementation(libs.bundles.http4k)

    // Environment & Configuration
    implementation(libs.dotenv)

    // Templating
    implementation(libs.jte.kotlin)

    // Database
    implementation(libs.bundles.database)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)

    // Testing
    testImplementation(libs.bundles.testing)

    // Jooq Codegen
    jooqCodegen(libs.jooq.meta)
    jooqCodegen(libs.jooq.meta.extensions)
    jooqCodegen(libs.jooq.postgres)
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(libs.jooq.codegen)
        classpath(libs.jooq.meta)
        classpath(libs.jooq.meta.extensions)
        classpath(libs.flyway.database.postgresql)
    }
}

// ====================================================================================================
// JTE TEMPLATE GENERATION
// ====================================================================================================

jte {
    sourceDirectory.set(Path(jtwSourceDir.asFile.absolutePath))
    targetDirectory.set(Path(jteOutputDir.asFile.absolutePath))
    precompile()
}

tasks.named("precompileJte") {
    dependsOn("compileKotlin")
}

tasks.register("genJte") {
    group = "codegen"
    description = "Precompile jte template into classes"

    dependsOn("precompileJte")
}

// ====================================================================================================
// JOOQ CODE GENERATION FROM SQL FILES
// ====================================================================================================

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
                directory = jooqOutputDir.asFile.absolutePath
            }

            strategy {
                name = "org.jooq.codegen.DefaultGeneratorStrategy"
            }
        }
    }
}

tasks.register("genJooq") {
    group = "codegen"
    description = "Generate jooq classes from migrations"

    dependsOn("jooqCodegen")
}

// ====================================================================================================
// FLYWAY MIGRATE AND CODEGEN TASK
// ====================================================================================================

flyway {
    url = envDbUrl
    user = envDbUsername
    password = envDbPassword
    locations = arrayOf("filesystem:${migrationSourceDir.asFile.absolutePath}")
    baselineOnMigrate = true
    validateMigrationNaming = true
}

tasks.register("migrate") {
    group = "codegen"
    description = "Run Flyway migrations and generate JOOQ code (no compilation)"

    dependsOn("flywayMigrate")
    finalizedBy("jooqCodegen")

    doFirst {
        logger.lifecycle("╔═══════════════════════════════════════════════════════════════╗")
        logger.lifecycle("║  Running Migrations and Code Generation                       ║")
        logger.lifecycle("╚═══════════════════════════════════════════════════════════════╝")
        logger.lifecycle("| Database URL: $envDbUrl")
        logger.lifecycle("| Migrations:  ${migrationSourceDir.asFile.absolutePath}")
        logger.lifecycle("| Username: $envDbUsername")
        logger.lifecycle("| Password: ${if (envDbUsername.isEmpty()) "not " else ""}provided")
    }

    doLast {
        logger.lifecycle("✓ Migration and code generation completed")
    }
}

// ====================================================================================================
// COMPILATION ORDER
// ====================================================================================================

tasks {
    withType<KotlinJvmCompile>().configureEach {
        dependsOn("genJooq")

        compilerOptions {
            allWarningsAsErrors = false
            jvmTarget.set(JVM_21)
            freeCompilerArgs.add("-Xjvm-default=all")
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }
}

// ====================================================================================================
// JAR BUILDING
// ====================================================================================================

tasks.named<ShadowJar>("shadowJar") {
    manifest {
        attributes("Main-Class" to "at.dokkae.homepage.HomepageKt")
    }

    dependsOn("genJte", "genJooq")

    from(jteOutputDir)

    archiveFileName.set("app.jar")

    mergeServiceFiles()

    exclude(
        "META-INF/*. RSA",
        "META-INF/*.SF",
        "META-INF/*.DSA"
    )
}

tasks.named("build") {
    dependsOn("shadowJar")
}

// ====================================================================================================
// HELPER TASKS
// ====================================================================================================

tasks.register("cleanGenerated") {
    group = "build"
    description = "Clean all generated code"

    doLast {
        delete(generatedResourcesDir)
        delete(generatedSourcesDir)
        logger.lifecycle("✓ Cleaned generated code directories")
    }
}

tasks.named("clean") {
    dependsOn("cleanGenerated")
}