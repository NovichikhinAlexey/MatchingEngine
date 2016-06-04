package com.lykke.matching.engine.services

import com.lykke.matching.engine.daos.AssetPair
import com.lykke.matching.engine.daos.ExternalCashOperation
import com.lykke.matching.engine.daos.WalletOperation
import com.lykke.matching.engine.daos.wallet.Wallet
import com.lykke.matching.engine.database.WalletDatabaseAccessor
import com.lykke.matching.engine.logging.AMOUNT
import com.lykke.matching.engine.logging.ASSET
import com.lykke.matching.engine.logging.BUSSINES_ID
import com.lykke.matching.engine.logging.CLIENT_ID
import com.lykke.matching.engine.logging.KeyValue
import com.lykke.matching.engine.logging.Line
import com.lykke.matching.engine.logging.ME_CASH_OPERATION
import com.lykke.matching.engine.logging.MetricsLogger
import com.lykke.matching.engine.logging.SEND_TO_BITCOIN
import com.lykke.matching.engine.logging.TIMESTAMP
import com.lykke.matching.engine.logging.UID
import com.lykke.matching.engine.messages.MessageWrapper
import com.lykke.matching.engine.messages.ProtocolMessages
import com.lykke.matching.engine.queue.transaction.CashIn
import com.lykke.matching.engine.queue.transaction.CashOut
import com.lykke.matching.engine.queue.transaction.Transaction
import org.apache.log4j.Logger
import java.time.LocalDateTime
import java.util.Date
import java.util.HashMap
import java.util.LinkedList
import java.util.UUID
import java.util.concurrent.BlockingQueue

class CashOperationService(private val walletDatabaseAccessor: WalletDatabaseAccessor, private val backendQueue: BlockingQueue<Transaction>): AbsractService<ProtocolMessages.CashOperation> {

    companion object {
        val LOGGER = Logger.getLogger(CashOperationService::class.java.name)
        val METRICS_LOGGER = MetricsLogger.getLogger()
    }

    private val balances = walletDatabaseAccessor.loadBalances()
    private val wallets = walletDatabaseAccessor.loadWallets()
    private val assetPairs = walletDatabaseAccessor.loadAssetPairs()

    override fun processMessage(messageWrapper: MessageWrapper) {
        val message = parse(messageWrapper.byteArray)
        LOGGER.debug("Processing cash operation (${message.bussinesId}) for client ${message.clientId}, asset ${message.assetId}, amount: ${message.amount}, sendToBitcoin: ${message.sendToBitcoin}")

        val externalCashOperation = walletDatabaseAccessor.loadExternalCashOperation(message.clientId, message.bussinesId)
        if (externalCashOperation != null) {
            messageWrapper.writeResponse(ProtocolMessages.Response.newBuilder().setUid(message.uid).setBussinesId(message.bussinesId)
                    .setRecordId(externalCashOperation.cashOperationId).build())
            LOGGER.debug("Cash operation (${message.bussinesId}) for client ${message.clientId}, asset ${message.assetId}, amount: ${message.amount}, sendToBitcoin: ${message.sendToBitcoin} already processed")
            return
        }

        val operation = WalletOperation(message.clientId, UUID.randomUUID().toString(), message.assetId,
                Date(message.dateTime), message.amount,if (message.sendToBitcoin) UUID.randomUUID().toString() else null)
        processWalletOperations(listOf(operation))

        walletDatabaseAccessor.insertExternalCashOperation(ExternalCashOperation(operation.getClientId(), message.bussinesId, operation.getUid()))

        if (message.sendToBitcoin) {
            val cashOperation = if (operation.amount > 0.0)
                CashIn(operation.transactionId, clientId = operation.getClientId(), Amount = operation.amount, Currency = operation.assetId, cashOperationId = operation.getUid())
            else
                CashOut(operation.transactionId, clientId = operation.getClientId(), Amount = -operation.amount, Currency = operation.assetId, cashOperationId = operation.getUid())

            backendQueue.put(cashOperation)
        }

        messageWrapper.writeResponse(ProtocolMessages.Response.newBuilder().setUid(message.uid).setBussinesId(message.bussinesId)
                .setRecordId(operation.getUid()).build())
        LOGGER.debug("Cash operation (${message.bussinesId}) for client ${message.clientId}, asset ${message.assetId}, amount: ${message.amount}, sendToBitcoin: ${message.sendToBitcoin} processed")

        METRICS_LOGGER.log(Line(ME_CASH_OPERATION, arrayOf(
                KeyValue(UID, message.uid.toString()),
                KeyValue(BUSSINES_ID, message.bussinesId),
                KeyValue(TIMESTAMP, LocalDateTime.now().format(MetricsLogger.DATE_TIME_FORMATTER)),
                KeyValue(CLIENT_ID, message.clientId),
                KeyValue(ASSET, message.assetId),
                KeyValue(AMOUNT, message.amount.toString()),
                KeyValue(SEND_TO_BITCOIN, message.sendToBitcoin.toString())
        )))
    }

    private fun parse(array: ByteArray): ProtocolMessages.CashOperation {
        return ProtocolMessages.CashOperation.parseFrom(array)
    }

    fun getAssetPair(assetPairId: String): AssetPair? {
        var assetPair = assetPairs[assetPairId]
        if (assetPair == null) {
            assetPair = walletDatabaseAccessor.loadAssetPair(assetPairId)
            if (assetPair != null) {
                assetPairs[assetPairId] = assetPair
            }
        }

        LOGGER.debug("Got assetPair : ${assetPair.toString()}")
        return assetPair
    }

    fun getBalance(clientId: String, assetId: String): Double {
        val client = balances[clientId]
        if (client != null) {
            val balance = client[assetId]
            if (balance != null) {
                return balance
            }
        }

        LOGGER.debug("Unable to find balance for client $clientId, asset: $assetId")

        return 0.0
    }

    fun processWalletOperations(operations: List<WalletOperation>) {
        val walletsToAdd = LinkedList<Wallet>()
        operations.forEach { operation ->
            val client = balances.getOrPut(operation.getClientId()) { HashMap<String, Double>() }
            val balance = client[operation.assetId] ?: 0.0
            client.put(operation.assetId, balance + operation.amount)

            val wallet = wallets.getOrPut(operation.getClientId()) { Wallet(operation.getClientId()) }
            wallet.addBalance(operation.assetId, operation.amount)

            if (!walletsToAdd.contains(wallet)) {
                walletsToAdd.add(wallet)
            }
        }

        walletDatabaseAccessor.insertOrUpdateWallets(walletsToAdd)
    }

    fun updateBalance(clientId: String, assetId: String, balance: Double) {
        val client = balances.getOrPut(clientId) { HashMap<String, Double>() }
        client.put(assetId, balance)

        val wallet = wallets.getOrPut(clientId) { Wallet(clientId) }
        wallet.setBalance(assetId, balance)

        walletDatabaseAccessor.insertOrUpdateWallet(wallet)
    }
}