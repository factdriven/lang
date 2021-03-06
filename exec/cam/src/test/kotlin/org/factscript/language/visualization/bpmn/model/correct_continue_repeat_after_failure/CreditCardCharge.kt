package org.factscript.language.visualization.bpmn.model.correct_continue_repeat_after_failure

import org.factscript.language.event
import org.factscript.language.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
data class CreditCardCharge(val fact: ChargeCreditCard) {

    private val reference: String = fact.account
    private val amount: Float = fact.amount
    private var successful: Boolean = false

    fun apply(fact: CreditCardGatewayConfirmationReceived) {
        successful = fact.amount > 0
    }

    companion object {

        init {

            flow <CreditCardCharge> {

                on command ChargeCreditCard::class emit {
                    success event CreditCardCharged::class
                    failure event CreditCardExpired::class
                }

                await event CreditCardGatewayConfirmationReceived::class having "reference" match { reference }

                select ("Credit card expired?") either {
                    given ("No")
                } or {
                    given ("Yes") condition { !successful }
                    emit event { CreditCardExpired(reference) }
                }

                emit event { CreditCardCharged(reference, amount) }

            }

        }

    }

}

data class ChargeCreditCard(val account: String, val amount: Float)
data class CreditCardCharged(val account: String, val amount: Float)
data class CreditCardDetailsUpdated(val account: String, val amount: Float)
data class CreditCardExpired(val account: String)
data class CreditCardGatewayConfirmationReceived(val account: String, val amount: Float)

