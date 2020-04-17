package io.factdriven.impl.definition

import io.factdriven.definition.Branching
import io.factdriven.definition.Node
import io.factdriven.definition.Gateway
import io.factdriven.execution.Type
import io.factdriven.execution.type
import io.factdriven.language.*
import kotlin.reflect.KClass

open class BranchingImpl<T: Any>(parent: Node):

    Select<T>,
    SelectOr<T>,
    AwaitOr<T>,
    ExecuteAnd<T>,
    Branching,
    NodeImpl(parent)

{

    override lateinit var gateway: Gateway
    override var label: String = ""; protected set

    override val type: Type
        get() = Type(
            entity.type.context,
            gateway.name
        )

    override fun either(path: ConditionalExecution<T>.() -> Unit): SelectOr<T> {
        gateway = Gateway.Exclusive
        return or(path)
    }

    override fun all(path: ConditionalExecution<T>.() -> Unit): SelectOr<T> {
        gateway = Gateway.Inclusive
        return or(path)
    }

    override fun or(path: ConditionalExecution<T>.() -> Unit): SelectOr<T> {
        @Suppress("UNCHECKED_CAST")
        val flow = ConditionalExecutionImpl(
            entity as KClass<T>,
            this
        ).apply(path)
        children.add(flow)
        return this
    }

    override fun or(path: Flow<T>.() -> Unit): AwaitOr<T> {
        @Suppress("UNCHECKED_CAST")
        val flow = FlowImpl(
            entity as KClass<T>,
            this
        ).apply(path)
        children.add(flow)
        return this
    }

    override fun and(path: Execution<T>.() -> Unit): ExecuteAnd<T> {
        @Suppress("UNCHECKED_CAST")
        val flow = FlowImpl(
            entity as KClass<T>,
            this
        ).apply(path)
        children.add(flow)
        return this
    }

    override fun invoke(case: String): Select<T> {
        this.label = case
        return this
    }

}
