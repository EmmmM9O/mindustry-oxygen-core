@file:Suppress("ConstPropertyName")

object Versions {
    const val arc = "1.0"
    const val mdt = "1.0"
    const val autoServiceAnno = "1.1.1"
    const val autoServiceKsp = "1.2.0"
    const val kotlinpoet = "2.2.0"
    val kotlin by lazy { Config.get("kotlinVersion") }
    val ksp by lazy { Config.get("kspVersion") }
}

object Library {
    fun getArc(moduleName: String): String {
        return "com.github.emmmm9o:$moduleName:${Versions.arc}"
    }

    fun getMdt(moduleName: String): String {
        return "com.github.emmmm9o.Mindustry:$moduleName:${Versions.mdt}"
    }

    var arcCore = getArc("arc-core")
    var mdtCore = getMdt("core")
    var kspApi = "com.google.devtools.ksp:symbol-processing-api:${Versions.ksp}"
    var autoServiceAnno = "com.google.auto.service:auto-service-annotations:${Versions.autoServiceAnno}"
    var autoServiceKsp = "dev.zacsweers.autoservice:auto-service-ksp:${Versions.autoServiceKsp}"
    const val kotlinpoet = "com.squareup:kotlinpoet:${Versions.kotlinpoet}"
    const val kotlinpoetKsp = "com.squareup:kotlinpoet-ksp:${Versions.kotlinpoet}"
    var kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}"
}
