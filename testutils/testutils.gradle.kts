plugins {
    `java-library`
}

description = "Common test utilities for the add-ons."

val nanohttpdVersion = "2.3.1"

configurations {
    "compileClasspath" {
        exclude(group = "log4j")
        exclude(group = "org.apache.logging.log4j", module = "log4j-1.2-api")
    }
}

dependencies {
    compileOnly("org.zaproxy:zap:2.14.0")
    implementation(project(":addOns:network"))
    implementation("org.apache.httpcomponents.client5:httpclient5:5.2.1")

    api("org.hamcrest:hamcrest-library:2.2")
    api("org.junit.jupiter:junit-jupiter:5.9.3")
    runtimeOnly("org.junit.platform:junit-platform-launcher")
    api("org.mockito:mockito-junit-jupiter:5.1.1")

    api("org.nanohttpd:nanohttpd-webserver:$nanohttpdVersion")
    api("org.nanohttpd:nanohttpd-websocket:$nanohttpdVersion")
}
