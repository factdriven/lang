package io.factdriven.language.visualization.bpmn.model.raise_failure

import io.factdriven.language.*
import io.factdriven.language.*
import io.factdriven.language.visualization.bpmn.model.BpmnModel
import org.junit.jupiter.api.Test

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class TwoFlows_SecondIsDefaultFlow_SecondIsSucceeding_NoneAreEmpty {

    data class CreditCardCharge(val fact: ChargeCreditCard) {

        private val reference: String = fact.reference
        private val amount: Float = fact.amount
        private var successful: Boolean = false

        fun apply(fact: CreditCardCharged) {
            successful = true
        }

        companion object {

            init {

                flow <CreditCardCharge> {

                    on command ChargeCreditCard::class promise {
                        report success CreditCardCharged::class
                        report failure CreditCardExpired::class
                    }

                    await event (CreditCardGatewayConfirmationReceived::class) having "reference" match { reference }

                    select ("Credit card expired?") either {
                        given ("No") condition { true }
                        execute command ChargeCreditCard::class
                        execute command ChargeCreditCard::class
                        emit event { CreditCardExpired(reference) }
                    } or {
                        given ("Yes")
                        execute command ChargeCreditCard::class
                    }

                    emit event {
                        CreditCardCharged(
                            reference,
                            amount
                        )
                    }

                }

            }

        }

    }

    data class ChargeCreditCard(val reference: String, val amount: Float)
    data class CreditCardCharged(val reference: String, val amount: Float)
    data class CreditCardExpired(val reference: String)
    data class CreditCardGatewayConfirmationReceived(val reference: String, val amount: Float)

    @Test
    fun testView() {
        Flows.initialize(CreditCardCharge::class)
        BpmnModel(Flows.get(CreditCardCharge::class)).toTempFile(true)
    }

}