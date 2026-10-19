package it.matato.dietreminder.data.repository.delegating

class OfflineWriteException : IllegalStateException("Writes are disabled in offline read-only mode.")

internal fun requireOnlineWrites(isOfflineProvider: () -> Boolean) {
	if (isOfflineProvider()) {
		throw OfflineWriteException()
	}
}
