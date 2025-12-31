import Modules.COMMON
import Modules.MDT_COMMON

dependencies {
    compileOnly(Library.arcCore)
    compileOnly(Library.mdtCore)
    import(MDT_COMMON)
    import(COMMON)
}
