package io.factdriven.flow.camunda

import io.factdriven.execution.Messages
import io.factdriven.language.execution.cam.FactDrivenLanguagePlugin
import io.factdriven.language.execution.cam.EngineMessageProcessor
import io.factdriven.language.execution.cam.EngineMessagePublisher
import io.factdriven.language.execution.cam.EngineMessageStore
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@SpringBootApplication @Configuration
@EnableProcessApplication
class PaymentRetrievalApplication {

    @Bean
    fun camundaFlowExecutionPlugin(): FactDrivenLanguagePlugin {
        return FactDrivenLanguagePlugin()
    }

}

fun main(args: Array<String>) {
    runApplication<PaymentRetrievalApplication>(*args) {
        Messages.register(EngineMessageProcessor())
        Messages.register(EngineMessagePublisher())
        Messages.register(EngineMessageStore())
        PaymentRetrieval.init()
        CreditCardCharge.init()
        CustomerAccount.init()
    }
}
