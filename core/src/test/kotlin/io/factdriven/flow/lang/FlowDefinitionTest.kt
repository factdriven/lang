package io.factdriven.flow.lang

import io.factdriven.flow.define
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class FlowDefinitionTest {

    val flow = define <PaymentRetrieval> {

        on message (RetrievePayment::class) create acceptance("PaymentRetrievalAccepted") by { PaymentRetrievalAccepted() }

        execute service {

            create intent "ChargeCreditCard" by { ChargeCreditCard() }
            on message (CreditCardCharged::class) having "reference" match { paymentId }

        }

        create success "PaymentRetrieved" by { PaymentRetrieved() }

    }

    @Test
    fun testMessagePatternCreditCardCharged() {

        val incoming = CreditCardCharged(reference = "value")
        val patterns: MessagePatterns = flow.patterns(incoming)

        assertEquals (1, patterns.size)
        assertEquals (MessagePattern(type = CreditCardCharged::class, properties = mapOf("reference" to "value")), patterns.iterator().next())

    }

    @Test
    fun testMessagePatternRetrievePayment() {

        val incoming = RetrievePayment(payment = 1F)
        val patterns: MessagePatterns = flow.patterns(incoming)

        assertEquals (1, patterns.size)
        assertEquals (MessagePattern(type = RetrievePayment::class), patterns.iterator().next())

    }

    @Test
    fun testMessagePatternPaymentRetrieved() {

        val incoming = PaymentRetrieved(paymentId = "id")
        val patterns: MessagePatterns = flow.patterns(incoming)

        assertEquals (0, patterns.size)

    }

    @Test
    fun testElementIds () {

        assertEquals("PaymentRetrieval", flow.flowElementId)
        assertEquals("PaymentRetrieval-RetrievePayment", flow.children[0].flowElementId)
        assertEquals("PaymentRetrieval-ChargeCreditCard", flow.children[1].flowElementId)
        assertEquals("PaymentRetrieval-ChargeCreditCard-ChargeCreditCard", (flow.children[1] as FlowDefinition).children[0].flowElementId)
        assertEquals("PaymentRetrieval-ChargeCreditCard-CreditCardCharged", (flow.children[1] as FlowDefinition).children[1].flowElementId)
        assertEquals("PaymentRetrieval-PaymentRetrieved", flow.children[2].flowElementId)

    }

    @Test
    fun testDescendants() {

        val descendants = flow.descendants

        assertEquals(5, descendants.size)

    }

}