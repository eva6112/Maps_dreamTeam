package com.example.tsumaps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import com.example.tsumaps.ant_algorithm.RouteScreen
import com.example.tsumaps.navigation.AppDestinations
import com.example.tsumaps.food.FoodScreen
import com.example.tsumaps.ui.theme.TSUMapsTheme
import com.example.tsumaps.map.MapScreen
import androidx.compose.material3.MaterialTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TSUMapsTheme {
                TSUMapsApp()
            }
        }
    }
}

@Composable
fun TSUMapsApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.MAP) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            painter = painterResource(id = destination.icon),
                            contentDescription = destination.label,
                            tint = MaterialTheme.colorScheme.primary

                        )
                    },
                    label = { Text(destination.label, color = MaterialTheme.colorScheme.primary) },
                    selected = destination == currentDestination,
                    onClick = { currentDestination = destination }
                )
            }
        }
        ) {
        when (currentDestination) {
            AppDestinations.MAP -> MapScreen()
            AppDestinations.FOOD -> FoodScreen()
            AppDestinations.TOUR -> RouteScreen()
        }
    }
}