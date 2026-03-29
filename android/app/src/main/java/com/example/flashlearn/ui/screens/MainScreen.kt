package com.example.flashlearn.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flashlearn.R

sealed class BottomNavItem(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    object Learn : BottomNavItem("learn", R.string.nav_learn, Icons.Default.PlayArrow)
    object MyDecks : BottomNavItem("my_decks", R.string.nav_my_decks, Icons.AutoMirrored.Filled.List)
    object Create : BottomNavItem("create", R.string.nav_create, Icons.Default.Add)
    object Explore : BottomNavItem("explore", R.string.nav_explore, Icons.Default.Search)
    object Profile : BottomNavItem("profile", R.string.nav_profile, Icons.Default.Person)
}

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onNavigateToCreateDeck: () -> Unit = {},
    onNavigateToFlashcards: (deckId: Long) -> Unit = {}
) {
    val items = listOf(
        BottomNavItem.Learn,
        BottomNavItem.MyDecks,
        BottomNavItem.Create,
        BottomNavItem.Explore,
        BottomNavItem.Profile
    )

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedItem = items[selectedIndex]

    Scaffold(
        bottomBar = {
            Column {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
            ) {
                items.forEach { item ->
                    val isSelected = selectedItem == item
                    val title = stringResource(item.titleRes)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { selectedIndex = items.indexOf(item) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = title,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedItem) {
                BottomNavItem.Learn -> LearnScreen()
                BottomNavItem.MyDecks -> DeckListScreen(
                    onNavigateToCreateDeck = onNavigateToCreateDeck,
                    onNavigateToFlashcards = onNavigateToFlashcards
                )
                BottomNavItem.Create -> CreateScreen()
                BottomNavItem.Explore -> MarketplaceScreen()
                BottomNavItem.Profile -> DashboardScreen(onLogout = onLogout)
            }
        }
    }
}
