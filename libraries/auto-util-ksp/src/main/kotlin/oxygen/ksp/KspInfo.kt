package oxygen.ksp

import com.google.devtools.ksp.symbol.*
import oxygen.annotations.*

class TypeInfoImpl(val self: KSType) : TypeInfo

class NodeInfoImpl(val self: KSNode?) : NodeInfo {

    override val parent: NodeInfo? = // getInfoNode(self?.parent)
        null
}

class AnnotatedInfoImpl(val self: KSAnnotated) : AnnotatedInfo {
    override val parent: NodeInfo? = //getInfoNode(self.parent)
        null
    override val annotations: Sequence<AnnotationInfo> = self.annotations.map { AnnotationInfoImpl(it) }
}

class AnnotationInfoImpl(val self: KSAnnotation) : AnnotationInfo {
    override val parent: NodeInfo? = getInfoNode(self.parent)
    override val annotationType: TypeRefInfo = TypeRefInfoImpl(self.annotationType)
}

class TypeRefInfoImpl(val self: KSTypeReference) : TypeRefInfo {
    override fun resolve(): TypeInfo = TypeInfoImpl(self.resolve())
    override val annotations: Sequence<AnnotationInfo> = self.annotations.map { AnnotationInfoImpl(it) }
    override val parent: NodeInfo? = //getInfoNode(self.parent)
        null

}

class DeclarationInfoImpl(val self: KSDeclaration) : DeclarationInfo {
    override val annotations: Sequence<AnnotationInfo> = self.annotations.map { AnnotationInfoImpl(it) }
    override val parent: NodeInfo? = getInfoNode(self.parent)
    override val qualifiedName: String? = self.qualifiedName?.asString()
    override val simpleName: String = self.simpleName.asString()
    override val packageName: String = self.packageName.asString()
    override val parentDeclaration = getInfoAs<DeclarationInfo>(self.parentDeclaration)
}

class ClassInfoImpl(val self: KSClassDeclaration) : ClassInfo {
    override val annotations: Sequence<AnnotationInfo> = self.annotations.map { AnnotationInfoImpl(it) }
    override val parent: NodeInfo? = getInfoNode(self.parent)
    override val qualifiedName: String? = self.qualifiedName?.asString()
    override val simpleName: String = self.simpleName.asString()
    override val packageName: String = self.packageName.asString()
    override val parentDeclaration = getInfoAs<DeclarationInfo>(self.parentDeclaration)
    override val isCompanionObject: Boolean = self.isCompanionObject
}

class PropertyInfoImpl(val self: KSPropertyDeclaration) : PropertyInfo {
    override val annotations: Sequence<AnnotationInfo> = self.annotations.map { AnnotationInfoImpl(it) }
    override val parent: NodeInfo? = getInfoNode(self.parent)
    override val qualifiedName: String? = self.qualifiedName?.asString()
    override val simpleName: String = self.simpleName.asString()
    override val packageName: String = self.packageName.asString()
    override val parentDeclaration = getInfoAs<DeclarationInfo>(self.parentDeclaration)
    override val type: TypeRefInfo = TypeRefInfoImpl(self.type)
}

fun getInfoAnnotated(anno: KSAnnotated): AnnotatedInfo = getInfoNode(anno) as AnnotatedInfo
fun getInfoNode(anno: KSNode?): NodeInfo? = anno?.let {
    when (anno) {
        is KSPropertyDeclaration -> PropertyInfoImpl(anno)
        is KSClassDeclaration -> ClassInfoImpl(anno)
        is KSDeclaration -> DeclarationInfoImpl(anno)
        is KSAnnotated -> AnnotatedInfoImpl(anno)
        else -> NodeInfoImpl(anno)
    }
}

@Suppress("UNCHECKED_CAST")
fun <T : NodeInfo> getInfoAs(anno: KSNode?): T? = getInfoNode(anno) as? T