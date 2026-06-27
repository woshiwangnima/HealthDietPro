package com.woshiwangnima.healthdietpro.model.profile

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val gender: Gender = Gender.MALE,
    val birthday: AppDate? = null,
    val province: String = "",
    val diseaseIds: List<String> = emptyList(),
    val heightRecords: List<BodyRecord> = emptyList(),
    val weightRecords: List<BodyRecord> = emptyList(),
    val avatarFileName: String = ""
) {
    val latestHeight: BodyRecord? get() = heightRecords.maxByOrNull { it.date }
    val latestWeight: BodyRecord? get() = weightRecords.maxByOrNull { it.date }
    val age: Int? get() {
        val bd = birthday?.date ?: return null
        val today = java.time.LocalDate.now()
        val birth = try { java.time.LocalDate.parse(bd) } catch (_: Exception) { return null }
        var a = today.year - birth.year
        if (today.monthValue < birth.monthValue || (today.monthValue == birth.monthValue && today.dayOfMonth < birth.dayOfMonth)) a--
        return a.coerceAtLeast(0)
    }
}
