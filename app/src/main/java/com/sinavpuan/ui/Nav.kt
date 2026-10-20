package com.sinavpuan.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "classes") {
        composable("classes") { ClassListScreen(nav) }
        composable("class/{classId}", listOf(navArgument("classId") { type = NavType.LongType })) {
            ClassDetailScreen(nav, it.arguments!!.getLong("classId"))
        }
        composable("roster/{classId}", listOf(navArgument("classId") { type = NavType.LongType })) {
            RosterScanScreen(nav, it.arguments!!.getLong("classId"))
        }
        composable(
            "exam/{studentId}/{examNo}",
            listOf(
                navArgument("studentId") { type = NavType.LongType },
                navArgument("examNo") { type = NavType.IntType },
            ),
        ) {
            ExamScanScreen(nav, it.arguments!!.getLong("studentId"), it.arguments!!.getInt("examNo"))
        }
        composable("parents/{classId}", listOf(navArgument("classId") { type = NavType.LongType })) {
            ParentsScreen(nav, it.arguments!!.getLong("classId"))
        }
    }
}
