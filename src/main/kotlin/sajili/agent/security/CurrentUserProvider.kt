package sajili.agent.security
//my inventoryservice only knows that it can ask "who is the current user?" via this interface
interface CurrentUserProvider {
    fun getCurrentUser(): AuthenticatedUsers
}