plugins {
    kotlin("jvm") version "1.6.21" // Última versión compatible con Java 8
    kotlin("plugin.spring") version "1.6.21"
    id("org.springframework.boot") version "2.6.14" // Versión de Spring Boot compatible con Java 8
    id("io.spring.dependency-management") version "1.0.14.RELEASE"
    id("war")
}

group = "com.bestool"
version = "0.0.1"

// Leer el entorno desde la línea de comandos o usar "local" como predeterminado
// Variable para identificar el entorno
val env: String = project.findProperty("env")?.toString() ?: "local"
println("Building for environment: $env")

tasks.register("generateBuildConfig") {
    val outputDir = file("src/main/kotlin/com/bestool/dataprocessor")
    doLast {
        val buildConfigFile = file("$outputDir/BuildConfig.kt")
        buildConfigFile.parentFile.mkdirs()

        val mainPath = when (env) {
            "dev" -> "${System.getProperty("user.home")}/Downloads/DATA"
            "qa" -> "/u01/ArchivosBestools"
            "prod" -> "/u01/oracle/ArchivosBestools"
            else -> "/tmp/oracle/apps/bestool"
        }

        val logPath = when (env) {
            "dev" -> "/tmp/oracle/apps/bestool"
            "qa" -> "/u01/oracle/apps/bestool"
            "prod" -> "/u01/oracle/apps/bestool"
            else -> "/var/log/bestool"
        }



        buildConfigFile.writeText(
            """
            package com.bestool.dataprocessor

            object BuildConfig {
                const val ACTIVE_PROFILE = "$env"
                const val MAIN_PATH = "$mainPath"
                const val SCHEDULE_ENABLE = true
                const val LOG_PATH = "$logPath"
            }
            """.trimIndent()
        )

        println("✅ BuildConfig.kt generado en: ${buildConfigFile.absolutePath}")
    }
}

// Agregar la carpeta generada al compilador de Kotlin
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    dependsOn("generateBuildConfig")
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

// Asegurar que el código generado se incluya en el classpath
tasks.register("setupSourceSet") {
    doLast {
        val generatedSrc = "$buildDir/generated/sources/buildConfig/kotlin"
        println("📂 Agregando ruta de fuentes: $generatedSrc")
        project.extensions.extraProperties["generatedSrc"] = generatedSrc
    }
}

// Incluir el código generado en `sourceSets`
tasks.withType<JavaCompile> {
    dependsOn("setupSourceSet")
    doFirst {
        val generatedSrc = project.extensions.extraProperties["generatedSrc"] as String
        options.compilerArgs.add("-sourcepath")
        options.compilerArgs.add(generatedSrc)
    }
}


java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}



dependencies {

    implementation("org.springframework.boot:spring-boot-starter-web:2.6.14") {
        if (env == "qa" || env == "prod") {
            exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
            exclude(group = "org.apache.tomcat.embed", module = "tomcat-embed-websocket")
            exclude(group = "org.apache.tomcat.embed", module = "tomcat-embed-core")
            exclude(group = "org.apache.tomcat.embed", module = "tomcat-embed-el")
        }
    }

    implementation("org.springdoc:springdoc-openapi-ui:1.6.14")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa:2.6.14")
    implementation("org.hibernate:hibernate-core:5.6.15.Final")
    implementation("com.oracle.database.jdbc:ojdbc8")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("commons-fileupload:commons-fileupload:1.5")
    implementation("org.springframework.boot:spring-boot-starter-batch:2.6.14")
    implementation("org.springframework.boot:spring-boot-starter-actuator:2.6.14")

    runtimeOnly("com.oracle.database.jdbc:ucp")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8" // Configura el objetivo de JVM para Kotlin
        freeCompilerArgs = listOf("-Xjsr305=strict") // Configuración estricta de Null-safety
    }
}

// Configuración dinámica del nombre del WAR
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootWar>("bootWar") {
    archiveFileName.set("${project.name}-$env-${project.version}.war")
}

// Pasar el perfil de Spring Boot según el entorno
tasks.named<JavaExec>("bootRun") {
    systemProperty("spring.profiles.active", env)
}

tasks.withType<ProcessResources> {
    filteringCharset = "UTF-8"
    inputs.property("env", env)
    inputs.property("user.home", System.getProperty("user.home"))
    filesMatching("application.properties") {
        expand(
            "env" to env,
            "user.home" to System.getProperty("user.home")
        )
    }
    filesMatching("WEB-INF/web.xml") {
        expand("springProfile" to env)
    }
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootWar>("bootWar") {
    from("src/main/webapp") {
        include("web.xml") // Incluye el archivo expandido
        into("WEB-INF") // Asegura que vaya en la ruta correcta dentro del WAR
    }
}

