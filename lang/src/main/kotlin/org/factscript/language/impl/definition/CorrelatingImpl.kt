package org.factscript.language.impl.definition

import org.factscript.execution.Message
import org.factscript.execution.Receptor
import org.factscript.execution.Type
import org.factscript.execution.type
import org.factscript.language.*
import org.factscript.language.definition.Correlating
import org.factscript.language.definition.Junction
import org.factscript.language.definition.Node
import org.factscript.language.impl.utils.getValue
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

open class CorrelatingImpl<T: Any>(parent: Node):

    Await<T>,
    AwaitEventHaving<T, Any>,
    AwaitEventHavingMatch<T>,

    Correlating,
    NodeImpl(parent)

{

    override lateinit var consuming: KClass<out Any>

    private val properties = mutableListOf<String>()
    private val matching = mutableListOf<Any.() -> Any?>()
    override val correlating: Map<String, Any.() -> Any?> get() = properties.mapIndexed { i, p -> p to matching[i] }.toMap()

    override val type: Type get() = consuming.type

    fun <M : Any> event(type: KClass<M>): AwaitEventHaving<T, M> {
        this.consuming = type
        @Suppress("UNCHECKED_CAST")
        return this as AwaitEventHaving<T, M>
    }

    override fun <M : Any> success(event: KClass<M>) {
        event(event)
    }

    override fun <M : Any> failure(event: KClass<M>) {
        event(event)
    }

    override fun having(property: String): AwaitEventHavingMatch<T> {
        properties.add(property)
        return this
    }

    override fun having(map: AwaitEventHavingMatches<T, Any>.() -> Unit): AwaitEventBut<T> {
        val matches = HavingMatchesImpl<T>()
        matches.apply(map)
        properties.addAll(matches.properties)
        @Suppress("UNCHECKED_CAST")
        matching.addAll(matches.matching as Collection<Any.() -> Any?>)
        return this
    }

    override fun match(value: T.() -> Any?): AwaitEventBut<T> {
        @Suppress("UNCHECKED_CAST")
        matching.add(value as (Any.() -> Any?))
        return this
    }

    @Suppress("UNCHECKED_CAST")
    override fun but(path: Catch<T>.() -> Unit): AwaitEventBut<T> {
        val flow = CorrelatingFlowImpl(
            entity as KClass<T>,
            this
        )
        flow.apply(path)
        children.add(flow)
        return this
    }

    @Suppress("UNCHECKED_CAST")
    override fun first(path: Catch<T>.() -> Unit): AwaitOr<T> {
        val branch = BranchingImpl<T>(parent!!)
        branch.fork = Junction.First
        (parent as NodeImpl).children.remove(this)
        (parent as NodeImpl).children.add(branch)
        val flow = CorrelatingFlowImpl(
            entity as KClass<T>,
            branch
        ).apply(path)
        (branch as NodeImpl).children.add(flow)
        return branch
    }

    @Suppress("UNCHECKED_CAST")
    override fun time(duration: AwaitTimeDuration<T>) {
        (parent as NodeImpl).children.remove(this)
        (parent as NodeImpl).children.add(duration as WaitingImpl)
        duration.parent = parent
    }

    override fun time(limit: AwaitTimeLimit<T>) {
        (parent as NodeImpl).children.remove(this)
        (parent as NodeImpl).children.add(limit as WaitingImpl)
        limit.parent = parent
    }

    override fun findReceptorsFor(message: Message): Set<Receptor> {
        return (if (consuming.isInstance(message.fact.details))
            listOf(Receptor(consuming, properties.map { it to message.fact.details.getValue(it) }.toMap()))
        else emptyList()).toSet()
    }

}

class HavingMatchesImpl<T: Any>: AwaitEventHavingMatches<T, Any>, OnCommandHavingMatches<T, Any> {

    val properties: MutableList<String> = mutableListOf()
    val matching: MutableList<T.() -> Any?> = mutableListOf()

    override fun String.match(match: T.() -> Any?) {
        properties.add(this)
        matching.add(match)
    }

}