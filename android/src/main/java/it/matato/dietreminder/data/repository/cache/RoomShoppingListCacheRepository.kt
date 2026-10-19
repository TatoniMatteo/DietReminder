package it.matato.dietreminder.data.repository.cache

import it.matato.dietreminder.data.repository.contracts.ShoppingListReadRepository

class RoomShoppingListCacheRepository(
	roomRepository: ShoppingListReadRepository,
) : ShoppingListReadRepository by roomRepository
