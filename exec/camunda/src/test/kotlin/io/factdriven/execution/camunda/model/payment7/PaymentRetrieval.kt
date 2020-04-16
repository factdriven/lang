package io.factdriven.execution.camunda.model.payment7

import io.factdriven.flow
import java.util.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class PaymentRetrieval(fact: RetrievePayment) {

    var id = UUID.randomUUID().toString()
    var total = fact.amount
    var covered = 0F

    companion object {

        init {

            flow<PaymentRetrieval> {

                on command RetrievePayment::class

                select ("Traffic light?") either {
                    given ("Green")
                    execute all {
                        execute command ChargeCreditCard::class by {
                            ChargeCreditCard(
                                id,
                                total - covered
                            )
                        }
                    } and {
                        execute command ChargeCreditCard::class by {
                            ChargeCreditCard(
                                id,
                                total - covered
                            )
                        }
                        select all {
                            given("A") condition { true }
                            execute command ChargeCreditCard::class by {
                                ChargeCreditCard(
                                    id,
                                    total - covered
                                )
                            }
                        } or {
                            given("B") condition { true }
                            execute command ChargeCreditCard::class by {
                                ChargeCreditCard(
                                    id,
                                    total - covered
                                )
                            }
                            execute command ChargeCreditCard::class by {
                                ChargeCreditCard(
                                    id,
                                    total - covered
                                )
                            }
                        }
                    }
                    execute command ChargeCreditCard::class by {
                        ChargeCreditCard(
                            id,
                            total - covered
                        )
                    }
                } or {
                    given ("Red") condition { true }
                    execute command ChargeCreditCard::class by {
                        ChargeCreditCard(
                            id,
                            total - covered
                        )
                    }
                    select ("End game?") either {
                        given ("Yes") condition { true }
                        execute command ChargeCreditCard::class by {
                            ChargeCreditCard(
                                id,
                                total - covered
                            )
                        }
                    } or {
                        given ("No") condition { true }
                        execute command ChargeCreditCard::class by {
                            ChargeCreditCard(
                                id,
                                total - covered
                            )
                        }
                        execute command ChargeCreditCard::class by {
                            ChargeCreditCard(
                                id,
                                total - covered
                            )
                        }
                    }
                    execute command ChargeCreditCard::class by {
                        ChargeCreditCard(
                            id,
                            total - covered
                        )
                    }
                }

                emit event PaymentRetrieved::class by {
                    PaymentRetrieved(total)
                }

            }

        }

    }

}

data class RetrievePayment(val amount: Float)
data class PaymentRetrieved(val amount: Float)
