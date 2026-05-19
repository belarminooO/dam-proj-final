package dam_a52057.wastewatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import dam_a52057.wastewatch.data.repository.AuthRepository
import dam_a52057.wastewatch.notifications.ExpiryNotificationWorker
import dam_a52057.wastewatch.ui.addproduct.AddProductScreen
import dam_a52057.wastewatch.ui.auth.LoginScreen
import dam_a52057.wastewatch.ui.auth.RegisterScreen
import dam_a52057.wastewatch.ui.auth.SocialHubScreen
import dam_a52057.wastewatch.ui.home.HomeScreen
import dam_a52057.wastewatch.ui.inventory.InventoryScreen
import dam_a52057.wastewatch.ui.inventory.ProductDetailScreen
import dam_a52057.wastewatch.ui.recipes.RecipesScreen
import dam_a52057.wastewatch.ui.scanner.ScannerScreen
import dam_a52057.wastewatch.ui.shopping.ShoppingListScreen
import dam_a52057.wastewatch.ui.mealplanner.MealPlannerScreen
import dam_a52057.wastewatch.ui.theme.WasteWatchTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Iniciar notificações em background
        ExpiryNotificationWorker.schedule(this)

        setContent {
            WasteWatchTheme {
                val navController = rememberNavController()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route
                
                val bottomNavRoutes = listOf("home", "inventory", "meal_plan", "recipes", "shopping")
                val showBottomBar = bottomNavRoutes.any { currentRoute == it }

                val startDestination = if (authRepository.currentUser != null) "home" else "login"

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentRoute == "home",
                                    onClick = { navController.navigate("home") { launchSingleTop = true } },
                                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                    label = { Text("Home") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "inventory",
                                    onClick = { navController.navigate("inventory") { launchSingleTop = true } },
                                    icon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
                                    label = { Text("Inventario") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "meal_plan",
                                    onClick = { navController.navigate("meal_plan") { launchSingleTop = true } },
                                    icon = { Icon(Icons.Default.EventNote, contentDescription = null) },
                                    label = { Text("Plano") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "recipes",
                                    onClick = { navController.navigate("recipes") { launchSingleTop = true } },
                                    icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                                    label = { Text("Receitas") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "shopping",
                                    onClick = { navController.navigate("shopping") { launchSingleTop = true } },
                                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                                    label = { Text("Compras") }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = { navController.navigate("register") }
                            )
                        }
                        composable("register") {
                            RegisterScreen(
                                onRegisterSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("register") { inclusive = true }
                                    }
                                },
                                onNavigateToLogin = { navController.navigate("login") }
                            )
                        }
                        composable("home") {
                            HomeScreen(
                                onNavigateToInventory = { navController.navigate("inventory") },
                                onNavigateToSocial = { navController.navigate("social_hub") },
                                onNavigateToItem = { id -> navController.navigate("product_detail/$id") }
                            )
                        }
                        composable("social_hub") {
                            SocialHubScreen(
                                onLogout = {
                                    navController.navigate("login") {
                                        popUpTo(0)
                                    }
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("inventory") {
                            InventoryScreen(
                                onNavigateToDetail = { id -> navController.navigate("product_detail/$id") },
                                onNavigateToAddProduct = { navController.navigate("scanner") }
                            )
                        }
                        composable("meal_plan") {
                            MealPlannerScreen()
                        }
                        composable("recipes") {
                            RecipesScreen()
                        }
                        composable("shopping") {
                            ShoppingListScreen()
                        }
                        composable("scanner") {
                            ScannerScreen(
                                onBarcodeDetected = { barcode ->
                                    navController.navigate("add_product?barcode=$barcode") {
                                        popUpTo("scanner") { inclusive = true }
                                    }
                                },
                                onNavigateToManualAdd = {
                                    navController.navigate("add_product") {
                                        popUpTo("scanner") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(
                            route = "product_detail/{itemId}",
                            arguments = listOf(navArgument("itemId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val itemId = backStackEntry.arguments?.getInt("itemId") ?: return@composable
                            ProductDetailScreen(
                                itemId = itemId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "add_product?barcode={barcode}",
                            arguments = listOf(navArgument("barcode") { 
                                type = NavType.StringType
                                nullable = true
                            })
                        ) { backStackEntry ->
                            val barcode = backStackEntry.arguments?.getString("barcode")
                            AddProductScreen(
                                barcode = barcode,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
