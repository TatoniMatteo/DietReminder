package it.matato.dietreminder.data.repository.cache

import it.matato.dietreminder.data.repository.contracts.DietReadRepository

class RoomDietCacheRepository(
	roomRepository: DietReadRepository,
) : DietReadRepository by roomRepository
