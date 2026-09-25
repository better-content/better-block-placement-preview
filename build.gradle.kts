plugins {
    id("net.minecraftforge.gradle") version "6.0.54"
    id("org.spongepowered.mixin") version "0.7.38"
    jacoco
}

val minecraftVersion = property("minecraft_version") as String
val forgeVersion = property("forge_version") as String
val modId = property("mod_id") as String
val modName = property("mod_name") as String
val modVersion = property("mod_version") as String

group = "com.bettercontent"
version = modVersion
base { archivesName.set(property("artifact_name") as String) }

java { toolchain.languageVersion.set(JavaLanguageVersion.of(17)); withSourcesJar() }

repositories {
    mavenCentral()
    maven("https://maven.minecraftforge.net")
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://www.cursemaven.com") { content { includeGroup("curse.maven") } }
}

minecraft {
    mappings("official", minecraftVersion)
    copyIdeResources = true
    runs {
        configureEach {
            workingDirectory(project.file("run"))
            property("forge.logging.console.level", "info")
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", file("build/createSrgToMcp/output.srg").absolutePath)
            mods { create(modId) { source(sourceSets.main.get()) } }
        }
        create("client")
        create("server") { arg("--nogui") }
        create("gameTestServer") {
            workingDirectory(project.file("run-gametest"))
            property("forge.enableGameTest", "true")
            property("forge.gameTestServer", "true")
            property("forge.enabledGameTestNamespaces", "$modId,minecraft")
            arg("--nogui")
        }
    }
}

dependencies {
    minecraft("net.minecraftforge:forge:$minecraftVersion-$forgeVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    compileOnly(fg.deobf("curse.maven:realistic-block-physics-375616:6393411"))
    runtimeOnly(fg.deobf("curse.maven:realistic-block-physics-375616:6393411"))
    compileOnly(fg.deobf("curse.maven:realistic-physics-1030082:6026115"))
    runtimeOnly(fg.deobf("curse.maven:realistic-physics-1030082:6026115"))
    compileOnly(fg.deobf("curse.maven:rotavision-1535985:8715474"))
    runtimeOnly(fg.deobf("curse.maven:rotavision-1535985:8715474"))
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    compileOnly(annotationProcessor("io.github.llamalad7:mixinextras-common:0.5.4")!!)
    implementation(jarJar("io.github.llamalad7:mixinextras-forge:[0.5.4,0.6.0)")!!)
}

tasks.named<Jar>("jar") { finalizedBy("reobfJar") }
val stageRuntimeJar by tasks.registering(Copy::class) {
    dependsOn(tasks.named("reobfJar"))
    from(layout.buildDirectory.file("reobfJar/output.jar"))
    into(layout.buildDirectory.dir("libs"))
    rename { "${base.archivesName.get()}-$version.jar" }
}
tasks.named("assemble") { dependsOn(stageRuntimeJar) }
tasks.processResources {
    val props = mapOf("mod_id" to modId, "mod_name" to modName, "mod_version" to modVersion, "minecraft_version" to minecraftVersion, "forge_version" to forgeVersion)
    inputs.properties(props)
    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) { expand(props) }
}
tasks.withType<Test>().configureEach { useJUnitPlatform(); finalizedBy("jacocoTestReport") }
jacoco { toolVersion = "0.8.12" }
tasks.jacocoTestReport { dependsOn(tasks.test); reports { xml.required.set(true); html.required.set(true) } }
tasks.register("headlessGameTest") { group = "verification"; dependsOn(tasks.named("runGameTestServer")) }
val syncGameTestStructures by tasks.registering(Copy::class) { from("src/main/resources/gameteststructures"); into("run-gametest/gameteststructures") }
tasks.matching { it.name.startsWith("prepareRunGameTestServer") }.configureEach { dependsOn(syncGameTestStructures) }
tasks.register("verifyFast") { group = "verification"; dependsOn(tasks.named("check")) }
tasks.register("verifyFull") { group = "verification"; dependsOn(tasks.named("verifyFast")); dependsOn(tasks.named("headlessGameTest")) }
tasks.withType<JavaCompile>().configureEach { options.release.set(17) }

mixin { add(sourceSets.main.get(), "realistic_placement_preview.refmap.json"); config("realistic_placement_preview.mixins.json") }
