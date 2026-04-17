package com.example.tsumaps


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.tsumaps.navigation.AppDestinations
import com.example.tsumaps.food.FoodScreen
import com.example.tsumaps.ui.theme.TSUMapsTheme

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
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.FOOD) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            painter = painterResource(id = destination.icon),
                            contentDescription = destination.label
                        )
                    },
                    label = { Text(destination.label) },
                    selected = destination == currentDestination,
                    onClick = { currentDestination = destination }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.MAP -> Text(
                    text = "Маршруты (А*) - будет позже",
                    modifier = Modifier.padding(innerPadding)
                )
                AppDestinations.FOOD -> FoodScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.TOUR -> Text(
                    text = "Экскурсия (муравьиный) - будет позже",
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}