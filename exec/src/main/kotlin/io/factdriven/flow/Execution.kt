package io.factdriven.flow

import io.factdriven.flow.lang.*
import kotlin.reflect.KClass

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
/**
 * Reconstruct the past flow instance state based on a given history of messages.
 * @param history of (consumed and produced) messages
 * @param flow definition
 * @return instance summarizing the state of a specific flow
 */
fun <I: FlowInstance> past(history: FlowMessages, flow: FlowExecution<I>): I {
    val type = flow.asDefinition().instanceType
    val constructor = type.constructors.find { it.parameters.size == 1 && it.parameters[0].type.classifier as KClass<*> == type }
    TODO()
}

/**
 * Produce new "present" messages based on a given history of messages and a trigger.
 * @param history of (consumed and produced) messages
 * @param flow definition
 * @param trigger coming in and influencing the flow instance
 * @return new messages produced
 */
fun <I: FlowInstance> present(history: FlowMessages, flow: FlowExecution<I>, trigger: FlowMessagePayload): FlowMessages {
    TODO()
}

/**
 * Produce a list of future matching patternBuilders matching in the future based on a given history of messages and a trigger.
 * @param history of (consumed and produced) messages
 * @param flow definition
 * @param trigger coming in and influencing the flow instance
 * @return future matching patternBuilders
 */
fun <I: FlowInstance> future(history: FlowMessages, flow: FlowExecution<I>, trigger: FlowMessagePayload): FlowMessagePatterns {
    TODO()
}

/**
 * Produce a list of potential patternBuilders for an incoming message.
 * @param flow definition
 * @param trigger coming in and potentially influencing many flow instances
 * @return matching patternBuilders
 */
fun <I: FlowInstance> potential(flow: FlowExecution<I>, trigger: FlowMessagePayload): FlowMessagePatterns {
    TODO()
}

/**
 * Determine a list of flow instances currently matching to given patterns.
 * @param patterns
 * @return matching flow instances
 */
fun <I: FlowInstance> determine(patterns: FlowMessagePatterns): FlowInstanceIds {
    TODO()
}

interface ExecutableFlowDefinition: FlowDefinition {

    val instances: FlowInstances

    fun patterns(message: FlowMessagePayload): List<FlowMessagePattern>

}

interface FlowDefinitions {

    fun all(): List<ExecutableFlowDefinition>
    fun get(id: FlowElementName): ExecutableFlowDefinition
    // fun add(definition: ExecutableFlowDefinition): List<ExecutableFlowDefinition>

}

interface FlowInstances {

    fun get(id: FlowInstanceId): FlowInstance
    fun find(pattern: FlowMessagePattern): FlowInstanceIds

}

interface FlowMessageCorrelator {

    val definitions: FlowDefinitions

    fun process(incoming: FlowMessage) {

        if (incoming.target == null) {

            // Implicit targeting means that all correlations will happen synchronously
            target(incoming).forEach { target -> process(FlowMessage(incoming, target)) }

        } else {

            // Begin transaction for message handling
            // (A flow definition specific transaction mechanism may be provided)

            // -. Retrieve flow definition by means of flow definition id.
            val flowDefinition = definitions.get(incoming.target!!.first)

            // -. Load flow instance history by means of the target flow instance id.
            //    (A flow definition specific loading mechanism may be provided)
            val flowInstance = flowDefinition.instances.get(incoming.target!!.second)

            // -. Reconstruct flow instance status by means of flow instance history.
            //    (A flow definition specific reconstruction mechanism may be provided)

            // -. Correlate message to flow instance and retrieve outgoing messages.
            //    (A flow definition specific correlation mechanism may be provided)

            // -. Append incoming message and outgoing reaction messages to store.
            //    (A flow definition specific appending mechanism may be provided)
            // -. Commit unit of work for message correlation phase in case everything goes smoothly
            //    If temporary failure, feed message into incoming queue and configure retry
            //    If permanent failure, feed message into error queue

            // -. Commit unit of work for message handling in case everything goes smoothly
            //    (A flow definition specific transaction mechanism may be provided)

        }

    }

    fun target(message: FlowMessage) : List<FlowMessageTarget> {

        return definitions.all().map { definition ->

            // Retrieve message correlation keys by means of (all) flow definitions and incoming
            // message. Lookup correlating flow instance ids by means of message correlation keys.
            // (A flow definition specific lookup mechanism may be provided)

            definition.patterns(message).map {
                definition.instances.find(it)
            }.flatten().map {
                Pair(definition.name, it)
            }

        }.flatten()

    }

    fun correlate(incoming: FlowMessage): FlowMessages {
        TODO()
    }

}
