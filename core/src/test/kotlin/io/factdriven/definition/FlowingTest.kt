package io.factdriven.definition

import io.factdriven.execution.Type
import io.factdriven.execution.type
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class FlowingTest {

    init {
        Flows.init(PaymentRetrieval::class)
    }

    @Test
    fun testGetDefinitionByType() {
        assertThrows<IllegalArgumentException> { Flows.get(Any::class) }
        assertDoesNotThrow { Flows.get(PaymentRetrieval::class) }
    }

    @Test
    fun testGetDefinitionByName() {
        assertThrows<IllegalArgumentException> { Flows.get(Any::class.type) }
        assertDoesNotThrow { Flows.get(Type(PaymentRetrieval::class.java.`package`.name, "PaymentRetrieval")) }
    }

}
