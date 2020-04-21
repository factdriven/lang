package io.factdriven.impl.definition

import io.factdriven.definition.Node
import io.factdriven.definition.Throwing
import io.factdriven.execution.Type
import io.factdriven.execution.type
import io.factdriven.language.Emit
import io.factdriven.language.Issue
import io.factdriven.language.Sentence
import kotlin.reflect.KClass

open class ThrowingImpl<T: Any>(parent: Node):

    Emit<T>,
    Issue<T>,
    Sentence<T, Any>,

    Throwing,
    NodeImpl(parent)

{

    override lateinit var throwing: KClass<*>
    override lateinit var instance: Any.() -> Any

    override val type: Type get() = throwing.type

    override fun <M : Any> event(type: KClass<M>): Sentence<T, M> {
        this.throwing = type
        @Suppress("UNCHECKED_CAST")
        return this as Sentence<T, M>
    }

    override fun <M : Any> command(type: KClass<M>): Sentence<T, M> {
        this.throwing = type
        @Suppress("UNCHECKED_CAST")
        return this as Sentence<T, M>
    }

    override fun by(instance: T.() -> Any) {
        @Suppress("UNCHECKED_CAST")
        this.instance = instance as Any.() -> Any
    }

}
