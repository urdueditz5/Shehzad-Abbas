package com.example.core.firebase

import android.content.Context
import com.example.R
import com.example.core.preferences.UserPreferencesRepository
import com.example.data.repositories.AiRepository
import com.example.data.repositories.ChatRepository
import com.example.data.repositories.ListingsRepository
import com.example.data.repositories.SocialRepository
import com.example.data.repositories.ToolsRepository
import com.example.data.repositories.UserRepository
import com.example.data.repositories.VideoRepository
import com.google.firebase.firestore.FirebaseFirestore

class ApnaDependencies(context: Context) {
    private val appContext = context.applicationContext
    val databaseId: String = appContext.getString(R.string.firestore_database_id)
    val db: FirebaseFirestore = FirebaseFirestore.getInstance(databaseId)

    val preferencesRepository = UserPreferencesRepository(appContext)
    val userRepository = UserRepository(db)
    val socialRepository = SocialRepository(db)
    val videoRepository = VideoRepository(db)
    val chatRepository = ChatRepository(db)
    val listingsRepository = ListingsRepository(db)
    val toolsRepository = ToolsRepository(db)
    val aiRepository = AiRepository()
}
