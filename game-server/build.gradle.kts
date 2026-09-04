plugins {
    alias(libs.plugins.shadow)
    application
    `maven-publish`
}
description = "Alter Game Server Launcher"
application {
    apply(plugin = "maven-publish")
    mainClass.set("org.alter.game.Launcher")
}
val lib = rootProject.project.libs
dependencies {
    with(lib) {
        implementation(projects.util)
        runtimeOnly(projects.gamePlugins)
        implementation(kotlinx.coroutines)
        implementation(reflection)
        implementation(commons)
        implementation(classgraph)
        implementation(fastutil)
        implementation(bouncycastle)
        implementation(jackson.module.kotlin)
        implementation(jackson.dataformat.yaml)
        implementation(kotlin.csv)
        implementation(mongo.bson)
        implementation(mongo.driver)
        implementation(rootProject.projects.plugins.rscm)
        testImplementation(junit)
        implementation(rootProject.project.libs.rsprot)
        implementation(rootProject.projects.plugins.filestore)
        implementation(rootProject.projects.plugins.rscm)
        implementation(rootProject.projects.plugins.tools)
        implementation(lib.routefinder)
    }
}
sourceSets {
    named("main") {
        kotlin.srcDirs("src/main/kotlin")
        resources.srcDirs("src/main/resources")
    }
}

@Suppress("ktlint:standard:max-line-length")
tasks.register("install") {
    description = "Install Alter"
    val cacheList =
        listOf(
            "/cache/main_file_cache.dat2",
            "/cache/main_file_cache.idx0",
            "/cache/main_file_cache.idx1",
            "/cache/main_file_cache.idx2",
            "/cache/main_file_cache.idx3",
            "/cache/main_file_cache.idx4",
            "/cache/main_file_cache.idx5",
            "/cache/main_file_cache.idx7",
            "/cache/main_file_cache.idx8",
            "/cache/main_file_cache.idx9",
            "/cache/main_file_cache.idx10",
            "/cache/main_file_cache.idx11",
            "/cache/main_file_cache.idx12",
            "/cache/main_file_cache.idx13",
            "/cache/main_file_cache.idx14",
            "/cache/main_file_cache.idx15",
            "/cache/main_file_cache.idx17",
            "/cache/main_file_cache.idx18",
            "/cache/main_file_cache.idx19",
            "/cache/main_file_cache.idx20",
            "/cache/main_file_cache.idx255",
            "xteas.json",
        )
    cacheList.forEach {
        val file = File("${rootProject.projectDir}/data/$it")
        if (!file.exists()) {
            throw GradleException(
                "\u001B[45m \u001B[30m Missing file! : $file. Go back to: https://github.com/AlterRSPS/Alter and read how to setup plz >____> It's so easy to set this up and you failed at it wtfff?!?!. \u001B[0m",
            )
        }
    }
    dependsOn("runRsaService")
    dependsOn("decryptMap")

    doLast {
        copy {
            into("${rootProject.projectDir}/")
            from("${rootProject.projectDir}/game.example.yml") {
                rename("game.example.yml", "game.yml")
            }
            from("${rootProject.projectDir}/dev-settings.example.yml") {
                rename("dev-settings.example.yml", "dev-settings.yml")
            }
            file("${rootProject.projectDir}/first-launch").createNewFile()
        }
    }
}
tasks.register<JavaExec>("runRsaService") {
    group = "application"
    workingDir = rootProject.projectDir
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.alter.game.service.rsa.RsaService")
    args = listOf("16", "1024", "./data/rsa/key.pem") // radix, bitcount, rsa pem file
}
tasks.register<JavaExec>("decryptMap") {
    description = "Will decrypt world map and remove xteas"
    group = "application"
    workingDir = rootProject.projectDir
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.alter.game.service.mapdecrypter.decryptMap")
}

