package io.factdriven.flowlang.transformation

import io.factdriven.flowlang.execute
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class FlowNodeTranslationTest {

    @Test
    fun translateFlowExecution() {
        val flow = execute<PaymentRetrieval> {
        }
        val gFlow = translate(flow)
        assertEquals(null, gFlow.parent)
        assertEquals(Position(0,0), gFlow.position)
        assertEquals(Dimension(0,0), gFlow.dimension)
    }

    @Test
    fun translateFlowReaction() {
        val flow = execute<PaymentRetrieval> {
            on message type(RetrievePayment::class)
        }
        val gFlow = translate(flow)
        assertEquals(null, gFlow.parent)
        assertEquals(Position(0,0), gFlow.position)
        assertEquals(Dimension(72,72), gFlow.dimension)
    }

    @Test
    fun translateFlowService() {
        val flow = execute<PaymentRetrieval> {
            execute service {
            }
        }
        val gFlow = translate(flow)
        assertEquals(null, gFlow.parent)
        assertEquals(Position(0,0), gFlow.position)
        assertEquals(Dimension(136,116), gFlow.dimension)
    }

    @Test
    fun translateFlowAction() {
        val flow = execute<PaymentRetrieval> {
            create success("Payment retrieved") by {}
        }
        val gFlow = translate(flow)
        assertEquals(null, gFlow.parent)
        assertEquals(Position(0,0), gFlow.position)
        assertEquals(Dimension(72,72), gFlow.dimension)
    }

}