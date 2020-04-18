package io.factdriven.execution.camunda.engine.issue_command

import io.factdriven.Flows
import io.factdriven.execution.camunda.engine.TestHelper
import io.factdriven.execution.load
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class PaymentRetrievalTest: TestHelper() {

    init {
        Flows.initialize(
            PaymentRetrieval::class,
            CreditCardCharge::class
        )
    }

    @Test
    fun test() {

        val id = send(PaymentRetrieval::class, RetrievePayment(amount = 5F))
        var paymentRetrieval = PaymentRetrieval::class.load(id)
        Assertions.assertEquals(5F, paymentRetrieval.amount)
        Assertions.assertEquals(false, paymentRetrieval.retrieved)

        send(CreditCardCharge::class, CreditCardGatewayConfirmationReceived(reference = paymentRetrieval.reference, amount = paymentRetrieval.amount))

        paymentRetrieval = PaymentRetrieval::class.load(id)
        Assertions.assertEquals(5F, paymentRetrieval.amount)
        Assertions.assertEquals(true, paymentRetrieval.retrieved)

    }

}