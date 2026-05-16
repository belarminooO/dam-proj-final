package dam_a52057.wastewatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import dam_a52057.wastewatch.ui.scanner.ScannerScreen
import dam_a52057.wastewatch.ui.theme.WasteWatchTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WasteWatchTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val bottomNavRoutes = listOf("home", "inventory", "scanner", "recipes", "shopping")
                val showBottomBar = bottomNavRoutes.any { currentRoute == it }

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
                                    icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                                    label = { Text("Inventario") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "scanner",
                                    onClick = { navController.navigate("scanner") { launchSingleTop = true } },
                                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                                    label = { Text("Scanner") }
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
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            HomeScreen(
                                onNavigateToInventory = { navController.navigate("inventory") },
                                onNavigateToItem = { itemId -> navController.navigate("product_detail/$itemId") }
                            )
                        }
                        composable("inventory") {
                            InventoryScreen(
                                onNavigateToAddProduct = { navController.navigate("add_product") },
                                onNavigateToDetail = { itemId -> navController.navigate("product_detail/$itemId") }
                            )
                        }
                        composable("scanner") {
                            ScannerScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToAddProduct = { barcode, name, brand ->
                                    val params = mutableListOf<String>()
                                    if (barcode != null) params.add("barcode=$barcode")
                                    if (name != null) params.add("name=$name")
                                    if (brand != null) params.add("brand=$brand")
                                    val route = if (params.isEmpty()) "add_product"
                                    else "add_product?${params.joinToString("&")}"
                                    navController.navigate(route)
                                }
                            )
                        }
                        composable("recipes") {
                            Box(Modifier.fillMaxSize().padding(16.dp)) { Text("Receitas — Em breve") }
                        }
                        composable("shopping") {
                            Box(Modifier.fillMaxSize().padding(16.dp)) { Text("Lista de Compras — Em breve") }
                        }
                        composable(
                            route = "product_detail/{itemId}",
                            arguments = listOf(navArgument("itemId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val itemId = backStackEntry.arguments?.getInt("itemId") ?: return@composable
                            Box(Modifier.fillMaxSize().padding(16.dp)) { Text("Detalhe $itemId — Em breve") }
                        }
                        composable(
                            route = "add_product?barcode={barcode}&name={name}&brand={brand}",
                            arguments = listOf(
                                navArgument("barcode") { type = NavType.StringType; nullable = true; defaultValue = null },
                                navArgument("name") { type = NavType.StringType; nullable = true; defaultValue = null },
                                navArgument("brand") { type = NavType.StringType; nullable = true; defaultValue = null }
                            )
                        ) { backStackEntry ->
                            AddProductScreen(
                                barcode = backStackEntry.arguments?.getString("barcode"),
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
