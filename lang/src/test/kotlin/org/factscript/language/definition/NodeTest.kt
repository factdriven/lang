package org.factscript.language.definition

import org.factscript.language.*
import org.factscript.language.Flows.activate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class NodeTest {

    init { activate(NodeTestFlow::class, CreditCardCharge::class) }

    @Test
    fun testLevel1Forward() {

        var node = Flows.get(NodeTestFlow::class).start as Node?
        assertTrue(node is Promising)
        assertTrue(node!!.isFirstSibling())
        assertTrue(node.isStart())

        node = node.forward
        assertTrue(node is Branching)

        node = node?.forward
        assertTrue(node is Throwing)
        assertTrue(node!!.isLastSibling())
        assertTrue(node.isFinish())

        node = node.forward
        assertTrue(node == null)

    }

    @Test
    fun testLevel1Backward() {

        var node = Flows.get(NodeTestFlow::class).finish as Node?
        assertTrue(node is Throwing)

        node = node?.backward
        assertTrue(node is Branching)
        assertTrue(!node!!.isFirstSibling())
        assertTrue(!node.isStart())

        node = node.backward
        assertTrue(node is Promising)
        assertTrue(node!!.isFirstSibling())
        assertTrue(node.isStart())

        node = node.backward
        assertTrue(node == null)

    }

    @Test
    fun testLevel2Forward() {

        var node = Flows.get(NodeTestFlow::class).start.forward!!.children.first().children.first() as Node?
        assertTrue(node is Executing)
        assertTrue(!node!!.isLastSibling())
        assertTrue(!node.isFinish())

        node = node.forward
        assertTrue(node is Executing)
        assertTrue(node!!.isLastSibling())
        assertTrue(!node.isFinish())

        node = node.forward
        assertTrue(node is Throwing)
        assertTrue(node!!.isLastSibling())
        assertTrue(node.isFinish())

        node = node.forward
        assertTrue(node == null)

    }

    @Test
    fun testLevel2Backward() {

        var node = Flows.get(NodeTestFlow::class).start.forward!!.children.first().children.first().forward as Node?
        assertTrue(node is Executing)
        assertTrue(!node!!.isFirstSibling())
        assertTrue(!node.isStart())

        node = node.backward
        assertTrue(node is Executing)
        assertTrue(node!!.isFirstSibling())
        assertTrue(!node.isStart())

        node = node.backward
        assertTrue(node is Promising)
        assertTrue(node!!.isFirstSibling())
        assertTrue(node.isStart())

        node = node.backward
        assertTrue(node == null)

    }

    @Test
    fun testLevel3Forward() {

        var node = Flows.get(CreditCardCharge::class).start.forward!!
            .children.first().children.first().forward!!
            .children.first().children.first()

        assertTrue (node is Consuming)
        assertFalse(node.isLastSibling())
        assertFalse(node.isFinish())

        node = node.forward!!

        assertTrue (node is Consuming)
        assertTrue (node.isLastSibling())
        assertFalse(node.isFinish())

        node = node.forward!!

        assertTrue (node is Consuming)
        assertTrue(node.isLastSibling())
        assertFalse(node.isFinish())

        node = node.forward!!

        assertTrue (node is Consuming)
        assertFalse(node.isLastSibling())
        assertFalse(node.isFinish())

        node = node.forward!!

        assertTrue (node is Throwing)
        assertTrue (node.isLastSibling())
        assertTrue (node.isFinish())

    }

}

class NodeTestFlow(fact: RetrievePayment) {

    var id = UUID.randomUUID().toString()
    var total = fact.amount
    var covered = 0F

    companion object {

        init {

            flow<NodeTestFlow> {

                on command RetrievePayment::class

                execute all {
                    execute command {
                        ChargeCreditCard(id, total - covered)
                    }
                    execute command {
                        ChargeCreditCard(id, total - covered)
                    }
                } and {
                    execute command {
                        ChargeCreditCard(id, total - covered)
                    }
                }

                emit event {
                    PaymentRetrieved(total)
                }

            }

        }

    }

}

data class CreditCardCharge(val fact: ChargeCreditCard) {

    private val reference: String = fact.reference
    private val amount: Float = fact.amount
    private var successful: Boolean = false

    fun apply(fact: CreditCardCharged) {
        successful = true
    }

    companion object {

        init {

            flow<CreditCardCharge> {

                on command ChargeCreditCard::class emit {
                    success event CreditCardCharged::class
                }

                execute all {
                    await event (CreditCardGatewayConfirmationReceived::class) having "reference" match { reference }
                    execute all {
                        await event (CreditCardGatewayConfirmationReceived::class) having "reference" match { reference }
                        await event (CreditCardGatewayConfirmationReceived::class) having "reference" match { reference }
                    } and {
                        await event (CreditCardGatewayConfirmationReceived::class) having "reference" match { reference }
                    }
                    await event (CreditCardGatewayConfirmationReceived::class) having "reference" match { reference }
                } and {
                    await event (CreditCardGatewayConfirmationReceived::class) having "reference" match { reference }
                }

                await event (CreditCardGatewayConfirmationReceived::class) having "reference" match { reference }

                emit event {
                    CreditCardCharged(reference, amount)
                }

            }

        }

    }

}

data class ChargeCreditCard(val reference: String, val amount: Float)
data class CreditCardCharged(val reference: String, val amount: Float)
data class CreditCardGatewayConfirmationReceived(val reference: String, val amount: Float)