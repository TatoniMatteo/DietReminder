package it.matato.dietreminder.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.matato.dietreminder.data.database.entity.ConfigKey
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.database.entity.ShoppingListItem
import it.matato.dietreminder.data.database.relation.CourseWithItems
import it.matato.dietreminder.data.database.relation.MealWithDetails
import it.matato.dietreminder.data.export.DietExport
import it.matato.dietreminder.data.model.AppVersionState
import it.matato.dietreminder.data.model.HydrationRange
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.data.model.QuantityUnit
import it.matato.dietreminder.data.repository.contracts.ConfigRepository
import it.matato.dietreminder.data.repository.contracts.DietImportItemConfig
import it.matato.dietreminder.data.repository.contracts.DietRepository
import it.matato.dietreminder.data.repository.contracts.ShoppingListInitialItem
import it.matato.dietreminder.data.repository.contracts.ShoppingListRepository
import it.matato.dietreminder.data.repository.contracts.VersionPolicyRepository
import it.matato.dietreminder.data.repository.delegating.OfflineWriteException
import it.matato.dietreminder.data.repository.github.GitHubVersionPolicyRepository
import it.matato.dietreminder.domain.IngredientSummary
import it.matato.dietreminder.domain.NextMeal
import it.matato.dietreminder.domain.nextMeal
import it.matato.dietreminder.domain.toIngredientSummaries
import it.matato.dietreminder.util.AppLog
import it.matato.dietreminder.util.UpdateManager
import it.matato.dietreminder.util.UpdateManagerStatus
import it.matato.dietreminder.util.alarm.AlarmScheduler
import it.matato.dietreminder.util.alarm.AlarmSyncHelper
import it.matato.dietreminder.widget.DietReminder
import java.time.DayOfWeek
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

