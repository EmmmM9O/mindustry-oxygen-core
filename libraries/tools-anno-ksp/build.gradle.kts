import Modules.*

plugins {
    id("com.google.devtools.ksp")
}
dependencies {
    import(AUTO_UTIL_KSP)
    import(TOOLS_ANNO)
    implementation(Library.autoServiceAnno)
    ksp(Library.autoServiceKsp)
}