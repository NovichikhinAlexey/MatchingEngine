package com.lykke.matching.engine.outgoing.rabbit.impl.listeners

import com.lykke.matching.engine.database.azure.AzureMessageLogDatabaseAccessor
import com.lykke.matching.engine.logging.MessageDatabaseLogger
import com.lykke.matching.engine.outgoing.messages.JsonSerializable
import com.lykke.matching.engine.outgoing.rabbit.RabbitMqService
import com.lykke.matching.engine.outgoing.rabbit.events.CashSwapEvent
import com.lykke.matching.engine.utils.config.Config
import com.lykke.utils.AppVersion
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import javax.annotation.PostConstruct

@Component
class CashSwapListener {

    private val  queue: BlockingQueue<JsonSerializable> = LinkedBlockingQueue<JsonSerializable>()

    @Autowired
    private lateinit var rabbitMqService: RabbitMqService

    @Autowired
    private lateinit var config: Config

    @Value("\${azure.logs.blob.container}")
    private lateinit var logBlobName: String

    @Value("\${azure.logs.swap.operations.table}")
    private lateinit var logTable: String

    @EventListener
    fun process(cashSwapEvent: CashSwapEvent) {
        queue.put(cashSwapEvent.cashSwapOperation)
    }

    @PostConstruct
    fun initRabbitMqPublisher() {
        rabbitMqService.startPublisher(config.me.rabbitMqConfigs.swapOperations, queue,
                config.me.name,
                AppVersion.VERSION,
                MessageDatabaseLogger(
                        AzureMessageLogDatabaseAccessor(config.me.db.messageLogConnString,
                                logTable, logBlobName)))
    }
}