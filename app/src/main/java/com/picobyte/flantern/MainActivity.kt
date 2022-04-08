package com.picobyte.flantern

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.picobyte.flantern.api.FlanternNotifications
import com.picobyte.flantern.authentication.AuthFirebase
import com.picobyte.flantern.authentication.AuthGoogle
import com.picobyte.flantern.databinding.ActivityMainBinding
import com.picobyte.flantern.db.GroupsViewModel
import com.picobyte.flantern.api.FlanternRequests
import com.picobyte.flantern.types.User

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    lateinit var authGoogle: AuthGoogle
    lateinit var authFirebase: AuthFirebase
    lateinit var rtDatabase: FirebaseDatabase
    lateinit var groupsViewModel: GroupsViewModel
    lateinit var storage: FirebaseStorage
    lateinit var adjectives: List<String>
    lateinit var animals: List<String>
    lateinit var requests: FlanternRequests
    val gson = Gson()
    val REQUEST_GALLERY_CODE = 300
    val userMap: HashMap<String, User> = HashMap<String, User>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_GALLERY_CODE
            )
        }
        authGoogle = AuthGoogle(this, Firebase.auth)
        authFirebase = AuthFirebase(this, Firebase.auth)
        rtDatabase = Firebase.database(getString(R.string.realtime_db_id))
        storage = Firebase.storage("gs://flantern-ea117.appspot.com")
        requests = FlanternRequests(this, rtDatabase, storage, Firebase.auth)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)

        authGoogle.signOut()
        authFirebase.signOut()
        val typeToken = object : TypeToken<List<String>>() {}.type
        adjectives = gson.fromJson(
            resources.openRawResource(R.raw.adjectives).bufferedReader().readText(),
            typeToken
        )
        animals = gson.fromJson(
            resources.openRawResource(R.raw.animals).bufferedReader().readText(),
            typeToken
        )
        //val myRef = rtDatabase.getReference("message")
        //myRef.setValue(Message("picobyte86","This is a test message. 19",0,"picobyte64",false))

        groupsViewModel = ViewModelProvider(this).get(GroupsViewModel::class.java)
        //authFirebase.createAccount("picobyte64@gmail.com", "amogus")
        //authGoogle.signIn()
        //Note: A Google account's email address can change, so don't use it to identify a user. Instead, use the account's ID, which you can get on the client with GoogleSignInAccount.getId, and on the backend from the sub claim of the ID token.
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                Toast.makeText(binding.root.context, "Settings Clicked!", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_GALLERY_CODE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
        }
    }
}
