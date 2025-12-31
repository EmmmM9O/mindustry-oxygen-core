import Modules.*

plugins {
    id("com.google.devtools.ksp")
}
dependencies {
    import(AUTO_UTIL_KSP)
    import(AUTO_UTIL)
    import(MDT_ANNO)
    implementation(Library.arcCore)
    implementation(Library.autoServiceAnno)
    ksp(Library.autoServiceKsp)
    ksp(AUTO_UTIL_KSP)
}
