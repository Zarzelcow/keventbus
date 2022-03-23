package com.github.garfieldkart.eventbus

import net.jodah.typetools.TypeResolver
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class Eventbus {
    /* A map of instances to a list of handlers. */
    val registry = ConcurrentHashMap<Any, List<OneArgHandler>>()

    /**
     * Publish an event to all listeners that are listening for it
     *
     * @param event Any - The event that is being published.
     */
    fun publish(event: Any) = registry.values.asSequence().flatten().filter { it.isListeningFor(event) }.forEach { it.pub(event) }

    /**
     * If the instance has any listeners, add them to the registry
     *
     * @param instance Any — The instance of the class that you want to subscribe to.
     */
    fun subscribe(instance: Any) {
        val listeners = collectListeners(instance)
        if (listeners.isNotEmpty()) {
            registry[instance] = listeners
        }
    }

    /**
     * Remove the given instance from the registry
     *
     * @param instance Any - The instance that you want to unsubscribe from the event.
     */
    fun unsubscribe(instance: Any) {
        registry.remove(instance)
    }

    /**
     * Remove all the subscribers from the registry
     */
    fun unsubscribeAll() {
        registry.clear()
    }


    /**
     * If the type of the event handler is not of the type specified, throw an error
     *
     * @param typeStr The type of the event handler.
     */
    @Suppress("NOTHING_TO_INLINE")
    private inline fun errorNotOfType(typeStr: String): Nothing = error("Event handler not of type $typeStr")

    /**
     * Used by [Eventbus.subscribe] to collect all the [OneArgHandler]s for a given [instance].
     *
     * traverses the given [instance], looking for fields annotated with [Handler]
     * and returning a list of handlers
     *
     * @param instance The instance to collect the [OneArgHandler]s for.
     * @return A list of [OneArgHandler]'s.
     */
    private fun collectListeners(instance: Any, clazz: Class<*> = instance.javaClass, builder: MutableList<OneArgHandler> = mutableListOf()): List<OneArgHandler> {
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
        clazz.superclass?.let { collectListeners(instance, it, builder) }
        return builder
    }

    // Helper function for handlers
    private fun handler1Arg(value: Any) = tryCast<(Any) -> Any>(value)?.let { OneArgHandler(it, TypeResolver.resolveRawArguments(kotlin.Function1::class.java, it.javaClass)[0]) } ?: errorNotOfType("(Any) -> Any")
    // Helper function for handlers
    private fun handler0Args(value: Any, event: Class<*>) = tryCast<() -> Any>(value)?.let { NoArgsHandler(it, event) } ?: errorNotOfType("() -> Any")

    /**
     * Get the value of a field from an instance of a class
     *
     * @param instance The object that contains the field.
     * @param field The field to get the value from.
     */
    private fun getValueFromField(instance: Any, field: Field) = field.also { it.trySetAccessible() }.get(instance)
    private inline fun <reified T> tryCast(instance: Any?): T? = if (instance is T) instance else null
}

/*
 * A handler that takes no arguments.
 * ex:
 *
 * @Handler(EventType::class)
 * val handler = { ... }
 */
class NoArgsHandler(val handler: () -> Any, listeningFor: Class<*>) : OneArgHandler({}, listeningFor) {
    override fun pub(obj: Any) {
        handler()
    }
}

/*
 * A handler that takes a single arguments.
 * ex:
 *
 * @Handler
 * val handler = { event: EventType -> ... }
 */
open class OneArgHandler(private val handler: (Any) -> Any, private val listeningFor: Class<*>) {
    open fun pub(obj: Any) {
        handler(obj)
    }

    fun isListeningFor(obj: Any) = this.listeningFor.isInstance(obj)
}


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
/**
 * Indicates a field that should be used as an event handler.
 */
annotation class Handler(val klass: KClass<*> = Handler::class)