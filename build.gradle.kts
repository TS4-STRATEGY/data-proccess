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

    if (env == "qa" || env == "prod") {
        implementation("org.springframework.boot:spring-boot-starter-web:2.6.14") {
            exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
            exclude(group = "org.apache.tomcat.embed", module = "tomcat-embed-websocket")
            exclude(group = "org.apache.tomcat.embed", module = "tomcat-embed-core")
            exclude(group = "org.apache.tomcat.embed", module = "tomcat-embed-el")
        }
    }

    // Dependencias específicas para Local
    if (env == "local") {
        implementation("org.springframework.boot:spring-boot-starter-web:2.6.14")
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
    archiveFileName.set("${project.name}-$env-$archiveVersion.war")
}

// Pasar el perfil de Spring Boot según el entorno
tasks.named<JavaExec>("bootRun") {
    systemProperty("spring.profiles.active", env)
}
