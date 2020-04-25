package io.factdriven.language.visualization.bpmn.model

import io.factdriven.language.definition.Conditional
import io.factdriven.language.definition.Node
import io.factdriven.language.visualization.bpmn.diagram.*
import io.factdriven.language.impl.utils.toLines
import org.camunda.bpm.model.bpmn.instance.BaseElement
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnLabel
import org.camunda.bpm.model.bpmn.instance.dc.Bounds
import java.lang.IllegalStateException

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class Label(node: Node, parent: Element<*, out BaseElement>): Element<Node, BpmnLabel>(node, parent) {

    override val model = process.model.newInstance(BpmnLabel::class.java)
    override val diagram: Artefact = Artefact(20,20,0)
    override val children = emptyList<Element<*,*>>()

    override val east: Symbol<*, *> get() = TODO("Not yet implemented")
    override val west: Symbol<*, *> get() = TODO("Not yet implemented")

    override fun initDiagram() {
        // Nothing to initialize
    }

    override fun initModel() {

        (parent!!.model as BaseElement).diagramElement.addChildElement(model)
        process.bpmnProcess.diagramElement.addChildElement((parent.model as BaseElement).diagramElement)

        val diagram = if (parent is Path) diagram else (parent.diagram as Artefact)

        val position = if (parent is Path) when {
            parent.from is Loop || parent.from is Sequence -> (parent.from.diagram as Box).position + (parent.from.diagram as Box).east west 12 north 17
            parent.parent is Loop -> (parent.parent as Loop).fork.diagram.raw.position + (parent.parent as Loop).fork.diagram.raw.north east 10 north 17
            else -> Position((parent.parent!!.diagram as Box).position.x, parent.diagram.waypoints[1].y) west 12 north ((parent.conditional!!.label.toLines().size - 1) * 13 + 20)
        } else when {
            parent is EventSymbol -> diagram.raw.position + diagram.raw.south west diagram.raw.dimension.width / 2 south 6
            parent is TaskSymbol -> diagram.raw.position west 6 south 6
            parent is GatewaySymbol && parent.parent is Branch && (parent.parent as Branch).needsSouthLabel() -> diagram.raw.position south diagram.raw.dimension.height south 6
            parent is GatewaySymbol && parent.parent is Branch && (parent.parent as Branch).needsNorthWestLabel() -> diagram.raw.position north ((node.label.toLines().size - 1) * 13 + 18) west (node.label.toLines().maxBy { it.length }!!.length * 3 + 6)
            parent is GatewaySymbol && parent.parent is Branch -> diagram.raw.position north ((node.label.toLines().size - 1) * 13 + 18)
            parent is GatewaySymbol && parent.parent is Loop -> diagram.raw.position south diagram.raw.dimension.height south 6
            else -> throw IllegalStateException()
        }

        with(process.model.newInstance(Bounds::class.java)) {
            x = position.x.toDouble()
            y = position.y.toDouble()
            height = diagram.raw.dimension.height.toDouble()
            width = diagram.raw.dimension.width.toDouble()
            model.addChildElement(this)
        }

    }

}

fun Branch.needsAdaptedLabel(): Boolean {
    val hasDefaultFlow = vertical.find {
        it.node.children.isEmpty() ||
                (it.node.children.first() is Conditional
                        && (it.node.children.first() as Conditional).condition == null )
    } != null
    val defaultFlowIsFirst = hasDefaultFlow && vertical.first().node.children.first() is Conditional && (vertical.first().node.children.first() as Conditional).condition == null
    return hasDefaultFlow && !defaultFlowIsFirst
}

fun Branch.needsSouthLabel(): Boolean {
    return needsAdaptedLabel() && vertical.size <= 2
}

fun Branch.needsNorthWestLabel(): Boolean {
    return needsAdaptedLabel() && vertical.size > 2
}
