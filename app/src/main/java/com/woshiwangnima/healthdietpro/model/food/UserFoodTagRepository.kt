package com.woshiwangnima.healthdietpro.model.food

import android.content.Context
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import kotlinx.serialization.Serializable

@Serializable
internal data class UserFoodTag(val id: String, val label: String)

internal class UserFoodTagRepository private constructor(
    private val context: Context,
    private val userId: String,
) {
    private val archiveStore = UserCustomFoodArchiveStore(context, userId)

    fun load(): List<UserFoodTag> = archiveStore.load().tags

    fun save(tags: List<UserFoodTag>) {
        archiveStore.saveTags(tags)
    }

    companion object {
        fun fromContext(context: Context) = UserFoodTagRepository(
            context = context.applicationContext,
            userId = ProfilePrefs.getCurrentUserId(context),
        )
    }
}
