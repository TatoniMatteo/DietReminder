package it.matato.dietreminder

class TestDietApplication : DietApplication() {
	override fun initServices() {
		// Skip background workers and alarm sync during test initialization
	}
}
