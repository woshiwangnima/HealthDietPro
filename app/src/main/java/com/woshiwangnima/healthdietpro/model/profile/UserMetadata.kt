package com.woshiwangnima.healthdietpro.model.profile

import com.google.gson.annotations.SerializedName

/** Public, lightweight identity data used by the user switcher. */
data class UserMetadata(
    val id: String,
    val name: String,
    val gender: Gender,
    val avatarFileName: String,
    val createdAtMillis: Long = 0L,
    @SerializedName(value = "lastActiveAtMillis", alternate = ["lastSelectedAtMillis"])
    val lastActiveAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
)

internal fun UserProfile.toMetadata(
    createdAtMillis: Long = System.currentTimeMillis(),
    lastActiveAtMillis: Long = createdAtMillis,
    updatedAtMillis: Long = System.currentTimeMillis(),
): UserMetadata =
    UserMetadata(
        id = id,
        name = name,
        gender = gender,
        avatarFileName = avatarFileName,
        createdAtMillis = createdAtMillis,
        lastActiveAtMillis = lastActiveAtMillis,
        updatedAtMillis = updatedAtMillis,
    )
