package it.matato.dietreminder.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import it.matato.dietreminder.data.database.AppDatabase
import it.matato.dietreminder.data.database.dao.ConfigDao
import it.matato.dietreminder.data.database.dao.CourseDao
import it.matato.dietreminder.data.database.dao.DietDao
import it.matato.dietreminder.data.database.dao.FoodItemDao
import it.matato.dietreminder.data.database.dao.MealDao
import it.matato.dietreminder.data.database.dao.ShoppingListDao
import it.matato.dietreminder.data.repository.cache.RoomDietCacheRepository
import it.matato.dietreminder.data.repository.cache.RoomShoppingListCacheRepository
import it.matato.dietreminder.data.repository.contracts.ConfigRepository
import it.matato.dietreminder.data.repository.contracts.DietRepository
import it.matato.dietreminder.data.repository.contracts.ShoppingListRepository
import it.matato.dietreminder.data.repository.contracts.VersionPolicyRepository
import it.matato.dietreminder.data.repository.delegating.DelegatingDietRepository
import it.matato.dietreminder.data.repository.delegating.DelegatingShoppingListRepository
import it.matato.dietreminder.data.repository.github.GitHubVersionPolicyRepository
import it.matato.dietreminder.data.repository.room.RoomConfigRepository
import it.matato.dietreminder.data.repository.room.RoomDietRepository
import it.matato.dietreminder.data.repository.room.RoomShoppingListRepository
import it.matato.dietreminder.util.UpdateManagerStatus
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

	@Provides
	@Singleton
	fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
		return AppDatabase.create(context)
	}

	@Provides
	fun provideDietDao(database: AppDatabase): DietDao = database.dietDao()

	@Provides
	fun provideMealDao(database: AppDatabase): MealDao = database.mealDao()

	@Provides
	fun provideCourseDao(database: AppDatabase): CourseDao = database.courseDao()

	@Provides
	fun provideFoodItemDao(database: AppDatabase): FoodItemDao = database.foodItemDao()

	@Provides
	fun provideConfigDao(database: AppDatabase): ConfigDao = database.configDao()

	@Provides
	fun provideShoppingListDao(database: AppDatabase): ShoppingListDao = database.shoppingListDao()

	@Provides
	@Singleton
	fun provideDietRepository(
		database: AppDatabase,
		dietDao: DietDao,
		mealDao: MealDao,
		courseDao: CourseDao,
		foodItemDao: FoodItemDao,
	): DietRepository {
		val roomRepo = RoomDietRepository(
			database = database,
			diets = dietDao,
			meals = mealDao,
			courses = courseDao,
			foodItems = foodItemDao,
		)
		return DelegatingDietRepository(
			onlineRepository = roomRepo,
			localCacheRepository = RoomDietCacheRepository(roomRepo),
			isOfflineProvider = { UpdateManagerStatus.writesBlocked },
		)
	}

	@Provides
	@Singleton
	fun provideShoppingListRepository(
		database: AppDatabase,
		shoppingListDao: ShoppingListDao,
		dietDao: DietDao,
		mealDao: MealDao,
	): ShoppingListRepository {
		val roomRepo = RoomShoppingListRepository(
			database = database,
			shoppingLists = shoppingListDao,
			diets = dietDao,
			meals = mealDao,
		)
		return DelegatingShoppingListRepository(
			onlineRepository = roomRepo,
			localCacheRepository = RoomShoppingListCacheRepository(roomRepo),
			isOfflineProvider = { UpdateManagerStatus.writesBlocked },
		)
	}

	@Provides
	@Singleton
	fun provideConfigRepository(
		database: AppDatabase,
		configDao: ConfigDao,
		dietDao: DietDao,
		shoppingListDao: ShoppingListDao,
	): ConfigRepository {
		val roomRepo = RoomConfigRepository(
			database = database,
			config = configDao,
			diets = dietDao,
			shoppingLists = shoppingListDao,
		)
		return roomRepo
	}

	@Provides
	@Singleton
	fun provideVersionPolicyRepository(
		@ApplicationContext context: Context,
	): VersionPolicyRepository {
		return GitHubVersionPolicyRepository(context)
	}
}
