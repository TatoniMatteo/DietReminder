package it.matato.dietreminder

import it.matato.dietreminder.data.repository.DietRepository
import it.matato.dietreminder.data.repository.FakeDietRepository

class TestDietApplication : DietApplication() {
    override val repository: DietRepository = FakeDietRepository()

    override fun initServices() {
        // Skip background workers and alarm sync during test initialization
    }
}
