package com.lykke.matching.engine.database.common.entity

import com.lykke.matching.engine.deduplication.ProcessedMessage
import org.springframework.util.CollectionUtils

class PersistenceData(val balancesData: BalancesData?,
                      val processedMessage: ProcessedMessage? = null,
                      val orderBooksData: OrderBooksPersistenceData?,
                      val stopOrderBooksData: OrderBooksPersistenceData?,
                      val messageSequenceNumber: Long?) {

    constructor(processedMessage: ProcessedMessage?, messageSequenceNumber: Long?) : this(null, processedMessage, null, null, messageSequenceNumber)
    constructor(processedMessage: ProcessedMessage?) : this(null, processedMessage, null, null, null)

    fun details(): String {
        val result = StringBuilder()
        append(result, "m: ", processedMessage?.messageId)
        append(result, "w: ", balancesData?.wallets?.size)
        append(result, "b: ", balancesData?.balances?.size)
        append(result, "o: ", orderBooksData?.orderBooks?.size)
        append(result, "so: ", stopOrderBooksData?.orderBooks?.size)
        append(result, "sn: ", messageSequenceNumber)
        return result.toString()
    }

    fun isEmpty(): Boolean {
        return isEmptyWithoutOrders() &&
                isOrdersEmpty()
    }

    fun isOrdersEmpty(): Boolean {
        return (orderBooksData == null || orderBooksData.isEmpty()) &&
                (stopOrderBooksData == null || stopOrderBooksData.isEmpty())
    }

    fun isEmptyWithoutOrders(): Boolean {
        return CollectionUtils.isEmpty(balancesData?.balances) &&
                CollectionUtils.isEmpty(balancesData?.wallets) &&
                processedMessage == null &&
                messageSequenceNumber == null
    }

    fun getSummary(): String {
        val result = StringBuilder()

        balancesData?.let {
            result.append("wallets: ${it.wallets.size}, ")
                    .append("balances: ${it.balances.size}, ")
        }

        orderBooksData?.let {
            result.append("order books: ${it.orderBooks.size}, ")
                    .append("orders to save: ${it.ordersToSave.size}, ")
                    .append("orders to remove: ${it.ordersToRemove.size}, ")
        }

        stopOrderBooksData?.let {
            result.append("stop order books: ${it.orderBooks.size}, ")
                    .append("stop orders to save: ${it.ordersToSave.size}, ")
                    .append("stop orders to remove: ${it.ordersToRemove.size}, ")
        }

        messageSequenceNumber?.let {
            result.append("message sequence number: $messageSequenceNumber")
        }


        return result.toString()
    }

    private fun append(builder: StringBuilder, prefix: String, obj: Any?) {
        obj?.let {
            if (builder.isNotEmpty()) {
                builder.append(", ")
            }
            builder.append(prefix).append(obj)
        }
    }
}