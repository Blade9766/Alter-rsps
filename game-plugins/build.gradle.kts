description = "Alter Servers Plugins"
val lib = rootProject.project.libs

dependencies {
    implementation(projects.gameServer)
    implementation(projects.util)
    implementation(project(":game-api"))
    implementation(rootProject.project.libs.rsprot)
    implementation(rootProject.projects.plugins.filestore)
    implementation(rootProject.projects.plugins.rscm)
    implementation(lib.routefinder)

    // AggroVerify walks the monster packages the way PluginRepository walks the whole jar.
    testImplementation(lib.classgraph)
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// The agility/skill configs the diag tests validate live in the repo's data directory, outside this
// module, so Gradle would otherwise call the test task up to date after a config-only edit - exactly
// the change those tests exist to catch.
tasks.named<Test>("test") {
    // Every World a diag test builds allocates a CollisionFlagMap, and several of them do. That
    // already sat close to Gradle's 512m default, and AggroVerify's plugin scan builds one more,
    // which tipped ZombieVerify into an OutOfMemoryError rather than a test failure.
    maxHeapSize = "2g"

    inputs.dir(rootProject.layout.projectDirectory.dir("data/cfg"))
        .withPropertyName("dataConfig")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
