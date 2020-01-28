package io.factdriven.view.payment2

import io.factdriven.def.Definition
import io.factdriven.view.render
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class PaymentRetrievalTest {

    @Test
    fun testView() {

        render(Definition.getDefinitionByType(PaymentRetrieval::class))

    }

}