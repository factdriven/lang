package io.factdriven.language.examples.payment3

import io.factdriven.Flows
import io.factdriven.definition.Catching
import io.factdriven.definition.Executing
import io.factdriven.definition.Throwing
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class PaymentRetrievalTest {

    init {
        Flows.initialize(PaymentRetrieval::class)
        Flows.initialize(CreditCardCharge::class)
    }

    @Test
    fun testDefinition() {

        val definition = Flows.get(PaymentRetrieval::class)
        Assertions.assertEquals(PaymentRetrieval::class, definition.entity)
        Assertions.assertEquals(3, definition.children.size)

        val instance = PaymentRetrieval(RetrievePayment(3F))

        val on = definition.find(nodeOfType = Catching::class, dealingWith = RetrievePayment::class)
        Assertions.assertEquals(PaymentRetrieval::class, on?.entity)
        Assertions.assertEquals(RetrievePayment::class, on?.catching)
        Assertions.assertEquals(definition, on?.parent)

        val execute = definition.find(nodeOfType = Executing::class, dealingWith = ChargeCreditCard::class)
        Assertions.assertEquals(PaymentRetrieval::class, execute?.entity)
        Assertions.assertEquals(ChargeCreditCard::class, execute?.throwing)
        Assertions.assertEquals(CreditCardCharged::class, execute?.catching)
        Assertions.assertEquals(ChargeCreditCard(instance.id, instance.total), execute?.instance?.invoke(instance))
        Assertions.assertEquals(definition, execute?.parent)

        val emit = definition.find(nodeOfType = Throwing::class, dealingWith = PaymentRetrieved::class)
        Assertions.assertEquals(PaymentRetrieval::class, emit?.entity)
        Assertions.assertEquals(PaymentRetrieved::class, emit?.throwing)
        Assertions.assertEquals(PaymentRetrieved(3F), emit?.instance?.invoke(instance))
        Assertions.assertEquals(definition, emit?.parent)

    }

}