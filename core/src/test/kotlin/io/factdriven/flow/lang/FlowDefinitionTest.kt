package io.factdriven.flow.lang

import io.factdriven.flow.define
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class FlowDefinitionTest {

    private val flow = define <PaymentRetrieval> {

        on message (RetrievePayment::class) create this.progress(PaymentRetrievalAccepted::class) by { PaymentRetrievalAccepted() }

        execute service {

            create intention ChargeCreditCard::class by { ChargeCreditCard() }
            on message (CreditCardCharged::class) having "reference" match { paymentId }

        }

        create success PaymentRetrieved::class by { PaymentRetrieved() }

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

        assertEquals("PaymentRetrieval", flow.id)
        assertEquals("PaymentRetrieval-RetrievePayment-1", flow.children[0].id)
        assertEquals("PaymentRetrieval-ChargeCreditCard-1", flow.children[1].id)
        assertEquals("PaymentRetrieval-ChargeCreditCard-1-ChargeCreditCard-1", (flow.children[1] as DefinedFlow<*>).children[0].id)
        assertEquals("PaymentRetrieval-ChargeCreditCard-1-CreditCardCharged-1", (flow.children[1] as DefinedFlow<*>).children[1].id)
        assertEquals("PaymentRetrieval-PaymentRetrieved-1", flow.children[2].id)

    }

    @Test
    fun testDescendants() {

        val descendants = flow.descendants

        assertEquals(6, descendants.size)

    }

    @Test
    fun testDescendantsMap() {

        val element1 = flow.descendantMap["PaymentRetrieval-RetrievePayment-1"]
        val element2 = flow.descendantMap["PaymentRetrieval-ChargeCreditCard-1"]
        val element22 = flow.descendantMap["PaymentRetrieval-ChargeCreditCard-1-CreditCardCharged-1"]
        val doesNotExist = flow.descendantMap["doesNotExist"]

        assertEquals("PaymentRetrieval-RetrievePayment-1", element1?.id)
        assertEquals("PaymentRetrieval-ChargeCreditCard-1", element2?.id)
        assertEquals("PaymentRetrieval-ChargeCreditCard-1-CreditCardCharged-1", element22?.id)
        assertEquals(null, doesNotExist?.id)

    }

    @Test
    fun testExpectedPatternForRetrievePayment() {

        val aggregate = null
        val retrievePayment = flow.descendantMap["PaymentRetrieval-RetrievePayment-1"] as DefinedMessageReaction

        assertEquals(MessagePattern(RetrievePayment::class), retrievePayment.expected(aggregate))

    }

    @Test
    fun testExpectedPatternForCreditCardCharged() {

        val aggregate = PaymentRetrieval(RetrievePayment(id = "anId", payment = 2F))
        val retrievePayment = flow.descendantMap["PaymentRetrieval-ChargeCreditCard-1-CreditCardCharged-1"] as DefinedMessageReaction

        assertEquals(MessagePattern(CreditCardCharged::class, mapOf("reference" to "anId")), retrievePayment.expected(aggregate))

    }

    @Test
    fun testMessageClass() {

        assertEquals(RetrievePayment::class, flow.messageType("RetrievePayment"))
        assertEquals(PaymentRetrievalAccepted::class, flow.messageType("PaymentRetrievalAccepted"))
        assertEquals(ChargeCreditCard::class, flow.messageType("ChargeCreditCard"))
        assertEquals(CreditCardCharged::class, flow.messageType("CreditCardCharged"))
        assertEquals(PaymentRetrieved::class, flow.messageType("PaymentRetrieved"))

        assertEquals(null, flow.messageType("NonExistingMessageName"))

    }

    @Test
    fun testSerialisation() {

        val original = listOf<Message<*>>(
            Message(RetrievePayment(id="anId", payment = 1F)),
            Message(PaymentRetrievalAccepted(paymentId = "anId")),
            Message(ChargeCreditCard(reference = "anId", payment = 1F)),
            Message(CreditCardCharged(reference = "anId")),
            Message(PaymentRetrieved(paymentId = "anId"))
        )

        val serialized = flow.serialize(original)
        val deserialised = flow.deserialize(serialized)

        assertEquals(original, deserialised)

    }

    @Test
    fun testAggregateWithOneMessage() {

        val messages = listOf(
            Message(RetrievePayment(id = "anId", accountId = "anAccountId", payment = 3F))
        )
        val aggregate = flow.aggregate(messages)

        assertEquals("anId", aggregate.paymentId)
        assertEquals("anAccountId", aggregate.accountId)
        assertEquals(3F, aggregate.uncovered)
        assertEquals(0F, aggregate.covered)

    }

    @Test
    fun testAggregateWithTwoMessages() {

        val messages = listOf(
            Message(RetrievePayment(id = "anId", accountId = "anAccountId", payment = 3F)),
            Message(PaymentRetrieved())
        )
        val aggregate = flow.aggregate(messages)

        assertEquals("anId", aggregate.paymentId)
        assertEquals("anAccountId", aggregate.accountId)
        assertEquals(0F, aggregate.uncovered)
        assertEquals(3F, aggregate.covered)

    }

    @Test
    fun testAggregateWithThreeMessages() {

        val messages = listOf(
            Message(RetrievePayment(id = "anId", accountId = "anAccountId", payment = 3F)),
            Message(PaymentRetrieved(payment = 1F)),
            Message(PaymentRetrieved(payment = 1F))
        )
        val aggregate = flow.aggregate(messages)

        assertEquals("anId", aggregate.paymentId)
        assertEquals("anAccountId", aggregate.accountId)
        assertEquals(1F, aggregate.uncovered)
        assertEquals(2F, aggregate.covered)

    }

}