import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.panteleyev.jpackage.ImageType

val author = "Valentin Koeltgen"
val packageName = "koeltv"

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.panteleyev.jpackageplugin") version "1.5.1"
    kotlin("jvm") version "1.7.20"
    application
}

group = "com.$packageName"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.seleniumhq.selenium:selenium-java:4.8.0")
    implementation("io.github.bonigarcia:webdrivermanager:5.3.2")
    implementation(kotlin("reflect"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}

tasks {
    register("fatJar", Jar::class.java) {
        //Set extension to specify name
        archiveClassifier.set("standalone")
        //Handle duplicates
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        //Precise the class containing the main function
        manifest {
            attributes("Main-Class" to application.mainClass)
        }
        //Add the dependencies
        from(configurations.runtimeClasspath.get()
            .onEach { println("add from dependencies: ${it.name}") }
            .map { if (it.isDirectory) it else zipTree(it) })
        //Add the local classes
        val sourcesMain = sourceSets.main.get()
        sourcesMain.allSource.forEach { println("add from sources: ${it.name}") }
        from(sourcesMain.output)

        //TODO Make it work correctly, currently replaced by shadowJar
    }
}

tasks {
    register("version") {
        doLast {
            println("v$version")
        }
    }
}

tasks.jpackage.configure {
    dependsOn("shadowJar")

    if(project.hasProperty("type")) {
        type = ImageType.valueOf(project.property("type").toString().toUpperCase())
    }

    appName = application.applicationName
    vendor = author
    appVersion = version.toString()
    icon = "./resources/download.ico"
    input = "./build/libs/"
    destination = "./build/jpackage"
    mainJar = "${application.applicationName}-$version-all.jar"

    //Windows options
    winConsole = true
    winDirChooser = true
    winShortcut = true
    winMenu = true
    winMenuGroup = packageName

    //Linux options
    linuxMenuGroup = packageName
    linuxShortcut = true
}