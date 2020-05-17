package io.factdriven.language.execution.aws.translation

import com.amazonaws.services.stepfunctions.builder.StateMachine
import com.amazonaws.services.stepfunctions.builder.StepFunctionBuilder
import io.factdriven.language.definition.Flow
import io.factdriven.language.definition.Node
import java.util.stream.Collectors
import java.util.stream.Stream

open class FlowTranslator {
    companion object {
        fun translate(flow: Flow, lambdaFunction: LambdaFunction) : TranslationResult {
            val stateMachineBuilder =  StepFunctionBuilder.stateMachine()
            val translationContext = TranslationContext.of(lambdaFunction,
                    SequentialTransitionStrategy(),
                    StateMachineBuilder(stateMachineBuilder),
                    SnsContext(namespace = "arn:aws:sns:eu-central-1:162654447161:"))
            FlowTranslator().translateGraph(translationContext, flow)
            return TranslationResult(stateMachineBuilder.build(), translationContext)
        }
    }

    open fun translateGraph(translationContext: TranslationContext, node: Node?) {
        if(node != null) {
            val strategy = determineTranslationStrategy(node)
            strategy.translate(translationContext, node)
            translateGraph(translationContext, node.nextSibling)
        }
    }

    private fun determineTranslationStrategy(node: Node): StepFunctionTranslationStrategy {
        val matchingStrategies = Stream.of(*getTranslators())
                .filter { strategy -> strategy.test(node) }
                .collect(Collectors.toList())

        when (matchingStrategies.size) {
            1 -> return matchingStrategies[0]
            0 -> throw NoMatchingTranslatorFoundException(node)
            else -> throw MultipleMatchingStrategiesFoundExeption(matchingStrategies)
        }
    }

    open fun getTranslators() : Array<StepFunctionTranslationStrategy> {
        return arrayOf(
                PromisingTranslationStrategy(this),
//                ThrowingTranslationStrategy(this),
                LoopTranslationStrategy(this),
                ParallelTranslationStrategy(this),
                SkipTranslationStrategy(this),
                FlowTranslationStrategy(this),
                ExclusiveTranslationStrategy(this),
                InclusiveTranslationStrategy(this),
                ExecuteTranslationStrategy(this)
        )
    }
}