package sajili.agent.inventory.entity

data class OperationResults <T>(
    val entity:T,
    val isNew: Boolean
)