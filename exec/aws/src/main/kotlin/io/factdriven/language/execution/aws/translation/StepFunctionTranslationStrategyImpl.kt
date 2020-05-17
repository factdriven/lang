package io.factdriven.language.execution.aws.translation

import com.amazonaws.services.stepfunctions.builder.StepFunctionBuilder
import com.amazonaws.services.stepfunctions.builder.StepFunctionBuilder.next
import com.amazonaws.services.stepfunctions.builder.conditions.*
import com.amazonaws.services.stepfunctions.builder.states.*
import io.factdriven.language.*
import io.factdriven.language.definition.*
import io.factdriven.language.definition.Junction.*
import io.factdriven.language.impl.utils.prettyJson
import java.util.stream.Collectors

class ExclusiveTranslationStrategy(flowTranslator: FlowTranslator) : StepFunctionTranslationStrategy(flowTranslator) {
    override fun test(node: Node): Boolean {
        return node is Branching && node.fork == One
    }

    override fun translate(translationContext: TranslationContext, node: Node) {
        val payload = Payload(id = node.id)
        val nodeParameter = NodeParameter(functionName = translationContext.lambdaFunction.name, payload = payload)

        translationContext.stepFunctionBuilder.state(toStateName(node),
                StepFunctionBuilder.taskState()
                        .resource(translationContext.lambdaFunction.resource)
                        .parameters(nodeParameter.prettyJson)
                        .resultPath("$.output.condition")
                        .transition(next(nameGateway(node))))

        val choices = toChoices(translationContext, node as Branching)
        translationContext.stepFunctionBuilder.state(nameGateway(node),
                ChoiceState.builder()
                        .choices(*choices)
        )
    }

    private fun nameGateway(node: Node) : String {
        return "gateway-"+toStateName(node)
    }

    private fun toChoices(translationContext: TranslationContext, branching: Branching): Array<Choice.Builder> {
        return branching.children.stream()
                .map {node -> node as Option<*>}
                .peek { conditionalExecution -> flowTranslator.translateGraph(translationContext, conditionalExecution.children.first()) }
                .map {condition ->
                    Choice.builder().condition(toCondition(condition)).transition(translationContext.transitionStrategy.nextTransition(condition.children.first()) as NextStateTransition.Builder?)
                }.collect(Collectors.toList())
                .toTypedArray()
    }

    private fun toCondition(condition: Option<*>): Condition.Builder {
        return StringEqualsCondition.builder().variable("$.output.condition").expectedValue(toStateName(condition))
    }
}

class InclusiveTranslationStrategy(flowTranslator: FlowTranslator) : StepFunctionTranslationStrategy(flowTranslator) {
    override fun test(node: Node): Boolean {
        return node is Branching && node.fork == Some
    }

    override fun translate(translationContext: TranslationContext, node: Node) {
        val payload = InclusivePayload(inclusiveContext = null, id = node.id)
        val nodeParameter = NodeParameter(functionName = translationContext.lambdaFunction.name, payload = payload)

        translationContext.stepFunctionBuilder.state(toStateName(node),
                StepFunctionBuilder.taskState()
                        .resource(translationContext.lambdaFunction.resource)
                        .parameters(nodeParameter.prettyJson)
                        .resultPath("$.InclusiveContext")
                        .transition(next("evaluate-${toStateName(node)}")))

        val evaluationPayload = InclusivePayload(id = node.id)
        val evaluationNodeParameter = NodeParameter(functionName = translationContext.lambdaFunction.name, payload = evaluationPayload)

        translationContext.stepFunctionBuilder.state("evaluate-${toStateName(node)}",
                StepFunctionBuilder.taskState()
                        .resource(translationContext.lambdaFunction.resource)
                        .parameters(evaluationNodeParameter.prettyJson)
                        .resultPath("$.InclusiveContext")
                        .transition(next(nameGateway(node))))

        val choicesTranslationContext = translationContext.copyWith(transitionStrategy = InclusiveTransitionStrategy(node, node.forward))
        val choices = toChoices(choicesTranslationContext, node as Branching)
        translationContext.stepFunctionBuilder.state(nameGateway(node),
                ChoiceState.builder()
                        .choices(*choices)
        )

        translationContext.stepFunctionBuilder.state("while-${toStateName(node)}", ChoiceState.builder().choices(
                Choice.builder().condition(NumericGreaterThanCondition.builder().variable("$.InclusiveContext.next").expectedValue(-1)).transition(next("evaluate-${toStateName(node)}")),
                Choice.builder().condition(NumericEqualsCondition.builder().variable("$.InclusiveContext.next").expectedValue(-1)).transition(translationContext.transitionStrategy.nextTransition(node) as NextStateTransition.Builder)
        ))
    }

