package com.example.separatecontacts

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class FirebaseSyncManager {
    
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private var contactsRef: DatabaseReference? = null
    
    init {
        // Sign in anonymously for instant access without user intervention
        signInAnonymously()
    }
    
    private fun signInAnonymously() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    initializeDatabase()
                }
            }
        } else {
            initializeDatabase()
        }
    }
    
    private fun initializeDatabase() {
        val userId = auth.currentUser?.uid ?: return
        contactsRef = database.getReference("users").child(userId).child("contacts")
    }
    
    suspend fun addContact(contact: Contact): String? {
        return try {
            val ref = contactsRef ?: return null
            val contactId = ref.push().key ?: return null
            val contactWithId = contact.copy(id = contactId)
            ref.child(contactId).setValue(contactWithId).await()
            contactId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun updateContact(contact: Contact): Boolean {
        return try {
            val ref = contactsRef ?: return false
            ref.child(contact.id).setValue(contact).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun deleteContact(contactId: String): Boolean {
        return try {
            val ref = contactsRef ?: return false
            ref.child(contactId).removeValue().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun getContactsFlow(): Flow<List<Contact>> = callbackFlow {
        val ref = contactsRef
        if (ref == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val contacts = mutableListOf<Contact>()
                for (contactSnapshot in snapshot.children) {
                    val contact = contactSnapshot.getValue(Contact::class.java)
                    contact?.let { contacts.add(it) }
                }
                trySend(contacts.sortedBy { it.name })
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        ref.addValueEventListener(listener)
        
        awaitClose {
            ref.removeEventListener(listener)
        }
    }
    
    suspend fun getContactByPhoneNumber(phoneNumber: String): Contact? {
        return try {
            val ref = contactsRef ?: return null
            val snapshot = ref.orderByChild("phoneNumber").equalTo(phoneNumber).get().await()
            snapshot.children.firstOrNull()?.getValue(Contact::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun isConnected(): Boolean {
        return auth.currentUser != null && contactsRef != null
    }
    
    fun getUserId(): String? {
        return auth.currentUser?.uid
    }
}

