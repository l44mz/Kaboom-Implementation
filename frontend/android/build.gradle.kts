allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

val newBuildDir: Directory =
    rootProject.layout.buildDirectory
        .dir("../../build")
        .get()
rootProject.layout.buildDirectory.value(newBuildDir)

val sdkCandidates = listOfNotNull(
    System.getenv("ANDROID_HOME"),
    System.getenv("ANDROID_SDK_ROOT"),
    System.getenv("LOCALAPPDATA")?.let { "$it/Android/Sdk" },
    System.getProperty("user.home")?.let { "$it/AppData/Local/Android/Sdk" }
).map(::java.io.File)

val androidSdk = sdkCandidates.firstOrNull { it.isDirectory }
if (androidSdk != null) {
    val localProperties = rootProject.file("local.properties")
    val existing = if (localProperties.isFile) localProperties.readLines() else emptyList()
    val withoutSdk = existing.filterNot { it.trimStart().startsWith("sdk.dir=") }
    localProperties.writeText(
        (withoutSdk + "sdk.dir=${androidSdk.absolutePath.replace('\\', '/')}")
            .joinToString(System.lineSeparator()) + System.lineSeparator()
    )
}

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}
subprojects {
    project.evaluationDependsOn(":app")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
