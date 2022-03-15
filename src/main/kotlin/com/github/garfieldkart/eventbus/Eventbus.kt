package com.github.garfieldkart.eventbus

import net.jodah.typetools.TypeResolver
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class Eventbus {
    val registry = ConcurrentHashMap<Any, List<OneArgHandler>>()

    fun publish(event: Any) = registry.values.asSequence().flatten().filter { it.isListeningFor(event) }.forEach { it.pub(event) }

    fun subscribe(instance: Any) {
        val listeners = collectListeners(instance)
        if (listeners.isNotEmpty()) {
            registry[instance] = listeners
        }
    }
    fun unsubscribe(instance: Any) {
        registry.remove(instance)
    }
    fun unsubscribeAll() {
        registry.clear()
    }


    @Suppress("NOTHING_TO_INLINE")
    private inline fun errorNotOfType(typeStr: String): Nothing = error("Event handler not of type $typeStr")

    /**
     * Used by [Eventbus.subscribe] to collect all the [OneArgHandler]s for a given [instance].
     *
     * traverses the given [instance], looking for fields annotated with [Handler]
     * and returning a list of handlers
     *
     * @param instance The instance to collect the [OneArgHandler]s for.
     * @return A list of [OneArgHandler]s.
     */
    private fun collectListeners(instance: Any, clazz: Class<*> = instance.javaClass): List<OneArgHandler> {
        val builder = mutableListOf<OneArgHandler>()
        for (field in clazz.declaredFields) {
            if (field.isAnnotationPresent(Handler::class.java)) {
                val annotation = field.getAnnotation(Handler::class.java)
                val fieldValue = getValueFromField(instance, field)
                builder.add(
                    when (annotation.klass) {
                        Handler::class -> handler1Arg(fieldValue)
                        else -> handler0Args(fieldValue, annotation.klass.java)
                    }
                )
            }
        }
        return builder.toList()
    }

    private fun handler1Arg(value: Any) = tryCast<(Any) -> Any>(value)?.let { OneArgHandler(it, TypeResolver.resolveRawArguments(kotlin.Function1::class.java, it.javaClass)[0]) } ?: errorNotOfType("(Any) -> Any")
    private fun handler0Args(value: Any, event: Class<*>) = tryCast<() -> Any>(value)?.let { NoArgsHandler(it, event) } ?: errorNotOfType("() -> Any")

    private fun getValueFromField(instance: Any, field: Field) = field.also { it.trySetAccessible() }.get(instance)
    private inline fun <reified T> tryCast(instance: Any?): T? = if (instance is T) instance else null
}

class NoArgsHandler(val handler: () -> Any, listeningFor: Class<*>) : OneArgHandler({}, listeningFor) {
    override fun pub(obj: Any) {
        handler()
    }
}

open class OneArgHandler(private val handler: (Any) -> Any, private val listeningFor: Class<*>) {
    open fun pub(obj: Any) {
        handler(obj)
    }

    fun isListeningFor(obj: Any) = this.listeningFor.isInstance(obj)
}


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Handler(val klass: KClass<*> = Handler::class)