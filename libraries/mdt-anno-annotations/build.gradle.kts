import Modules.*
plugins {
    id("com.google.devtools.ksp")
}
dependencies {
    import(AUTO_UTIL)
    ksp(AUTO_UTIL_KSP)
    compileOnly(Library.arcCore)
}
