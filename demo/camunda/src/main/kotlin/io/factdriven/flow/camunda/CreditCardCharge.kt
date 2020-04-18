package io.factdriven.flow.camunda

import io.factdriven.flow

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
data class CreditCardCharge (

    val reference: String,
    val charge: Float,
    var successful: Boolean = false

){

    constructor(fact: ChargeCreditCard): this(fact.reference, fact.charge)

    fun apply(fact: CreditCardCharged) {
        successful = true
    }

    companion object {

        fun init() {

            flow<CreditCardCharge> {

                on command ChargeCreditCard::class promise {
                    report success CreditCardCharged::class
                }

                await event ConfirmationReceived::class having "reference" match { reference }

                emit event CreditCardCharged::class by {
                    CreditCardCharged(reference = reference, charge = charge)
                }

            }

        }

    }
}


data class ChargeCreditCard(val reference: String, val charge: Float)
data class ConfirmationReceived(val reference: String)
data class CreditCardCharged(val reference: String, val charge: Float)