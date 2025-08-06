package oxygen.annotations

interface TypeInfo

interface NodeInfo {
    val parent: NodeInfo?
}

interface AnnotatedInfo : NodeInfo {
    val annotations: Sequence<AnnotationInfo>
}

interface TypeRefInfo : AnnotatedInfo {
    fun resolve(): TypeInfo
}

interface AnnotationInfo : NodeInfo {
    val annotationType: TypeRefInfo
}

interface DeclarationInfo : AnnotatedInfo {
    val packageName: String
    val simpleName: String
    val qualifiedName: String?
    val parentDeclaration: DeclarationInfo?
}

interface ClassInfo : DeclarationInfo {
    val isCompanionObject: Boolean
}

interface PropertyInfo : DeclarationInfo {
    val type: TypeRefInfo
}
