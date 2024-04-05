package co.present.present.extensions

import kotlin.reflect.KClass
import kotlin.reflect.KDeclarationContainer
import kotlin.reflect.KProperty
import kotlin.jvm.internal.CallableReference

val KProperty<*>.ownerCanonicalName: String? get() = owner?.canonicalName
val KProperty<*>.owner: KDeclarationContainer? get() = if (this is CallableReference) owner else null
val KDeclarationContainer.canonicalName: String? get() = (this as? KClass<*>)?.java?.canonicalName