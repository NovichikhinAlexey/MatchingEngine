package com.lykke.matching.engine.services

import com.lykke.matching.engine.logging.MetricsLogger
import com.lykke.matching.engine.messages.MessageStatus
import com.lykke.matching.engine.messages.MessageWrapper
import com.lykke.matching.engine.messages.ProtocolMessages
import com.lykke.matching.engine.outgoing.messages.JsonSerializable
import com.lykke.matching.engine.outgoing.messages.OrderBook
import org.apache.log4j.Logger
import java.util.Date
import java.util.concurrent.BlockingQueue

class MultiLimitOrderCancelService(val limitOrderService: GenericLimitOrderService,
                             val orderBookQueue: BlockingQueue<OrderBook>,
                             val rabbitOrderBookQueue: BlockingQueue<JsonSerializable>): AbstractService<ProtocolMessages.MultiLimitOrderCancel> {

    companion object {
        val LOGGER = Logger.getLogger(MultiLimitOrderService::class.java.name)
        val METRICS_LOGGER = MetricsLogger.getLogger()
    }

    override fun processMessage(messageWrapper: MessageWrapper) {
        val message = parse(messageWrapper.byteArray)
        LOGGER.debug("Got multi limit order cancel id: ${message.uid}, client ${message.clientId}, assetPair: ${message.assetPairId}, isBuy: ${message.isBuy}")

        val now = Date()

        val ordersToCancel = limitOrderService.getAllPreviousOrders(message.clientId, message.assetPairId, message.isBuy)

        if (ordersToCancel.isNotEmpty()) {
            val orderBook = limitOrderService.getOrderBook(message.assetPairId).copy()

            ordersToCancel.forEach { order ->
                orderBook.removeOrder(order)
            }

            limitOrderService.setOrderBook(message.assetPairId, orderBook)
            limitOrderService.cancelLimitOrders(ordersToCancel)

            val orderBookCopy = orderBook.copy()

            val newOrderBook = OrderBook(message.assetPairId, message.isBuy, now, orderBookCopy.getOrderBook(message.isBuy))
            orderBookQueue.put(newOrderBook)
            rabbitOrderBookQueue.put(newOrderBook)

            messageWrapper.writeNewResponse(ProtocolMessages.NewResponse.newBuilder().setId(message.uid).setStatus(MessageStatus.OK.type).build())
        }

        LOGGER.debug("Multi limit order cancel id: ${message.uid}, client ${message.clientId}, assetPair: ${message.assetPairId}, isBuy: ${message.isBuy} processed")
    }

    private fun parse(array: ByteArray): ProtocolMessages.MultiLimitOrderCancel {
        return ProtocolMessages.MultiLimitOrderCancel.parseFrom(array)
    }
}
