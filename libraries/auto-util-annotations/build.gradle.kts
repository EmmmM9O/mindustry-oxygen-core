import Modules.COMMON

dependencies {
    compileOnly(project(COMMON.path))
}
tasks.register<Copy>("copyKotlin") {
    val to = "${layout.buildDirectory.get()}/generated/oxy/main/kotlin"
    from("${rootDir}/buildSrc/src/main/kotlin")
    into(to)
    include("oxygen/**/*.kt")
    doFirst {
        delete(to)
    }
}
tasks.named("compileKotlin") {
    dependsOn("copyKotlin")
}
