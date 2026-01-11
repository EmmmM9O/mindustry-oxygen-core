plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}
repositories {
    mavenLocal()
    mavenCentral()
}
dependencies {
    implementation(gradleApi())
}

gradlePlugin {
    plugins {
        create("glsl") {
            id = "oxygen.glsl"
            implementationClass = "oxygen.gradle.GlslPlugin"
        }
    }
}
