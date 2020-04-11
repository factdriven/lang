package io.factdriven.aws.translation

import com.amazonaws.services.stepfunctions.builder.StateMachine
import com.amazonaws.services.stepfunctions.builder.StepFunctionBuilder
import com.amazonaws.services.stepfunctions.builder.StepFunctionBuilder.next
import com.amazonaws.services.stepfunctions.builder.conditions.NumericEqualsCondition
import com.amazonaws.services.stepfunctions.builder.states.Choice
import com.amazonaws.services.stepfunctions.builder.states.ChoiceState
import io.factdriven.definition.Branching
import io.factdriven.definition.Flow
import io.factdriven.definition.Node

class FlowTranslator {

    companion object {
        fun translate(flow: Flow) : StateMachine {
            val stateMachineBuilder =  StepFunctionBuilder.stateMachine()
            FlowTranslator().translateToStateMachine(stateMachineBuilder, flow)
            return stateMachineBuilder.build()
        }
    }

    private fun translateToStateMachine(stateMachineBuilder: StateMachine.Builder, flow: Flow){
        translateTraverse(flow, stateMachineBuilder)
    }

    private fun translateTraverse(node: Node?, stateMachineBuilder: StateMachine.Builder) {
        var currentTraverse = node
        while (currentTraverse != null) {
            if (currentTraverse is Branching) {
                translateBlock(stateMachineBuilder, currentTraverse)
            } else if (!(currentTraverse is Flow)) {
                if (currentTraverse.isFirstChild()) {
                    stateMachineBuilder.startAt(currentTraverse.id)
                }
                translate(stateMachineBuilder, currentTraverse)
            }
            currentTraverse = currentTraverse.next
        }
    }

    private fun translateBlock(stateMachineBuilder: StateMachine.Builder, currentTraverse: Branching) {

        val choices = ArrayList<Choice.Builder>()

        for(path in currentTraverse.children){
            val transition = Choice.builder().condition(NumericEqualsCondition.builder()
                    .expectedValue(0L).variable("$.output.my"))
                    .transition(next(path.next?.id))
            choices.add(transition)
        }

        stateMachineBuilder.state(currentTraverse.id,
                ChoiceState.builder().choices(*choices.map { it }.toTypedArray()))

        translateTraverse(currentTraverse.next, stateMachineBuilder)
    }

    private fun translate(stateMachineBuilder: StateMachine.Builder, traverse: Node){
        val strategy = determineTranslationStrategy(traverse)
        strategy.translate(stateMachineBuilder, traverse)
    }

    private fun determineTranslationStrategy(node: Node): FlowTranslationStrategy<Node> {
        return ExecuteTranslationStrategy()
    }
}