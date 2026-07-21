package com.woshiwangnima.healthdietpro.model.profile

import com.woshiwangnima.healthdietpro.model.archive.ArchiveSchemaVersion

data class UserProfile(
    val id: String = "",
    val archiveSchemaVersion: ArchiveSchemaVersion? = null,
    val archiveAppVersion: String = "",
    val name: String = "",
    val gender: Gender = Gender.MALE,
    val birthday: AppDate? = null,
    /**
     * 完整地区信息：经纬度 + 省/市/县三级代码与名称。
     * 旧版本曾用 `province: String` 存 2 位省代码；迁移逻辑放在
     * [com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs.load]，
     * 应用层统一通过本字段读写，不再单独存 province。
     */
    val region: com.woshiwangnima.healthdietpro.model.region.RegionSnapshot = com.woshiwangnima.healthdietpro.model.region.RegionSnapshot(),
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
