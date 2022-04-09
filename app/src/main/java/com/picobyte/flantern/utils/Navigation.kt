package com.picobyte.flantern.utils

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.picobyte.flantern.R

fun navigateTo(root: View, destination: Int) {
    ((root.context as AppCompatActivity).supportFragmentManager.findFragmentById(
        R.id.nav_host_fragment_content_main
    ) as NavHostFragment).navController.navigate(destination)
}
fun navigateWithBundle(root: View, destination: Int, bundle: Bundle) {
    ((root.context as AppCompatActivity).supportFragmentManager.findFragmentById(
        R.id.nav_host_fragment_content_main
    ) as NavHostFragment).navController.navigate(destination, bundle)
}
fun navigateUp(root: View) {
    ((root.context as AppCompatActivity).supportFragmentManager.findFragmentById(
        R.id.nav_host_fragment_content_main
    ) as NavHostFragment).navController.navigateUp()
}