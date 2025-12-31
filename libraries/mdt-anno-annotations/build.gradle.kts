import Modules.AUTO_UTIL
import Modules.AUTO_UTIL_KSP

plugins {
    id("com.google.devtools.ksp")
}
dependencies {
    import(AUTO_UTIL)
    ksp(AUTO_UTIL_KSP)
    compileOnly(Library.arcCore)
    compileOnly(Library.mdtCore)
}
