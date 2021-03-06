package org.factscript.language.definition.loop

import org.factscript.language.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
data class CustomerAccount(val fact: CreateAccount) {

    private val name: String = fact.name
    private var balance: Float = 0F
    private var pending: Float = 0F

    fun apply(fact: WithdrawAmount) {
        pending = fact.amount
    }

    fun apply(fact: AmountWithdrawn) {
        pending = 0F
        balance += fact.amount
    }

    companion object {

        init {

            flow<CustomerAccount> {

                on command WithdrawAmount::class emit {
                    success event AmountWithdrawn::class
                }

                emit event {
                    AmountWithdrawn(name, pending)
                }

            }

        }

    }

}

data class CreateAccount(val name: String)
data class WithdrawAmount(val name: String, val amount: Float)
data class AmountWithdrawn(val name: String, val amount: Float)
