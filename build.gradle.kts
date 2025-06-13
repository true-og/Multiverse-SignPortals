import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.api.tasks.bundling.Jar

plugins {
    `java-library`
    id("eclipse")
    id("maven-publish")
    id("checkstyle")
    id("com.gradleup.shadow") version "8.3.5"
}

version = System.getenv("GITHUB_VERSION") ?: "1.19.4"
group = "com.onarandombox.multiversesignportals"
description = "Multiverse-SignPortals"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "spigot"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    maven {
        name = "onarandombox"
        url = uri("https://repo.onarandombox.com/content/groups/public/")
    }
}

dependencies {
    implementation("org.bukkit:bukkit:1.13.2-R0.1-SNAPSHOT") {
        exclude(group = "junit", module = "junit")
    }
    implementation("com.onarandombox.multiversecore:Multiverse-Core:4.2.2")
    api("com.dumptruckman.minecraft:Logging:1.1.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).encoding = "UTF-8"
}

configurations {
    val apiElements by getting
    val runtimeElements by getting
    listOf(apiElements, runtimeElements).forEach { cfg ->
        cfg.outgoing.artifacts.removeIf { a ->
            a.buildDependencies.getDependencies(null).contains(tasks.named("jar").get())
        }
        cfg.outgoing.artifact(tasks.named("shadowJar"))
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Multiverse/Multiverse-SignPortals")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
    outputs.upToDateWhen { false }
}

checkstyle {
    toolVersion = "6.1.1"
    configFile = file("config/mv_checks.xml")
    isIgnoreFailures = true
}

tasks.named<Javadoc>("javadoc") {
    source = sourceSets["main"].allJava
    classpath = configurations.compileClasspath.get()
}

tasks.named<ShadowJar>("shadowJar") {
    relocate("com.dumptruckman.minecraft.util.Logging", "com.onarandombox.MultiverseSignPortals.util.MVSPLogging")
    relocate("com.dumptruckman.minecraft.util.DebugLog", "com.onarandombox.MultiverseSignPortals.util.DebugFileLogger")
    configurations = listOf(project.configurations.getByName("compileClasspath"))
    archiveFileName.set("${project.name}-${project.version}.jar")
}

tasks.named("build") {
    dependsOn("shadowJar")
}

tasks.named<Jar>("jar") {
    enabled = false
}

