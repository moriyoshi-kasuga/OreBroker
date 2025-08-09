import xyz.jpenilla.runtask.RunExtension
import org.gradle.internal.os.OperatingSystem

plugins {
  java
  id("xyz.jpenilla.run-paper") version "2.3.1"
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
  maven {
    name = "papermc"
    url = uri("https://repo.papermc.io/repository/maven-public/")
  }
}

dependencies {
  compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
}

tasks.withType<JavaCompile> {
  options.encoding = "UTF-8"
}

tasks.withType<ProcessResources> {
  filteringCharset = "UTF-8"
}

extensions.configure<RunExtension> {
  disablePluginJarDetection()
}

tasks {
  runServer {
    dependsOn("movePlugin")
    minecraftVersion("1.21.8")
    jvmArgs("-Dpaper.disablePluginRemapping=true", "-Dfile.encoding=UTF-8")
  }
}

interface FsInjected {
  @get:Inject val fs: FileSystemOperations
}

tasks.register("movePlugin") {
  dependsOn("build")
  val injected = project.objects.newInstance<FsInjected>()
  doLast {
    injected.fs.delete { delete("run/plugins/SurvivalShooter.jar") }
    injected.fs.copy {
      from("build/libs/SurvivalShooter.jar")
      into("run/plugins")
    }
  }
}