task<Copy>("extractDependencies") {
    from(zipTree("build/distributions/game-server-${project.version}.zip")) {
        include("game-${project.version}/lib/*")
        eachFile {
            path = name
        }
        includeEmptyDirs = false
    }
    into("build/deps")
}

tasks.register<Copy>("applicationDistribution") {
    from("$rootDir/data/") {
        into("bin/data/")
        include("**")
        exclude("saves/*")
    }
}
tasks.named<Copy>("applicationDistribution") {
    from("$rootDir") {
        into("bin")
        include("/game-plugins/*")
        include("game.example.yml")
        rename("game.example.yml", "game.yml")
    }
}
tasks.named<Zip>("shadowDistZip") {
    from("$rootDir/data/") {
        into("game-shadow-${project.version}/bin/data/")
        include("**")
        exclude("saves/*")
    }
    from("$rootDir") {
        into("game-shadow-${project.version}/bin/")
        include("/game-plugins/*")
        include("game.example.yml")
        rename("game.example.yml", "game.yml")
    }
}
tasks.register<Tar>("myShadowDistTar") {
    archiveFileName.set("game-shadow-${project.version}.tar")
    destinationDirectory.set(file("build/distributions/"))
    from("$rootDir/data/") {
        into("game-shadow-${project.version}/bin/data/")
        include("**")
        exclude("saves/*")
    }
    from("$rootDir") {
        into("game-shadow-${project.version}/bin/")
        include("/game-plugins/*")
        include("game.example.yml")
        rename("game.example.yml", "game.yml")
    }
}
tasks.named("build") {
    finalizedBy("extractDependencies")
}
tasks.named("install") {
    dependsOn("build")
}
tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}


/**
 * @TODO Forgot about this one.
 */
publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
        groupId = "org.alter"
        artifactId = "alter"
        pom {
            packaging = "jar"
            name.set("Alter")
            description.set("AlterServer All")
        }
    }
}
// TEMPORARY diagnostic task - remove after use.
tasks.register<JavaExec>("itemOptDiag") {
    group = "diagnostics"
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.alter.tools.ItemOptDiag")
    workingDir = rootDir
}

// Resolves the cache libraries' own runtime dependencies without dragging in :game-plugins.
val locDumpRuntime: Configuration by configurations.creating

dependencies {
    locDumpRuntime(rootProject.projects.plugins.filestore)
    locDumpRuntime(rootProject.projects.plugins.rscm)
}

// TEMPORARY diagnostic task - remove after use.
tasks.register<JavaExec>("agilityLocDump") {
    group = "diagnostics"
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    // compileClasspath + own output rather than runtimeClasspath: the dump only reads the cache, and
    // runtimeClasspath would drag in :game-plugins, coupling this diagnostic to content compiling.
    classpath = sourceSets["main"].output + sourceSets["main"].compileClasspath + locDumpRuntime
    mainClass.set("org.alter.tools.AgilityLocDump")
    workingDir = rootDir
}

// TEMPORARY diagnostic task - remove after use.
tasks.register<JavaExec>("agilityMapDump") {
    group = "diagnostics"
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    classpath = sourceSets["main"].output + sourceSets["main"].compileClasspath + locDumpRuntime
    mainClass.set("org.alter.tools.AgilityMapDump")
    workingDir = rootDir
}

// TEMPORARY diagnostic task - remove after use.
// TEMPORARY diagnostic - remove after use.
tasks.register<JavaExec>("objDefDump") {
    group = "diagnostics"
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    classpath = sourceSets["main"].output + sourceSets["main"].compileClasspath + locDumpRuntime
    mainClass.set("org.alter.tools.ObjDefDump")
    workingDir = rootDir
}

tasks.register<JavaExec>("agilityReachDump") {
    group = "diagnostics"
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    classpath = sourceSets["main"].output + sourceSets["main"].compileClasspath + locDumpRuntime
    mainClass.set("org.alter.tools.AgilityReachDump")
    workingDir = rootDir
}

