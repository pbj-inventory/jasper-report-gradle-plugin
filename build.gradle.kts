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
    implementation("net.sf.jasperreports:jasperreports:7.0.7")
    implementation("net.sf.jasperreports:jasperreports-jdt:7.0.7")
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("net.sf.jasperreports:jasperreports-fonts:7.0.7")
    testRuntimeOnly("net.sf.jasperreports:jasperreports-charts:7.0.7")
    testRuntimeOnly("net.sf.jasperreports:jasperreports-javascript:7.0.7")
    testRuntimeOnly("net.sf.jasperreports:jasperreports-json:7.0.7")
    testRuntimeOnly("net.sf.jasperreports:jasperreports-hibernate:7.0.7")
    testRuntimeOnly("net.sf.jasperreports:jasperreports-barbecue:7.0.7")
    testRuntimeOnly("net.sf.jasperreports:jasperreports-pdf:7.0.7")
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
