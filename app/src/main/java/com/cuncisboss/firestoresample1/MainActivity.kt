package com.cuncisboss.firestoresample1

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val personCollectionPref = Firebase.firestore.collection("persons")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnUploadData.setOnClickListener {
            val person = getOldPerson()
            savePerson(person)
        }

        btnRetrieveData.setOnClickListener {
            retrievePerson()
        }

        btnDeletePerson.setOnClickListener {
            val person = getOldPerson()
            deletePerson(person)
        }

        btnUpdatePerson.setOnClickListener {
            val oldPerson = getOldPerson()
            val newPerson = getNewPersonMap()
            updatePerson(oldPerson, newPerson)
        }

        btnBatchedWrite.setOnClickListener {
            changeName("OSgr1JWudeKP9NN6tLvE", "Viga", "Cha")
        }
    }

    private fun getOldPerson(): Person {
        val firstName = etFirstName.text.toString()
        val lastName = etLastName.text.toString()
        val age = etAge.text.toString().toInt()
        return Person(firstName, lastName, age)
    }

    private fun getNewPersonMap(): Map<String, Any> {
        val firstName = etNewFirstName.text.toString()
        val lastName = etNewLastName.text.toString()
        val age = etNewAge.text.toString()
        val map = mutableMapOf<String, Any>()
        if (firstName.isNotEmpty()) {
            map["firstName"] = firstName
        }
        if (lastName.isNotEmpty()) {
            map["lastName"] = lastName
        }
        if (age.isNotEmpty()) {
            map["age"] = age.toInt()
        }
        return map
    }

    private fun changeName(
        personId: String,
        newFirstName: String,
        newLastName: String
    ) = CoroutineScope(Dispatchers.IO).launch {
        try {
            Firebase.firestore.runBatch { writeBatch ->
                val personRef = personCollectionPref.document(personId)
                writeBatch.update(personRef, "firstName", newFirstName)
                writeBatch.update(personRef, "lastName", newLastName)
                writeBatch.update(personRef, mapOf(
                    "address" to FieldValue.delete()
                ))
            }.await()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deletePerson(person: Person) = CoroutineScope(Dispatchers.IO).launch {
        val personQuery = personCollectionPref
            .whereEqualTo("firstName", person.firstName)
            .whereEqualTo("lastName", person.lastName)
            .whereEqualTo("age", person.age.toInt())
            .get()
            .await()

        if (personQuery.documents.isNotEmpty()) {
            for (document in personQuery) {
                try {
                    personCollectionPref.document(document.id).delete().await()
//                    personCollectionPref.document(document.id).update(mapOf(
//                        "firstName" to FieldValue.delete()
//                    ))
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "No person matched the query", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePerson(person: Person, newPersonMap: Map<String, Any>) = CoroutineScope(Dispatchers.IO).launch {
        val personQuery = personCollectionPref
            .whereEqualTo("firstName", person.firstName)
            .whereEqualTo("lastName", person.lastName)
            .whereEqualTo("age", person.age.toInt())
            .get()
            .await()

        if (personQuery.documents.isNotEmpty()) {
            for (document in personQuery) {
                try {
                    personCollectionPref.document(document.id).set(
                        newPersonMap,
                        SetOptions.merge()
                    )
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "No person matched the query", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun subscribeToRealtimeDatabase() {
        personCollectionPref
//            .whereEqualTo("firstName", "peter")
            .addSnapshotListener { value, error ->
            value?.let {
                val sb = StringBuilder()
                for (document in it) {
                    val person = document.toObject<Person>()
                    sb.append("$person\n")
                }

                tvPersons.text = sb.toString()
            }
            error?.let {
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
        }
    }

    private fun retrievePerson() = CoroutineScope(Dispatchers.IO).launch {
        val fromAge = etFrom.text.toString().toInt()
        val toAge = etTo.text.toString().toInt()
        try {
            val querySnapshot = personCollectionPref
                .whereGreaterThan("age", fromAge)
                .whereLessThanOrEqualTo("age", toAge)
                .orderBy("age")
                .get()
                .await()
            val sb = StringBuilder()
            for (document in querySnapshot) {
                val person = document.toObject<Person>()
                sb.append("$person\n")
            }
            Log.d("_logFirebase", "retrievePerson: $sb")
            withContext(Dispatchers.Main) {
                tvPersons.text = sb.toString()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun savePerson(person: Person) = CoroutineScope(Dispatchers.IO).launch {
        try {
            personCollectionPref.add(person).await()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Successfully added data", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}














