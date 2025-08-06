import oxygen.annotations.*
import Modules.*

plugins {
    id("com.google.devtools.ksp")
}
dependencies {
    compileOnly(Library.arcCore)
    compileOnly(Library.mdtCore)
    import(
        COMMON,
        MDT_ANNO, MDT_ANNO_KSP
    )
    ksp(AUTO_UTIL_KSP)
}
ksp {
    arg(KConfig.debug, "true")
}
