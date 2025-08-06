import Modules.*

plugins {
    id("com.google.devtools.ksp")
}
dependencies {
    implementation(kotlin("stdlib-jdk8"))
    import(COMMON)
    import(AUTO_UTIL)
    implementation(Library.kspApi)
    implementation(Library.kotlinpoet)
    implementation(Library.kotlinpoetKsp)
    implementation(Library.autoServiceAnno)
    ksp(Library.autoServiceKsp)
}