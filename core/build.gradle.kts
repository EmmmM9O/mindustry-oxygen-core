import Modules.*
import oxygen.annotations.*

plugins {
    id("com.google.devtools.ksp")
}
dependencies {
    compileOnly(Library.arcCore)
    compileOnly(Library.mdtCore)
    import(MDT_COMMON)
    import(GRAPHICS)
    import(MDT_ANNO_KSP)
    ksp(AUTO_UTIL_KSP)
}
ksp {
    arg(KConfig.debug, "true")
}
