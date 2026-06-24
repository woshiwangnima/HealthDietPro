package com.woshiwangnima.healthdietpro.model.profile

data class UserProfile(
    val name: String = "",
    val gender: Gender = Gender.MALE,
    val birthday: AppDate? = null,
    val province: String = "",
    val diseaseIds: List<String> = emptyList(),
    val heightRecords: List<BodyRecord> = emptyList(),
    val weightRecords: List<BodyRecord> = emptyList()
) {
    val latestHeight: BodyRecord? get() = heightRecords.maxByOrNull { it.date }
    val latestWeight: BodyRecord? get() = weightRecords.maxByOrNull { it.date }
}
