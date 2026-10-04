package it.matato.dietreminder.data.repository.contracts

import it.matato.dietreminder.data.model.VersionPolicy

interface VersionPolicyRepository {
	suspend fun fetchPolicy(): Pair<VersionPolicy, Boolean>
}
