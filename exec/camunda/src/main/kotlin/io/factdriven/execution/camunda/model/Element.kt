package io.factdriven.execution.camunda.model

import io.factdriven.definition.Node

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
abstract class Element<IN: Node, OUT: Any>(val node: IN, open val parent: Element<*,*>? = null) {

    internal abstract val children: List<Element<*,*>>

    internal open val paths: List<Path> = emptyList()

    internal abstract val diagram: Any

    internal abstract val model: OUT

    internal val process: BpmnModel get() = parent?.process ?: this as BpmnModel

    open fun toExecutable(): OUT {
        initModel()
        children.forEach { it.toExecutable() }
        paths.forEach { it.toExecutable() }
        return model
    }

    internal abstract fun initModel()

    internal abstract fun initDiagram()

}
