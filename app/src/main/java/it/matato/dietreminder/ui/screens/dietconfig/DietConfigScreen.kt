package it.matato.dietreminder.ui.screens.dietconfig

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.DietEntity
import it.matato.dietreminder.data.MealWithDetails
import it.matato.dietreminder.ui.components.MealCard
import it.matato.dietreminder.ui.viewmodel.DietViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietConfigScreen(
    vm: DietViewModel,
    dietId: Long,
    onBack: () -> Unit,
    onAddMeal: (Long, DayOfWeek) -> Unit,
    onEditMeal: (Long, Long) -> Unit
) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    var diet by remember { mutableStateOf<DietEntity?>(null) }
    val meals by vm.meals.collectAsState()
    
    LaunchedEffect(dietId) {
        diet = vm.diets.value.find { it.id == dietId }
    }

    var day by rememberSaveable { mutableStateOf(DayOfWeek.MONDAY) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(diet?.name ?: stringResource(R.string.configure_diet)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddMeal(dietId, day) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Add, stringResource(R.string.add))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(
                selectedTabIndex = day.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
                divider = {}
            ) {
                DayOfWeek.entries.forEach { currentDay ->
                    Tab(
                        selected = currentDay == day,
                        onClick = { day = currentDay },
                        text = {
                            Text(
                                text = currentDay.getDisplayName(TextStyle.SHORT, locale),
                                fontWeight = if (currentDay == day) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            AnimatedContent(
                targetState = day,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "day_change"
            ) { selectedDay ->
                var dietMeals by remember { mutableStateOf<List<MealWithDetails>>(emptyList()) }
                LaunchedEffect(dietId, selectedDay, meals) {
                    val allMeals = if (vm.active.value?.id == dietId) meals else vm.getDietMeals(dietId)
                    dietMeals = allMeals.filter { it.meal.dayOfWeek == selectedDay }.sortedBy { it.meal.timeMinutes }
                }

                if (dietMeals.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.no_meals_for_day),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(dietMeals, key = { it.meal.id }) { mealDetails ->
                            MealCard(
                                meal = mealDetails.meal,
                                onClick = { onEditMeal(mealDetails.meal.id, dietId) }
                            )
                        }
                    }
                }
            }
        }
    }
}
