package com.example.separatecontacts

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AddEditContactActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextPhone: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextNotes: EditText
    private lateinit var buttonSave: Button
    private lateinit var buttonDelete: Button
    private lateinit var firebaseSyncManager: FirebaseSyncManager
    
    private var contactId: String = ""
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_contact)

        initViews()
        firebaseSyncManager = FirebaseSyncManager()

        // Check if we're editing an existing contact
        contactId = intent.getStringExtra("contact_id") ?: ""
        isEditMode = contactId.isNotEmpty()

        if (isEditMode) {
            title = "Edit Contact"
            loadContactData()
            buttonDelete.visibility = android.view.View.VISIBLE
        } else {
            title = "Add Contact"
            buttonDelete.visibility = android.view.View.GONE
        }

        buttonSave.setOnClickListener {
            saveContact()
        }

        buttonDelete.setOnClickListener {
            deleteContact()
        }
    }

    private fun initViews() {
        editTextName = findViewById(R.id.editTextName)
        editTextPhone = findViewById(R.id.editTextPhone)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextNotes = findViewById(R.id.editTextNotes)
        buttonSave = findViewById(R.id.buttonSave)
        buttonDelete = findViewById(R.id.buttonDelete)
    }

    private fun loadContactData() {
        val name = intent.getStringExtra("contact_name") ?: ""
        val phone = intent.getStringExtra("contact_phone") ?: ""
        val email = intent.getStringExtra("contact_email") ?: ""
        val notes = intent.getStringExtra("contact_notes") ?: ""

        editTextName.setText(name)
        editTextPhone.setText(phone)
        editTextEmail.setText(email)
        editTextNotes.setText(notes)
    }

    private fun saveContact() {
        val name = editTextName.text.toString().trim()
        val phone = editTextPhone.text.toString().trim()
        val email = editTextEmail.text.toString().trim()
        val notes = editTextNotes.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Name and phone number are required", Toast.LENGTH_SHORT).show()
            return
        }

        val contact = Contact(
            id = if (isEditMode) contactId else "",
            name = name,
            phoneNumber = phone,
            email = email,
            notes = notes
        )

        lifecycleScope.launch {
            val success = if (isEditMode) {
                firebaseSyncManager.updateContact(contact)
            } else {
                firebaseSyncManager.addContact(contact) != null
            }

            if (success) {
                Toast.makeText(
                    this@AddEditContactActivity,
                    if (isEditMode) "Contact updated and synced" else "Contact saved and synced",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } else {
                Toast.makeText(
                    this@AddEditContactActivity,
                    "Error saving contact. Check your internet connection.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun deleteContact() {
        if (!isEditMode) return

        lifecycleScope.launch {
            val success = firebaseSyncManager.deleteContact(contactId)
            if (success) {
                Toast.makeText(this@AddEditContactActivity, "Contact deleted", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(
                    this@AddEditContactActivity,
                    "Error deleting contact. Check your internet connection.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