// Bakes data/cfg/grandexchange/prices.json from the OSRS wiki price API. Re-run whenever the
// guide prices should be refreshed; the file it writes is committed.
tasks.register<JavaExec>("gePriceDump") {
    group = "diagnostics"
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    classpath = sourceSets["main"].output + sourceSets["main"].compileClasspath + locDumpRuntime
    mainClass.set("org.alter.tools.GrandExchangePriceDump")
    workingDir = rootDir
}

// TEMPORARY diagnostic task - remove after use.
tasks.register<JavaExec>("makeoverDiag") {
    group = "diagnostics"
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.alter.tools.MakeoverDiag")
    // The server resolves its own paths against "..", the way the packaged launcher runs.
    workingDir = File(rootDir, "game-server")
}

// TEMPORARY diagnostic task - remove after use.
// TEMPORARY diagnostic - remove after use.
tasks.register<JavaExec>("wildernessGateDiag") {
    group = "diagnostics"
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.alter.tools.WildernessGateDiag")
    // The server resolves its own paths against "..", the way the packaged launcher runs.
    workingDir = File(rootDir, "game-server")
}

tasks.register<JavaExec>("varpSaveDiag") {
    group = "diagnostics"
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.alter.tools.VarpSaveDiag")
    // The server resolves its own paths against "..", the way the packaged launcher runs.
    workingDir = File(rootDir, "game-server")
}

// Bakes data/cfg/settings/settings.json from this cache's own settings catalogue (enum 422 plus
// clientscripts 3960/3965). Re-run if the cache revision moves; the file it writes is committed.
tasks.register<JavaExec>("settingsDump") {
    group = "diagnostics"
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    classpath = sourceSets["main"].output + sourceSets["main"].compileClasspath + locDumpRuntime
    mainClass.set("org.alter.tools.SettingsDump")
    workingDir = rootDir
}

// Boots a world and checks the Settings plugin registered, then prints the sub map the All Settings
// panel will be read with. See org.alter.tools.SettingsDiag.
tasks.register<JavaExec>("settingsDiag") {
    group = "diagnostics"
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.alter.tools.SettingsDiag")
    // The server resolves its own paths against "..", the way the packaged launcher runs.
    workingDir = File(rootDir, "game-server")
}

// Boots a world and checks the Duel Arena plugin registered, then re-verifies every cache id the
// duel screens depend on. See org.alter.tools.DuelDiag.
tasks.register<JavaExec>("duelDiag") {
    group = "diagnostics"
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.alter.tools.DuelDiag")
    // The server resolves its own paths against "..", the way the packaged launcher runs.
    workingDir = File(rootDir, "game-server")
}

// TEMPORARY diagnostic task - remove after use.
tasks.register<JavaExec>("interfaceTextDump") {
    group = "diagnostics"
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    classpath = sourceSets["main"].output + sourceSets["main"].compileClasspath + locDumpRuntime
    mainClass.set("org.alter.tools.InterfaceTextDump")
    workingDir = rootDir
}

// TEMPORARY diagnostic task - remove after use.
tasks.register<JavaExec>("npcAnimDiag") {
    group = "diagnostics"
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    classpath = sourceSets["main"].output + sourceSets["main"].compileClasspath + locDumpRuntime
    mainClass.set("org.alter.tools.NpcAnimDiag")
    workingDir = rootDir
}

tasks.register<JavaExec>("chaosDruidDiag") {
    group = "diagnostics"
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.alter.tools.ChaosDruidDiag")
    // The server resolves its own paths against "..", the way the packaged launcher runs.
    workingDir = File(rootDir, "game-server")
}

// TEMPORARY diagnostic task - remove after use.
tasks.register<JavaExec>("dataOrbsDiag") {
    group = "diagnostics"
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.alter.tools.DataOrbsDiag")
    // The server resolves its own paths against "..", the way the packaged launcher runs.
    workingDir = File(rootDir, "game-server")
}
