package org.factscript.language.execution.cam.compensation

import org.factscript.execution.*
import org.factscript.language.Flows.activate
import org.factscript.language.execution.cam.*
import org.factscript.language.visualization.bpmn.model.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class FulfillmentTest: TestHelper() {

    private val kClass = arrayOf(Fulfillment::class, Inventory1::class, Inventory2::class, Payment::class, Account1::class, Account2::class, CreditCard::class, Shipment::class)
    private lateinit var fact: Any
    private lateinit var id: String

    init { activate(*kClass).forEach { script -> BpmnModel(script).toTempFile(true) }  }

    @Test
    fun testOrderCoveredByCustomerAccountbalance() {

        val orderId = "orderId"
        val accountId = "kermit"
        val charge = 5F

        fact = FulfillOrder(orderId, accountId, charge)
        id = send(Fulfillment::class, fact)

        val instance = Fulfillment::class.load(id)

        assertEquals(orderId, instance.orderId)
        assertEquals(accountId, instance.accountId)
        assertEquals(charge, instance.total)

        assertEquals(true, instance.started)
        assertEquals(true, instance.fetched)
        assertEquals(true, instance.paid)
        assertEquals(true, instance.readyToShip)
        assertEquals(true, instance.shipped)
        assertEquals(true, instance.fulfilled)

    }

    @Test
    fun testPaymentCoveredByValidCreditCard() {

        val orderId = "orderId"
        val accountId = "kermit"
        val charge = 10F

        fact = FulfillOrder(orderId, accountId, charge)
        id = send(Fulfillment::class, fact)

        var instance = Fulfillment::class.load(id)

        assertEquals(orderId, instance.orderId)
        assertEquals(accountId, instance.accountId)
        assertEquals(charge, instance.total)

        assertEquals(true, instance.started)
        assertEquals(true, instance.fetched)
        assertEquals(false, instance.paid)
        assertEquals(false, instance.readyToShip)
        assertEquals(false, instance.shipped)
        assertEquals(false, instance.fulfilled)

        fact = ConfirmationReceived(orderId, true)
        send(CreditCard::class, fact)

        instance = Fulfillment::class.load(id)

        assertEquals(true, instance.started)
        assertEquals(true, instance.fetched)
        assertEquals(true, instance.paid)
        assertEquals(true, instance.readyToShip)
        assertEquals(true, instance.shipped)
        assertEquals(true, instance.fulfilled)

    }

    @Test
    fun testPaymentCoveredAfterUpdatingExpiredCreditCard() {

        val orderId = "orderId"
        val accountId = "kermit"
        val charge = 10F

        fact = FulfillOrder(orderId, accountId, charge)
        id = send(Fulfillment::class, fact)

        var instance = Fulfillment::class.load(id)

        assertEquals(orderId, instance.orderId)
        assertEquals(accountId, instance.accountId)
        assertEquals(charge, instance.total)

        assertEquals(true, instance.started)
        assertEquals(true, instance.fetched)
        assertEquals(false, instance.paid)
        assertEquals(false, instance.readyToShip)
        assertEquals(false, instance.shipped)
        assertEquals(false, instance.fulfilled)

        fact = ConfirmationReceived(orderId, false)
        send(CreditCard::class, fact)

        instance = Fulfillment::class.load(id)

        assertEquals(true, instance.started)
        assertEquals(true, instance.fetched)
        assertEquals(false, instance.paid)
        assertEquals(false, instance.readyToShip)
        assertEquals(false, instance.shipped)
        assertEquals(false, instance.fulfilled)

        fact = CreditCardDetailsUpdated(accountId)
        send(Payment::class, fact)
        fact = ConfirmationReceived(orderId, true)
        send(CreditCard::class, fact)

        instance = Fulfillment::class.load(id)

        assertEquals(true, instance.started)
        assertEquals(true, instance.fetched)
        assertEquals(true, instance.paid)
        assertEquals(true, instance.readyToShip)
        assertEquals(true, instance.shipped)
        assertEquals(true, instance.fulfilled)

    }

    @Test
    fun testPaymentFailed() {

        val orderId = "orderId"
        val accountId = "kermit"
        val charge = 10F

        fact = FulfillOrder(orderId, accountId, charge)
        id = send(Fulfillment::class, fact)

        var instance = Fulfillment::class.load(id)

        assertEquals(orderId, instance.orderId)
        assertEquals(accountId, instance.accountId)
        assertEquals(charge, instance.total)

        assertEquals(true, instance.started)
        assertEquals(true, instance.fetched)
        assertEquals(false, instance.paid)
        assertEquals(false, instance.readyToShip)
        assertEquals(false, instance.shipped)
        assertEquals(false, instance.fulfilled)

        fact = ConfirmationReceived(orderId, false)
        send(CreditCard::class, fact)

        instance = Fulfillment::class.load(id)

        assertEquals(true, instance.started)
        assertEquals(true, instance.fetched)
        assertEquals(false, instance.paid)
        assertEquals(false, instance.readyToShip)
        assertEquals(false, instance.shipped)
        assertEquals(false, instance.fulfilled)

        sleep(60, 5000)

        instance = Fulfillment::class.load(id)

        assertEquals(true, instance.started)
        assertEquals(false, instance.fetched)
        assertEquals(false, instance.paid)
        assertEquals(false, instance.readyToShip)
        assertEquals(false, instance.shipped)
        assertEquals(false, instance.fulfilled)

    }

}