package com.hunterrasmussen.maintenancetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hunterrasmussen.maintenancetracker.ui.caredit.CarEditScreen
import com.hunterrasmussen.maintenancetracker.ui.cardetail.CarDetailScreen
import com.hunterrasmussen.maintenancetracker.ui.carlist.CarListScreen
import com.hunterrasmussen.maintenancetracker.ui.recordedit.RecordEditScreen
import com.hunterrasmussen.maintenancetracker.ui.settings.SettingsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.CAR_LIST) {
        composable(Routes.CAR_LIST) {
            CarListScreen(
                onAddCar = { navController.navigate(Routes.carEdit(NEW_ITEM_ID)) },
                onOpenCar = { carId -> navController.navigate(Routes.carDetail(carId)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            route = Routes.CAR_EDIT,
            arguments = listOf(navArgument("carId") { type = NavType.LongType }),
        ) {
            CarEditScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.CAR_DETAIL,
            arguments = listOf(navArgument("carId") { type = NavType.LongType }),
        ) {
            CarDetailScreen(
                onBack = { navController.popBackStack() },
                onEditCar = { carId -> navController.navigate(Routes.carEdit(carId)) },
                onAddRecord = { carId -> navController.navigate(Routes.recordEdit(carId, NEW_ITEM_ID)) },
                onEditRecord = { carId, recordId -> navController.navigate(Routes.recordEdit(carId, recordId)) },
                onCarDeleted = {
                    navController.popBackStack(Routes.CAR_LIST, inclusive = false)
                },
            )
        }

        composable(
            route = Routes.RECORD_EDIT,
            arguments = listOf(
                navArgument("carId") { type = NavType.LongType },
                navArgument("recordId") { type = NavType.LongType },
            ),
        ) {
            RecordEditScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