    private fun nameGateway(node: Node) : String {
        return "gateway-"+toStateName(node)
    }

    private fun toChoices(translationContext: TranslationContext, branching: Branching): Array<Choice.Builder> {
        return branching.children.stream()
                .map {node -> node as Option<*>}
                .peek { conditionalExecution -> flowTranslator.translateGraph(translationContext, conditionalExecution.children.first()) }
                .map {condition ->
                    Choice.builder().condition(toCondition(branching.children.indexOf(condition))).transition(translationContext.transitionStrategy.nextTransition(condition.children.first()) as NextStateTransition.Builder?)
                }.collect(Collectors.toList())
                .toTypedArray()
    }

    private fun toCondition(index: Int): Condition.Builder {
        return BooleanEqualsCondition.builder().variable("$.InclusiveContext.conditions.$index").expectedValue(true)
    }
}

class ParallelTranslationStrategy(flowTranslator: FlowTranslator) : StepFunctionTranslationStrategy(flowTranslator){
    override fun test(node: Node): Boolean {
        return node is Branching && node.fork == All
    }

    override fun translate(translationContext: TranslationContext, node: Node) {
        val branches = translateBranches(translationContext, node as Branching)
        translationContext.stepFunctionBuilder.state(toStateName(node),
                ParallelState.builder()
                        .branches(*branches)
                        .transition(next("merge-" + toStateName(node)))
        )

        val nodeParameter = NodeParameter(functionName = translationContext.lambdaFunction.name, payload = ParallelMergePayload())

        translationContext.stepFunctionBuilder.state("merge-" + toStateName(node),
                StepFunctionBuilder.taskState()
                        .resource(translationContext.lambdaFunction.resource)
                        .parameters(nodeParameter.prettyJson)
                        .transition(translationContext.transitionStrategy.nextTransition(node))
        )
    }

    private fun translateBranches(translationContext: TranslationContext, branching: Branching): Array<Branch.Builder> {
        val branches = mutableListOf<Branch.Builder>()

        for (subFlow in branching.children) {
            val branch = Branch.builder().startAt(toStateName(subFlow.children.first()))
            val branchTranslationContext = translationContext.copyWith(transitionStrategy = ParallelTransitionStrategy(branching.forward), stepFunctionBuilder = BranchBuilder(branch))
            flowTranslator.translateGraph(branchTranslationContext, subFlow.children.first())
            branches.add(branch)
        }

        return branches.toTypedArray()
    }
}

class FlowTranslationStrategy(flowTranslator: FlowTranslator) : StepFunctionTranslationStrategy(flowTranslator) {
    override fun test(node: Node): Boolean {
        return node is PromisingFlow
    }

    override fun translate(translationContext: TranslationContext, node: Node) {
        translationContext.stepFunctionBuilder.startAt(toStateName(node.children.first()))
        flowTranslator.translateGraph(translationContext, node.children.first())
    }
}

class ExecuteTranslationStrategy(flowTranslator: FlowTranslator) : StepFunctionTranslationStrategy(flowTranslator) {

    override fun test(node: Node) : Boolean{
        return node.parent != null && (node is Execute<*> || node is Throwing)
    }

    override fun translate(translationContext: TranslationContext, node: Node) {
        val payload = Payload(id = node.id)
        val nodeParameter = NodeParameter(functionName = translationContext.lambdaFunction.name, payload = payload)

        if(node is Throwing && node !is Execute<*>){
            translationContext.snsContext.addTopic(node)
        }

        executionTranslation(translationContext, node, nodeParameter)
    }

    private fun executionTranslation(translationContext: TranslationContext, node: Node, nodeParameter: NodeParameter) {
        translationContext.stepFunctionBuilder.state(toStateName(node),
                StepFunctionBuilder.taskState()
                        .resource(translationContext.lambdaFunction.resource)
                        .parameters(nodeParameter.prettyJson)
                        .resultPath("$.Messages")
                        .transition(translationContext.transitionStrategy.nextTransition(node))
        )
    }
}

