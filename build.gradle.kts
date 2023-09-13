plugins {
    java
    signing
    distribution
    id("com.diffplug.spotless") version "6.12.0"
    id("org.omegat.gradle") version "1.5.11"
}

version = "0.2.0"

omegat {
    version = "6.0.0"
    pluginClass = "org.omegat.machinetranslators.libretranslate.LibreTranslate"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    packIntoJar("org.slf4j:slf4j-api:2.0.7")
    implementation("com.fasterxml.jackson.core:jackson-core:2.14.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("commons-io:commons-io:2.11.0")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.0")
    testImplementation("org.slf4j:slf4j-nop:2.0.7")
}

distributions {
    main {
        contents {
            from(tasks["jar"], "README.md", "COPYING", "CHANGELOG.md")
        }
    }
}

val signKey = listOf("signingKey", "signing.keyId", "signing.gnupg.keyName").find {project.hasProperty(it)}
tasks.withType<Sign> {
    onlyIf { signKey != null }
}

signing {
    when (signKey) {
        "signingKey" -> {
            val signingKey: String? by project
            val signingPassword: String? by project
            useInMemoryPgpKeys(signingKey, signingPassword)
        }

        "signing.keyId" -> {/* do nothing */
        }

        "signing.gnupg.keyName" -> {
            useGpgCmd()
        }
    }
    sign(tasks.distZip.get())
    sign(tasks.jar.get())
}

val jar by tasks.getting(Jar::class) {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

spotless {
    java {
        target(listOf("src/*/java/**/*.java"))
        removeUnusedImports()
        palantirJavaFormat()
        importOrder("org.omegat", "java", "javax", "", "\\#")
    }
}
