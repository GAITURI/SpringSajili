package sajili.agent.erp

import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import sajili.agent.inventory.entity.InventoryTransactionEntity

@Component
class MicrosoftErpClient(
    private val erpRestClient: RestClient,
    private val mapper: ErpCodeMapperService
) {

    fun postItemJournalEntry(tx: InventoryTransactionEntity){
        val payload = mapOf(
            "Document_No" to tx.id.toString(),
            "Item_No" to mapper.resolveItemCode(tx.productId),
            "Location_Code" to mapper.resolveWarehouseCode(tx.warehouseId),
            "Quantity" to tx.quantity,
            "Entry_type" to tx.transactionType,
            "External_Document_No" to tx.idempotencyKey.toString()
        )

        erpRestClient.post()
            .uri("v2.0/companies({companyId}/ItemJournalLines")
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .onStatus({it.is4xxClientError || it.is5xxServerError}) {_, response ->
                val body = response.body.readAllBytes().toString(Charsets.UTF_8)
                throw ErpApiException(response.statusCode.value(),body)

            }
            .toBodilessEntity()
    }
    class ErpApiException(val statusCode:Int, message:String): RuntimeException(message)
}