class PromisingTranslationStrategy(flowTranslator: FlowTranslator) : StepFunctionTranslationStrategy(flowTranslator){
    override fun test(node: Node): Boolean {
        return node is Promising && node !is PromisingFlow && node !is Execute<*>
    }

    override fun translate(translationContext: TranslationContext, node: Node) {
        translationContext.snsContext.addTopic(node)
        translationContext.stepFunctionBuilder.state(toStateName(node),
                PassState.builder()
                        .transition(translationContext.transitionStrategy.nextTransition(node)))
    }
}

class ThrowingTranslationStrategy(flowTranslator: FlowTranslator) : StepFunctionTranslationStrategy(flowTranslator){
    override fun test(node: Node): Boolean {
       return node is Throwing && node !is Execute<*>
    }

    override fun translate(translationContext: TranslationContext, node: Node) {
//        val throwing = node as Throwing
//        val throwingClass = throwing.throwing
//        val snsContext = translationContext.snsContext
//        val topicName = throwingClass.simpleName!!
//        snsContext.topics.add(topicName)
//
//        val snsParameter = SnsParameter(snsContext.getTopicArn(topicName), throwingClass.qualifiedName!!)
//
//        translationContext.stepFunctionBuilder.state(toStateName(node), TaskState.builder()
//                .resource(snsContext.resource)
//                .parameters(snsParameter.prettyJson)
//                .transition(translationContext.transitionStrategy.nextTransition(node)))
    }
}

class LoopTranslationStrategy(flowTranslator: FlowTranslator) : StepFunctionTranslationStrategy(flowTranslator){
    override fun test(node: Node): Boolean {
        return node is Loop<*>
    }

    override fun translate(translationContext: TranslationContext, node: Node) {
        val first = node.children.first()
        val last = node.children[node.children.size-2]//node.children.last { node !is Until<*> }
        val loopTransitionStrategy  = LoopTransitionStrategy(last, "evaluate-${toStateName(node)}")

        val payload = LoopPayload(id = node.id, loopContext = null)
        val nodeParameter = NodeParameter(functionName = translationContext.lambdaFunction.name, payload = payload)

        translationContext.stepFunctionBuilder.state(toStateName(node),
                StepFunctionBuilder.taskState()
                        .resource(translationContext.lambdaFunction.resource)
                        .parameters(nodeParameter.prettyJson)
                        .resultPath("$.LoopContext")
                        .transition(next(toStateName(first))))

        flowTranslator.translateGraph(translationContext.copyWith(transitionStrategy = loopTransitionStrategy), first)

        val evaluationPayload = LoopPayload(id = node.id)
        val evaluationNodeParameter = NodeParameter(functionName = translationContext.lambdaFunction.name, payload = evaluationPayload)

        translationContext.stepFunctionBuilder.state("evaluate-${toStateName(node)}",
                StepFunctionBuilder.taskState()
                        .resource(translationContext.lambdaFunction.resource)
                        .parameters(evaluationNodeParameter.prettyJson)
                        .resultPath("$.LoopContext")
                        .transition(next("while-${toStateName(node)}")))

        translationContext.stepFunctionBuilder.state("while-${toStateName(node)}",
                StepFunctionBuilder.choiceState()
                        .choices(
                                Choice.builder()
                                        .condition(BooleanEqualsCondition.builder()
                                                .variable("$.LoopContext.continue")
                                                .expectedValue(true))
                                        .transition(next(toStateName(first))),
                                Choice.builder()
                                        .condition(BooleanEqualsCondition.builder()
                                                .variable("$.LoopContext.continue")
                                                .expectedValue(false))
                                        .transition(translationContext.transitionStrategy.nextTransition(node) as NextStateTransition.Builder)
                        ))

    }
}

class SkipTranslationStrategy(flowTranslator: FlowTranslator) : StepFunctionTranslationStrategy(flowTranslator){
    override fun test(node: Node): Boolean {
        return node is Given<*> || node is Until <*>
    }

    override fun translate(translationContext: TranslationContext, node: Node) {
        //skip
    }
}

fun toStateName(prefix: String?, node: Node) : String{
    if(prefix != null){
        return "$prefix-${node.description}-${node.id}"
    }
    return "${node.description}-${node.id}"
}

fun toStateName(node: Node) : String{
    return toStateName(null, node)
}