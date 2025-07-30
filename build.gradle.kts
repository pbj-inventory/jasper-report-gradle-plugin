group = "ca.cleaningdepot.tools"
version = "3.6.3"
description = "jasperreports-gradle-plugin"

plugins {
    `kotlin-dsl`
    `maven-publish`
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://jaspersoft.jfrog.io/jaspersoft/third-party-ce-artifacts/")
    }
}

dependencies {
    api("net.sf.jasperreports:jasperreports:6.21.4")
    api("net.sf.jasperreports:jasperreports-fonts:6.21.4")
    api("net.sf.jasperreports:jasperreports-functions:6.21.4")
    api("org.mozilla:rhino:1.7.14")
    api("org.codehaus.plexus:plexus-compiler-api:2.13.0")
    implementation(kotlin("stdlib-jdk8"))

    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    plugins {
        create("jasperreports-plugin") {
            id = "ca.cleaningdepot.tools.jasperreports-gradle-plugin"
            implementationClass = "ca.cleaningdepot.tools.jasperreports.JasperPlugin"
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    repositories {
        mavenLocal()
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
