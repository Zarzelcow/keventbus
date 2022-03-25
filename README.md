# EventBus

Simple event bus for event-driven programming.
Taking advantage of kotlin language features
instead of typical reflections

###### Table of Contents
* [Installation](#Installation)
* [Usage](#Usage)

## Installation
* [Build Tool](#Build-Tool)
* [From Source](#From-Source)

### Build Tool
If you are not using Gradle fallow the guide for other build tools on [jitpack.io](https://jitpack.io/).

###### Gradle
Add `https://www.jitpack.io` as a maven repo to your project
```groovy
repositories {
    maven { url 'https://www.jitpack.io' }
}
```
Then add the following dependency to your project
```groovy
dependencies {
    implementation 'com.github.Zarzelcow:eventbus:-SNAPSHOT'
}
```

### From Source
All code except dependencies is contained in one file `Eventbus.kt`

Dependencies are the Kotlin Standard Library and jodah's [TypeTools](https://github.com/jhalterman/typetools)

## Usage
Unlike most 'conventional' annotation based event bus implementations,
You instead register listeners on fields using kotlin functions


btw this still uses reflections, just not to call handlers
***
```kotlin
import com.github.zarzelcow.eventbus.Eventbus

val EVENT_BUS = EventBus()

class MyListener {

    @Handler
    val onKeypress = { event: EventKeypress ->
        println("Key pressed: ${event.key}")
    }

}
```
Btw. you can also create handlers like so if you
don't need the event itself
```kotlin
@Handler(EventKeypress::class)
fun onKeypress() {
    println("A Key pressed")
}
```
This will register a listener on the event
`EventKeypress`
and will be called when the event is fired.
***
Events are completely normal java objects
meaning you don't have to extend a type to
publish anything

An example of an event is
```kotlin
    class EventKeypress(val key: Int)
```
***
To publish an event to all register listeners
use code like this
```kotlin
fun `example publish`() {
    EVENT_BUS.publish(EventKeypress(key))
}
```
***
Registering the listener is done by calling `subscribe`
with the instance of the class holding the listener
```kotlin
    EVENT_BUS.subscribe(MyListenerInstance)
```
***
To unregister a listener, call `unsubscribe`
with the instance of the class holding the listener
```kotlin
    EVENT_BUS.unsubscribe(MyListenerInstance)
```
***
That gives you a simple event bus loop
that you can use to publish events
and subscribe to them in your code.
```kotlin
import com.github.zarzelcow.eventbus.Eventbus

val EVENT_BUS = EventBus()

class EventKeypress(val key: Int)
class MyListener {

    @Handler
    val onKeypress = { event: EventKeypress ->
        println("Key pressed: ${event.key}")
    }

}

fun main() {
    val MyListenerInstance = MyListener()
    EVENT_BUS.subscribe(MyListenerInstance)
    EVENT_BUS.publish(EventKeypress(1)) // prints "Key pressed: 1"
    EVENT_BUS.unsubscribe(MyListenerInstance)
}
```
***
Look at Test cases for some more examples