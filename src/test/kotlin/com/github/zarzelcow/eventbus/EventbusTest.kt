package com.github.zarzelcow.eventbus

import org.testng.Assert.*
import org.testng.annotations.*

class EventbusTest {
    private val eventbus = Eventbus()
    private val myListenerInstance = MyListener()

    @AfterMethod
    fun tearDown() {
        eventbus.unsubscribeAll()
        myListenerInstance.count = 0
    }

    @Test
    fun publish() {
        eventbus.subscribe(myListenerInstance)
        eventbus.publish(GenericEvent)
        assertEquals(1, myListenerInstance.count)
    }

    @Test
    fun subscribe() {
        eventbus.subscribe(myListenerInstance)
        assertEquals(1, eventbus.registry.size)
    }

    @Test
    fun unsubscribe() {
        eventbus.subscribe(myListenerInstance)
        assertEquals(1, eventbus.registry.size)
        eventbus.publish(GenericEvent)
        assertEquals(1, myListenerInstance.count)
        eventbus.unsubscribe(myListenerInstance)
        assertEquals(0, eventbus.registry.size)
        eventbus.publish(GenericEvent) // Should be ignored
        assertEquals(1, myListenerInstance.count)
    }

    @Test
    fun `test multiple listeners`() {
        val listener1 = MyListener()
        val listener2 = MyListener()
        val listener3 = MyListener()
        eventbus.subscribe(listener1)
        eventbus.subscribe(listener2)
        eventbus.subscribe(listener3)
        assertEquals(3, eventbus.registry.size)
        eventbus.publish(GenericEvent)
        assertEquals(1, listener1.count)
        assertEquals(1, listener2.count)
        assertEquals(1, listener3.count)
    }

    @Test
    fun `test register Class With No Handlers`() {
        eventbus.subscribe(object {})
        assertEquals(0, eventbus.registry.size)
    }


    @Test
    fun `test handler no args`() {
        eventbus.subscribe(myListenerInstance)
        eventbus.publish(AnotherEvent)
        assertEquals(1, myListenerInstance.count2)
    }

    @Test
    fun `throw on invalid handler`() {
        assertThrows(IllegalStateException::class.java) {
            eventbus.subscribe(object {
                @Handler
                val onEvent1 = {}
            })
        }
        assertThrows(IllegalStateException::class.java) {
            eventbus.subscribe(object {
                @Handler(GenericEvent::class)
                val onEvent2 = Any()
            })
        }
    }

    @Test
    fun `collect listeners from super`() {
        val clazz = object : MyListener() {}
        eventbus.subscribe(clazz)
        assertEquals(1, eventbus.registry.size)
    }

    internal open class MyListener {
        var count = 0
        var count2 = 0

        @Handler
        val onEvent = { event: GenericEvent ->
            count++
        }

        @Handler(AnotherEvent::class)
        val onAnotherEvent = {
            count2++
        }
    }

    internal object AnotherEvent
    internal object GenericEvent
}