sealed interface ImportCheckResult {
	data object Valid : ImportCheckResult
	data object Conflict : ImportCheckResult
	data class Invalid(val message: String) : ImportCheckResult
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class DietViewModel @Inject constructor(
	private val application: Application,
	private val dietRepository: DietRepository,
	private val shoppingListRepository: ShoppingListRepository,
	private val configRepository: ConfigRepository,
	versionPolicyRepository: VersionPolicyRepository,
) : ViewModel() {

	constructor(
		application: Application,
		repository: DietRepository,
		versionPolicyRepository: VersionPolicyRepository =
			(repository as? VersionPolicyRepository) ?: GitHubVersionPolicyRepository(application),
	) : this(
		application = application,
		dietRepository = repository,
		shoppingListRepository = requireNotNull(repository as? ShoppingListRepository) {
			"Repository must also provide shopping-list operations."
		},
		configRepository = requireNotNull(repository as? ConfigRepository) {
			"Repository must also provide configuration operations."
		},
		versionPolicyRepository = versionPolicyRepository,
	)

	private val updateManager = UpdateManager(versionPolicyRepository)
	val versionStatus = updateManager.versionStatus

	init {
		viewModelScope.launch {
			updateManager.checkUpdate()
		}
	}

	fun checkUpdate() {
		viewModelScope.launch {
			updateManager.checkUpdate()
		}
	}

	fun forceUpdateState(state: AppVersionState?) {
		updateManager.forceState(state)
	}

	val diets = dietRepository.all.stateIn(
		viewModelScope,
		SharingStarted.WhileSubscribed(5000),
		emptyList(),
	)

	val active = dietRepository.active.stateIn(
		viewModelScope,
		SharingStarted.WhileSubscribed(5000),
		null,
	)

	val meals = active
		.flatMapLatest { diet ->
			diet?.let { dietRepository.observeMeals(it.id) } ?: flowOf(emptyList())
		}
		.stateIn(
			viewModelScope,
			SharingStarted.WhileSubscribed(5000),
			emptyList(),
		)

	val existingFoodNames = meals
		.map { mealsList ->
			mealsList
				.flatMap { m -> m.courses.flatMap { c -> c.items.map { i -> i.name.trim() } } }
				.filter { it.isNotBlank() }
				.distinct()
				.sortedWith(String.CASE_INSENSITIVE_ORDER)
		}
		.stateIn(
			viewModelScope,
			SharingStarted.WhileSubscribed(5000),
			emptyList(),
		)

	val defaultTimes = configRepository.defaultTimes.stateIn(
		viewModelScope,
		SharingStarted.WhileSubscribed(5000),
		emptyList(),
	)

	val shoppingLists = shoppingListRepository.allShoppingLists.stateIn(
		viewModelScope,
		SharingStarted.WhileSubscribed(5000),
		emptyList(),
	)

	val theme = configRepository.observeConfig(ConfigKey.THEME)
		.map { it?.value ?: "system" }
		.stateIn(
			viewModelScope,
			SharingStarted.WhileSubscribed(5000),
			"system",
		)

	val useDynamicColors = configRepository.observeConfig(ConfigKey.USE_DYNAMIC_COLORS)
		.map { it?.value != "false" }
		.stateIn(
			viewModelScope,
			SharingStarted.WhileSubscribed(5000),
			true,
		)

	val seedColor = configRepository.observeConfig(ConfigKey.SEED_COLOR)
		.map { it?.value ?: "0xFF6750A4" }
		.stateIn(
			viewModelScope,
			SharingStarted.WhileSubscribed(5000),
			"0xFF6750A4",
		)

	val language = configRepository.observeConfig(ConfigKey.LANGUAGE)
		.map { it?.value ?: "it" }
		.stateIn(
			viewModelScope,
			SharingStarted.WhileSubscribed(5000),
			"it",
		)

	val hydrationRanges = configRepository.observeConfig(ConfigKey.HYDRATION_RANGES)
		.map { config ->
			config?.value?.let { value ->
				try {
					Json.decodeFromString<List<HydrationRange>>(value)
				} catch (_: Exception) {
					defaultHydrationRanges()
				}
			} ?: defaultHydrationRanges()
		}
		.stateIn(
			viewModelScope,
			SharingStarted.WhileSubscribed(5000),
			defaultHydrationRanges(),
		)

	val hydrationEnabled = configRepository.observeConfig(ConfigKey.HYDRATION_ENABLED)
		.map { it?.value == "true" }
		.stateIn(
			viewModelScope,
			SharingStarted.WhileSubscribed(5000),
			true,
		)

	val hydrationInterval = configRepository.observeConfig(ConfigKey.HYDRATION_INTERVAL)
		.map { it?.value?.toIntOrNull() ?: 120 }
		.stateIn(
			viewModelScope,
			SharingStarted.WhileSubscribed(5000),
			120,
		)

	val hydrationDays = configRepository.observeConfig(ConfigKey.HYDRATION_DAYS)
		.map { config ->
			config?.value
				?.split(",")
				?.mapNotNull { runCatching { DayOfWeek.valueOf(it.trim()) }.getOrNull() }
				?.toSet() ?: DayOfWeek.entries.toSet()
		}
		.stateIn(
			viewModelScope,
			SharingStarted.WhileSubscribed(5000),
			DayOfWeek.entries.toSet(),
		)

	val mealRemindersEnabled = configRepository.observeConfig(ConfigKey.MEAL_REMINDERS_ENABLED)
		.map { it?.value != "false" }
		.stateIn(
			viewModelScope,
			SharingStarted.WhileSubscribed(5000),
			true,
		)

	val isDeveloperMode = configRepository.observeConfig(ConfigKey.DEVELOPER_MODE)
		.map { it?.value == "true" }
		.stateIn(
			viewModelScope,
			SharingStarted.WhileSubscribed(5000),
			false,
		)

	val appLogs = AppLog.entries.stateIn(
		viewModelScope,
		SharingStarted.WhileSubscribed(5000),
		emptyList(),
	)

	val lastImportError = mutableStateOf<String?>(null)
	private val _writeBlockedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
	val writeBlockedEvents = _writeBlockedEvents.asSharedFlow()

	private fun launchWrite(action: suspend () -> Unit) {
		viewModelScope.launch {
			if (UpdateManagerStatus.writesBlocked) {
				AppLog.w("Write blocked because the app is in offline read-only mode")
				_writeBlockedEvents.emit(Unit)
				return@launch
			}

			try {
				action()
			} catch (_: OfflineWriteException) {
				AppLog.w("Write blocked because the app switched to offline read-only mode")
				_writeBlockedEvents.emit(Unit)
			}
		}
	}

	private fun launchLocalWrite(action: suspend () -> Unit) {
		viewModelScope.launch {
			action()
		}
	}

	fun setTheme(theme: String) {
		launchLocalWrite {
			configRepository.saveConfig(ConfigKey.THEME, theme)
		}
	}

	fun setUseDynamicColors(enabled: Boolean) {
		launchLocalWrite {
			configRepository.saveConfig(ConfigKey.USE_DYNAMIC_COLORS, enabled.toString())
		}
	}

	fun setSeedColor(color: String) {
		launchLocalWrite {
			configRepository.saveConfig(ConfigKey.SEED_COLOR, color)
		}
	}

	fun setLanguage(language: String) {
		launchLocalWrite {
			configRepository.saveConfig(ConfigKey.LANGUAGE, language)
		}
	}

	fun setHydrationRanges(ranges: List<HydrationRange>) {
		launchLocalWrite {
			configRepository.saveConfig(
				ConfigKey.HYDRATION_RANGES,
				Json.encodeToString(ranges),
			)
		}
	}

	fun setHydrationEnabled(enabled: Boolean) {
		launchLocalWrite {
			configRepository.saveConfig(
				ConfigKey.HYDRATION_ENABLED,
				enabled.toString(),
			)
		}
	}

	fun setHydrationInterval(interval: Int) {
		launchLocalWrite {
			configRepository.saveConfig(
				ConfigKey.HYDRATION_INTERVAL,
				interval.toString(),
			)
		}
	}

	fun setHydrationDays(days: Set<DayOfWeek>) {
		launchLocalWrite {
			configRepository.saveConfig(
				ConfigKey.HYDRATION_DAYS,
				days.joinToString(",") { it.name },
			)
		}
	}

	fun setMealRemindersEnabled(enabled: Boolean) {
		launchLocalWrite {
			configRepository.saveConfig(
				ConfigKey.MEAL_REMINDERS_ENABLED,
				enabled.toString(),
			)
		}
	}

	fun setDietDayNotificationEnabled(dietId: Long, day: DayOfWeek, enabled: Boolean) {
		launchWrite {
			val diet = diets.value.find { it.id == dietId } ?: return@launchWrite
			val updated = diet.withDayNotificationToggled(day, enabled)
			dietRepository.updateDiet(updated)
			DietReminder.updateAll(application)
			AlarmSyncHelper.syncAlarms(application)
		}
	}

	fun setDeveloperMode(enabled: Boolean) {
		launchLocalWrite {
			configRepository.saveConfig(
				ConfigKey.DEVELOPER_MODE,
				enabled.toString(),
			)
			AlarmSyncHelper.syncAlarms(application)
		}
	}

	fun getDefaultFallback(type: MealType): Int {
		return when (type) {
			MealType.BREAKFAST -> 8 * 60
			MealType.MORNING_SNACK -> 10 * 60 + 30
			MealType.LUNCH -> 13 * 60
			MealType.AFTERNOON_SNACK -> 16 * 60 + 30
			MealType.DINNER -> 20 * 60
			MealType.OTHER -> 12 * 60
		}
	}

	fun getDefaultTime(type: MealType): Int {
		return defaultTimes.value
			.find { it.type == type }
			?.timeMinutes
			?: getDefaultFallback(type)
	}

	suspend fun getDietMeals(id: Long) = dietRepository.getMeals(id)

	suspend fun getDietIngredients(id: Long): List<IngredientSummary> {
		return dietRepository.getMeals(id).toIngredientSummaries()
	}

	suspend fun getMeal(id: Long): MealWithDetails? {
		if (id == 0L) {
			return null
		}

		return dietRepository
			.getMeals(active.value?.id ?: 0L)
			.find { it.meal.id == id }
	}

	fun next(): NextMeal? {
		return active.value?.let {
			nextMeal(
				meals.value,
				LocalDateTime.now(),
				it.nextMealWindowMinutes,
			)
		}
	}

	fun scheduleTestAlarm(seconds: Int) {
		AppLog.d("Scheduling test alarm in $seconds seconds...")

		AlarmScheduler.scheduleTestAlarm(
			context = application,
			delayMillis = seconds * 1000L
		)
	}

	fun saveDefaultTime(type: MealType, timeMinutes: Int) {
		launchLocalWrite {
			configRepository.saveDefaultTime(type, timeMinutes)
		}
	}

	fun resetDatabase() {
		launchWrite {
			configRepository.resetDatabase()
		}
	}

	fun create(name: String, window: Int) {
		launchWrite {
			dietRepository.create(name, window)
		}
	}

	fun activate(id: Long) {
		launchWrite {
			dietRepository.activate(id)
			DietReminder.updateAll(application)
		}
	}

	fun deleteDiet(id: Long) {
		launchWrite {
			dietRepository.delete(id)
			DietReminder.updateAll(application)
		}
	}

	fun duplicate(id: Long) {
		launchWrite {
			dietRepository.duplicate(id)
		}
	}

	fun saveMeal(meal: Meal, courses: List<CourseWithItems>) {
		launchWrite {
			dietRepository.saveMeal(meal, courses)
			DietReminder.updateAll(application)
		}
	}

	fun deleteMeal(id: Long) {
		launchWrite {
			dietRepository.deleteMeal(id)
			DietReminder.updateAll(application)
		}
	}

	fun parseDietJson(json: String): DietExport = dietRepository.parseDietJson(json)

	suspend fun dietExists(uuid: String): Boolean = dietRepository.dietExists(uuid)

	suspend fun checkImportConflict(json: String): ImportCheckResult {
		return try {
			lastImportError.value = null

			val data = dietRepository.parseDietJson(json)

			val existing = data.uuid?.let { dietRepository.dietExists(it) }

			if (existing == true) {
				ImportCheckResult.Conflict
			} else {
				ImportCheckResult.Valid
			}
		} catch (exception: Exception) {
			AppLog.e("Import validation failed", exception)

			val message = exception.localizedMessage ?: "Invalid import data"
			lastImportError.value = message

			ImportCheckResult.Invalid(message)
		}
	}

	suspend fun importDiet(
		json: String,
		overwrite: Boolean = false,
	): Result<Unit> {
		return try {
			lastImportError.value = null
			if (UpdateManagerStatus.writesBlocked) {
				val exception = OfflineWriteException()
				lastImportError.value = exception.message
				AppLog.w("Diet import blocked because the app is in offline read-only mode")
				_writeBlockedEvents.emit(Unit)
				return Result.failure(exception)
			}
			dietRepository.importJson(json, overwrite)
			Result.success(Unit)
		} catch (exception: OfflineWriteException) {
			lastImportError.value = exception.message
			_writeBlockedEvents.emit(Unit)
			Result.failure(exception)
		} catch (exception: Exception) {
			AppLog.e("Import failed", exception)

			val message = exception.localizedMessage ?: "Unknown error"
			lastImportError.value = message

			Result.failure(exception)
		}
	}

	suspend fun exportDiet(id: Long): String {
		return dietRepository.exportJson(id)
	}

	fun observeShoppingList(id: Long) = shoppingListRepository.observeShoppingList(id)

	fun createShoppingList(
		name: String,
		dietId: Long? = null,
		initialItems: List<ShoppingListInitialItem> = emptyList(),
		onCreated: ((Long) -> Unit)? = null,
	) {
		launchWrite {
			val id = shoppingListRepository.createShoppingList(name, dietId, initialItems)
			onCreated?.invoke(id)
		}
	}

	fun updateShoppingListName(id: Long, name: String) {
		launchWrite {
			shoppingListRepository.updateShoppingListName(id, name)
		}
	}

	fun deleteShoppingList(id: Long) {
		launchWrite {
			shoppingListRepository.deleteShoppingList(id)
		}
	}

	fun addShoppingListItem(
		listId: Long,
		name: String,
		amount: String = "",
		unit: QuantityUnit = QuantityUnit.GRAMS,
		isCustom: Boolean = true,
	) {
		launchWrite {
			shoppingListRepository.addShoppingListItem(listId, name, amount, unit, isCustom)
		}
	}

	fun updateShoppingListItem(item: ShoppingListItem) {
		launchWrite {
			shoppingListRepository.updateShoppingListItem(item)
		}
	}

	fun toggleShoppingListItem(item: ShoppingListItem) {
		launchWrite {
			shoppingListRepository.toggleShoppingListItemBought(item.id, !item.isBought)
		}
	}

	fun toggleShoppingListItemDay(dayId: Long, isBought: Boolean) {
		launchWrite {
			shoppingListRepository.toggleShoppingListItemDayBought(dayId, isBought)
		}
	}

	fun deleteShoppingListItem(itemId: Long) {
		launchWrite {
			shoppingListRepository.deleteShoppingListItem(itemId)
		}
	}

	fun importDietIngredientsToShoppingList(
		listId: Long,
		dietId: Long,
		configs: List<DietImportItemConfig>,
		onCompleted: (() -> Unit)? = null,
	) {
		launchWrite {
			shoppingListRepository.addDietIngredientsToShoppingList(listId, dietId, configs)
			onCompleted?.invoke()
		}
	}

	private fun defaultHydrationRanges(): List<HydrationRange> {
		return listOf(
			HydrationRange(
				startMinutes = 8 * 60,
				endMinutes = 22 * 60,
			),
		)
	}
}
