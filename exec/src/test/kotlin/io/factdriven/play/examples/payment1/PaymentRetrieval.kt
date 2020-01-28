package io.factdriven.play.examples.payment1

import io.factdriven.lang.define

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class PaymentRetrieval(fact: RetrievePayment) {

    val amount = fact.amount
    var retrieved = false; private set

    fun apply(fact: PaymentRetrieved) {
        retrieved = true
    }

    companion object {

        init {
            define <PaymentRetrieval> {
                on command RetrievePayment::class
                emit event PaymentRetrieved::class by {
                    PaymentRetrieved(amount)
                }
            }
        }

    }

}

data class RetrievePayment(val amount: Float)
data class PaymentRetrieved(val amount: Float)
