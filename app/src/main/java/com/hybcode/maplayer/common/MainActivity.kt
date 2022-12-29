package com.hybcode.maplayer.common

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.findNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.hybcode.maplayer.R
import com.hybcode.maplayer.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private val navController by lazy { findNavController(R.id.nav_host_fragment) }
    private val appBarConfiguration by lazy { AppBarConfiguration(topLevelDestinationIds = setOf(
        R.id.NavMusicLibrary, R.id.NavSongs, R.id.NavPlaybackControls)) }

    private var isNavHostFragmentVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideFragment()
        setupActionBar()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (isNavHostFragmentVisible){
            hideFragment()
            showNavButton()
        }
        else super.onBackPressed()
    }

    private fun setupActionBar() {
        setSupportActionBar(binding.toolbar)
        //setupActionBarWithNavController(navController, appBarConfiguration)
    }

    private fun hideFragment() {
        val fragmentManager = supportFragmentManager
        val fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment)
        fragment?.let {
            fragmentManager.beginTransaction()
                .hide(it)
                .commit()
        }
        isNavHostFragmentVisible = false
    }

    private fun showFragment(){
        hideNavButton()
        val fragmentManager = supportFragmentManager
        val fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment)
        fragment?.let {
            fragmentManager.beginTransaction()
                .show(it)
                .commit()
        }
        isNavHostFragmentVisible = true
    }

    private fun hideNavButton() {
        binding.musicLibraryButton.visibility = View.GONE
        binding.playListButton.visibility = View.GONE
        binding.albumLibraryButton.visibility = View.GONE
        binding.artistLibraryButton.visibility = View.GONE
    }

    private fun showNavButton() {
        binding.musicLibraryButton.visibility = View.VISIBLE
        binding.playListButton.visibility = View.VISIBLE
        binding.albumLibraryButton.visibility = View.VISIBLE
        binding.artistLibraryButton.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
        } else ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            1)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
        } else {
            toast(getString(R.string.permission_required))
            finish()
        }
    }

    fun buttonPressed(view: View) {
        when(view){
            binding.musicLibraryButton -> showFragment()
            binding.playListButton -> showFragment()
            binding.artistLibraryButton -> showFragment()
            binding.albumLibraryButton -> showFragment()
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}