package com.example.separatecontacts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var firebaseSyncManager: FirebaseSyncManager

    private val PERMISSION_REQUEST_CODE = 100
    private val OVERLAY_PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupRecyclerView()
        initializeFirebase()
        requestPermissions()
        observeContacts()

        fabAdd.setOnClickListener {
            val intent = Intent(this, AddEditContactActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sync_status -> {
                showSyncStatus()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewContacts)
        fabAdd = findViewById(R.id.fabAdd)
    }

    private fun setupRecyclerView() {
        contactAdapter = ContactAdapter(emptyList()) { contact ->
            editContact(contact)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = contactAdapter
    }

    private fun initializeFirebase() {
        firebaseSyncManager = FirebaseSyncManager()
    }

    private fun observeContacts() {
        lifecycleScope.launch {
            firebaseSyncManager.getContactsFlow().collect { contacts ->
                contactAdapter.updateContacts(contacts)
            }
        }
    }

    private fun showSyncStatus() {
        val status = if (firebaseSyncManager.isConnected()) {
            "Connected to cloud sync (User ID: ${firebaseSyncManager.getUserId()?.take(8)}...)"
        } else {
            "Not connected to cloud sync"
        }
        Toast.makeText(this, status, Toast.LENGTH_LONG).show()
    }

    private fun editContact(contact: Contact) {
        val intent = Intent(this, AddEditContactActivity::class.java).apply {
            putExtra("contact_id", contact.id)
            putExtra("contact_name", contact.name)
            putExtra("contact_phone", contact.phoneNumber)
            putExtra("contact_email", contact.email)
            putExtra("contact_notes", contact.notes)
        }
        startActivity(intent)
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            checkOverlayPermission()
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "Please grant overlay permission for caller ID to work",
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                checkOverlayPermission()
            } else {
                Toast.makeText(
                    this,
                    "Permissions are required for caller ID functionality",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Overlay permission denied. Caller ID won't work properly.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

