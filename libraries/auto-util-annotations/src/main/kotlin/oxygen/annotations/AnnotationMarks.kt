package oxygen.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class AnnoResolver(
    val name: String = "@Gen",
    val path: String = "@",
    val file: String = "@Func"
)

@Retention(AnnotationRetention.SOURCE)
annotation class Source(val source: String)

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AnnoObject(
    val name: String = "@Obj",
    val funcName: String = "toObj",
    val gloFuncName:String="@ToObj",
    val path: String = "@",
    val file: String = "@Object"
)