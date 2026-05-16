package dam_a52057.wastewatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import dam_a52057.wastewatch.ui.addproduct.AddProductScreen
import dam_a52057.wastewatch.ui.home.HomeScreen
import dam_a52057.wastewatch.ui.inventory.InventoryScreen
import dam_a52057.wastewatch.ui.theme.WasteWatchTheme

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home      : Screen("home",      "Home",       Icons.Default.Home)
    object Inventory : Screen("inventory", "Inventário", Icons.Default.List)
    object Scanner   : Screen("scanner",   "Scanner",    Icons.Default.CameraAlt)
    object Recipes   : Screen("recipes",   "Receitas",   Icons.Default.Restaurant)
    object Shopping  : Screen("shopping",  "Compras",    Icons.Default.ShoppingCart)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Inventory,
    Screen.Scanner,
    Screen.Recipes,
    Screen.Shopping
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WasteWatchTheme {
                WasteWatchApp()
            }
        }
    }
}

@Composable
fun WasteWatchApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToInventory = { navController.navigate(Screen.Inventory.route) },
                    onNavigateToDetail = { itemId ->
                        navController.navigate("product_detail/$itemId")
                    }
                )
            }
            composable(Screen.Inventory.route) {
                InventoryScreen(
                    onNavigateToDetail = { itemId ->
                        navController.navigate("product_detail/$itemId")
                    },
                    onNavigateToAddProduct = {
                        navController.navigate("add_product")
                    }
                )
            }
            composable(Screen.Scanner.route) {
                Surface { Text("Scanner — Em breve") }
            }
            composable(Screen.Recipes.route) {
                Surface { Text("Receitas — Em breve") }
            }
            composable(Screen.Shopping.route) {
                Surface { Text("Lista de Compras — Em breve") }
            }
            composable(
                route = "product_detail/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.IntType })
            ) {
                Surface { Text("Detalhe do Produto — Em breve") }
            }
            composable(
                route = "add_product?barcode={barcode}",
                arguments = listOf(navArgument("barcode") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                AddProductScreen(
                    barcode = backStackEntry.arguments?.getString("barcode"),
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}