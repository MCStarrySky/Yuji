import io.papermc.paperweight.util.cache
import io.papermc.paperweight.util.cacheDir
import io.papermc.paperweight.util.download
import io.papermc.paperweight.util.fromJson

plugins {
    java
    `maven-publish`

    // Nothing special about this, just keep it up to date
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false

    // In general, keep this version in sync with upstream. Sometimes a newer version than upstream might work, but an older version is extremely likely to break.
    id("io.papermc.paperweight.patcher") version "1.5.5"
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

repositories {
    mavenCentral()
    maven(paperMavenPublicUrl) {
        content { onlyForConfigurations(configurations.paperclip.name) }
    }
}

dependencies {
    remapper("net.fabricmc:tiny-remapper:0.8.6:fat") // Must be kept in sync with upstream
    decompiler("net.minecraftforge:forgeflower:2.0.629.0") // Must be kept in sync with upstream
    paperclip("io.papermc:paperclip:3.0.3") // You probably want this to be kept in sync with upstream
}

allprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
}

subprojects {
    tasks.withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }
    tasks.withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
    }
    tasks.withType<ProcessResources> {
        filteringCharset = Charsets.UTF_8.name()
    }

    repositories {
        mavenCentral()
        maven(paperMavenPublicUrl)
    }
}

paperweight {
    serverProject.set(project(":yuji-server"))

    remapRepo.set(paperMavenPublicUrl)
    decompileRepo.set(paperMavenPublicUrl)

    useStandardUpstream("Kaiiju") {
        url.set(github("KaiijuMC", "Kaiiju"))
        ref.set(providers.gradleProperty("kaiijuRef"))

        withStandardPatcher {
            baseName("Kaiiju")

            apiPatchDir.set(layout.projectDirectory.dir("patches/api"))
            apiOutputDir.set(layout.projectDirectory.dir("yuji-api"))

            serverPatchDir.set(layout.projectDirectory.dir("patches/server"))
            serverOutputDir.set(layout.projectDirectory.dir("yuji-server"))
        }
    }
}

tasks.register("kaiijuRefLatest") {
    // Update the kaiijuRef in gradle.properties to be the latest commit.
    val tempDir = layout.cacheDir("kaiijuRefLatest");
    val file = "gradle.properties";

    doFirst {
        data class GithubCommit(
            val sha: String
        )

        val kaiijuLatestCommitJson = layout.cache.resolve("kaiijuLatestCommit.json");
        download.get().download("https://api.github.com/repos/KaiijuMC/Kaiiju/commits/ver/1.20.1", kaiijuLatestCommitJson);
        val kaiijuLatestCommit = io.papermc.paperweight.util.gson.fromJson<paper.libs.com.google.gson.JsonObject>(kaiijuLatestCommitJson)["sha"].asString;

        copy {
            from(file)
            into(tempDir)
            filter { line: String ->
                line.replace("kaiijuRef=.*".toRegex(), "kaiijuRef=$kaiijuLatestCommit")
            }
        }
    }

    doLast {
        copy {
            from(tempDir.file("gradle.properties"))
            into(project.file(file).parent)
        }
    }
}
