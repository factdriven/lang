package io.factdriven.visualization.examples.payment2

import io.factdriven.definition.Flows
import io.factdriven.visualization.render
import org.junit.jupiter.api.Test

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class PaymentRetrievalTest {

    @Test
    fun testView() {
        render(Flows.init(PaymentRetrieval::class))
    }

}