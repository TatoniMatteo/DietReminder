package it.matato.dietreminder.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.Diet
import it.matato.dietreminder.data.database.entity.ShoppingList
import it.matato.dietreminder.data.database.entity.ShoppingListItem
import it.matato.dietreminder.data.database.relation.ShoppingListItemWithDays
import it.matato.dietreminder.data.database.relation.ShoppingListWithItems
import it.matato.dietreminder.data.model.HydrationRange
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.ui.screens.hydration.HydrationContent
import it.matato.dietreminder.ui.screens.dietconfig.DietConfigContent
import it.matato.dietreminder.ui.screens.diets.DietsActionsMenu
import it.matato.dietreminder.ui.screens.diets.DietsContent
import it.matato.dietreminder.ui.screens.diets.DietsHeader
import it.matato.dietreminder.ui.screens.hydration.HydrationRangeItem
import it.matato.dietreminder.ui.screens.hydration.HydrationSettingsSection
import it.matato.dietreminder.ui.screens.mealdetail.MealHeader
import it.matato.dietreminder.ui.screens.mealdetail.MealInformationSection
import it.matato.dietreminder.ui.screens.mealdetail.MealNotesSection
import it.matato.dietreminder.ui.screens.mealdetail.CoursesList
import it.matato.dietreminder.ui.screens.settings.SettingsListItem
import it.matato.dietreminder.ui.screens.settings.SettingsAppearanceSection
import it.matato.dietreminder.ui.screens.settings.SettingsAboutSection
import it.matato.dietreminder.ui.screens.settings.SettingsNotificationsSection
import it.matato.dietreminder.ui.AppContent
import it.matato.dietreminder.ui.screens.shoppinglist.ShoppingListDetailContent
import it.matato.dietreminder.ui.screens.shoppinglist.ShoppingListsContent
import it.matato.dietreminder.ui.theme.DietTheme
import java.time.DayOfWeek
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineWriteControlsFunctionalTest {

	@get:Rule
	val composeTestRule = createComposeRule()

	private val context: Context = ApplicationProvider.getApplicationContext()

	private fun showOffline(content: @Composable () -> Unit) {
		composeTestRule.setContent {
			DietTheme {
				CompositionLocalProvider(LocalOfflineMode provides true) {
					content()
				}
			}
		}
	}

	@Test
	fun dietCreateButtonIsDisabled() {
		showOffline {
			DietsContent(emptyList(), {}, {}, {}, {}, {}, {})
		}
		composeTestRule.onNodeWithContentDescription(context.getString(R.string.new_diet)).assertIsNotEnabled()
	}

	@Test
	fun dietImportButtonIsDisabled() {
		showOffline { DietsHeader(count = 0, onImportClick = {}) }
		composeTestRule.onNodeWithContentDescription(context.getString(R.string.import_diet)).assertIsNotEnabled()
	}

	@Test
	fun dietActivateMenuActionIsDisabled() {
		showOffline { DietsActionsMenu(true, false, {}, {}, {}, {}, {}) }
		composeTestRule.onNodeWithText(context.getString(R.string.activate)).assertIsNotEnabled()
	}

	@Test
	fun dietDuplicateMenuActionIsDisabled() {
		showOffline { DietsActionsMenu(true, false, {}, {}, {}, {}, {}) }
		composeTestRule.onNodeWithText(context.getString(R.string.duplicate)).assertIsNotEnabled()
	}

	@Test
	fun dietDeleteMenuActionIsDisabled() {
		showOffline { DietsActionsMenu(true, false, {}, {}, {}, {}, {}) }
		composeTestRule.onNodeWithText(context.getString(R.string.delete)).assertIsNotEnabled()
	}

	@Test
	fun mealCreateButtonIsDisabled() {
		showOffline {
			DietConfigContent(
				dietName = "Dieta",
				selectedDay = DayOfWeek.MONDAY,
				dietMeals = emptyList(),
				isDayNotificationEnabled = true,
				isDayNotificationSwitchEnabled = true,
				onToggleDayNotification = {},
				onBack = {},
				onIngredientsClick = {},
				onDaySelected = {},
				onAddMeal = {},
				onEditMeal = {},
			)
		}
		composeTestRule.onNodeWithContentDescription(context.getString(R.string.add)).assertIsNotEnabled()
	}

	@Test
	fun dietNotificationSwitchIsDisabled() {
		showOffline {
			DietConfigContent(
				"Dieta", DayOfWeek.MONDAY, emptyList(), true, true, {}, {}, {}, {}, {}, {},
			)
		}
		composeTestRule.onNodeWithTag("diet-day-notifications-switch").assertIsNotEnabled()
	}

	@Test
	fun mealSaveButtonIsDisabled() {
		showOffline { MealHeader(true, MealType.LUNCH, 720, {}, {}, {}) }
		composeTestRule.onNodeWithText(context.getString(R.string.save)).assertIsNotEnabled()
	}

	@Test
	fun mealDeleteButtonIsDisabled() {
		showOffline { MealHeader(false, MealType.LUNCH, 720, {}, {}, {}) }
		composeTestRule.onNodeWithContentDescription(context.getString(R.string.delete)).assertIsNotEnabled()
	}

	@Test
	fun mealTimeButtonIsDisabled() {
		showOffline {
			MealInformationSection(720, {}, MealType.LUNCH, {}, true, {})
		}
		composeTestRule.onNodeWithText("12:00").assertIsNotEnabled()
	}

	@Test
	fun mealTypeSelectorIsDisabled() {
		showOffline {
			MealInformationSection(720, {}, MealType.LUNCH, {}, true, {})
		}
		composeTestRule.onNodeWithText(context.getString(R.string.meal_type)).assertIsNotEnabled()
	}

	@Test
	fun mealNotificationSwitchIsDisabled() {
		showOffline {
			MealInformationSection(720, {}, MealType.LUNCH, {}, true, {})
		}
		composeTestRule.onNodeWithTag("meal-notifications-switch").assertIsNotEnabled()
	}

	@Test
	fun mealNotesFieldIsDisabled() {
		showOffline { MealNotesSection("Note", {}) }
		composeTestRule.onNodeWithText("Note").assertIsNotEnabled()
	}

	@Test
	fun addCourseButtonIsDisabled() {
		showOffline { CoursesList(emptyList(), onUpdate = { _, _ -> }, onDelete = {}, onAddCourse = {}) }
		composeTestRule.onNodeWithText(context.getString(R.string.add_course)).assertIsNotEnabled()
	}

	@Test
	fun hydrationReminderSwitchIsEnabled() {
		showOffline { HydrationSettingsSection(true, {}) }
		composeTestRule.onNodeWithTag("hydration-reminders-switch").assertIsEnabled()
	}

	@Test
	fun hydrationAddRangeButtonIsEnabled() {
		showOffline {
			HydrationContent(
				hydrationEnabled = true,
				hydrationInterval = 60,
				hydrationRanges = emptyList(),
				activeDays = emptySet(),
				onEnabledChange = {},
				onIntervalChange = {},
				onDaysChange = {},
				onDeleteRange = {},
				onAddRangeClick = {},
			)
		}
		composeTestRule.onNodeWithContentDescription(context.getString(R.string.add)).assertIsEnabled()
	}

	@Test
	fun hydrationRangeDeleteMenuButtonIsEnabled() {
		showOffline { HydrationRangeItem(HydrationRange(540, 720), {}) }
		composeTestRule.onNodeWithContentDescription(context.getString(R.string.delete)).assertIsEnabled()
	}

	@Test
	fun shoppingListCreateButtonIsDisabled() {
		showOffline {
			ShoppingListsContent(emptyList(), onSelectList = {}, onIngredientsClick = {}, onCreateClick = {}, onDeleteList = {}, onRenameList = { _, _ -> })
		}
		composeTestRule.onNodeWithContentDescription(context.getString(R.string.new_shopping_list)).assertIsNotEnabled()
	}

	@Test
	fun shoppingListItemAddButtonIsDisabled() {
		showOffline {
			ShoppingListDetailContent(
				listWithItems = ShoppingListWithItems(ShoppingList(id = 1, name = "Lista"), emptyList()),
				hasActiveDiet = false,
				onBack = {},
				onToggleItem = {},
				onToggleDay = { _, _ -> },
				onDeleteItem = {},
				onUpdateItem = {},
				onAddCustomClick = {},
				onImportDietClick = {},
				onDeleteList = {},
				onRenameList = {},
			)
		}
		composeTestRule.onNodeWithContentDescription(context.getString(R.string.add_item)).assertIsNotEnabled()
	}

	@Test
	fun purchasedShoppingItemCheckboxIsDisabled() {
		val item = ShoppingListItem(id = 1, shoppingListId = 1, name = "Mela", isBought = false)
		showOffline {
			ShoppingListDetailContent(
				ShoppingListWithItems(ShoppingList(id = 1, name = "Lista"), listOf(ShoppingListItemWithDays(item, emptyList()))),
				false, {}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {},
			)
		}
		composeTestRule.onNodeWithText("Mela").assertExists()
		composeTestRule.onNodeWithTag("shopping-item-checkbox").assertIsNotEnabled()
	}

	@Test
	fun settingsWriteRowIsDisabled() {
		showOffline { SettingsListItem(title = "Modifica impostazione", enabled = false, onClick = {}) }
		composeTestRule.onNodeWithText("Modifica impostazione").assertIsNotEnabled()
	}

	@Test
	fun mealReminderSettingSwitchIsEnabled() {
		showOffline { SettingsNotificationsSection(enabled = true, onEnabledChange = {}) }
		composeTestRule.onNodeWithText(context.getString(R.string.meal_reminders)).assertIsEnabled()
	}

	@Test
	fun appearanceSettingsRemainEnabledOffline() {
		showOffline {
			SettingsAppearanceSection(
				currentTheme = "system",
				dynamicEnabled = true,
				seedColorHex = "0xFF6750A4",
				currentLanguage = "en",
				onThemeChange = {},
				onDynamicColorsChange = {},
				onColorClick = {},
				onLanguageClick = {},
			)
		}
		composeTestRule.onNodeWithText(context.getString(R.string.theme)).assertIsEnabled()
	}

	@Test
	fun versionRowForDeveloperModeActivationIsEnabledOffline() {
		showOffline { SettingsAboutSection(onVersionClick = {}) }
		composeTestRule.onNodeWithText(context.getString(R.string.version)).assertIsEnabled()
	}

	@Test
	fun offlineBannerIsVisibleAndPositionedBelowSystemStatusBar() {
		composeTestRule.setContent {
			DietTheme {
				AppContent(
					currentRoute = null,
					showBottomBar = false,
					isOffline = true,
					onNavigateToRoot = {},
					content = {},
				)
			}
		}

		val banner = composeTestRule.onNodeWithText(context.getString(R.string.offline_mode_banner))
		banner.assertExists()
		assertTrue(banner.fetchSemanticsNode().boundsInRoot.top > 0f)
	}

	@Test
	fun dietRowConfigurationStillRemainsReadableOffline() {
		showOffline {
			DietsContent(
				diets = listOf(Diet(id = 1, name = "Dieta", nextMealWindowMinutes = 60)),
				onCreateClick = {},
				onImportClick = {},
				onActivate = {},
				onDuplicate = {},
				onDelete = {},
				onConfigure = {},
			)
		}
		composeTestRule.onNodeWithText("Dieta").assertExists()
	}
}
