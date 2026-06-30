package com.woshiwangnima.healthdietpro.model.region

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionRepositoryTest {

    @Test
    fun loadsAnhuiProvinceData() {
        val repo = RegionRepository.fromAsset("src/main/assets/regions.json")
        val cities = repo.citiesOf("34")
        assertEquals(16, cities.size)
        assertEquals("合肥市", cities.first().name)
    }

    @Test
    fun hefeiHasDistricts() {
        val repo = RegionRepository.fromAsset("src/main/assets/regions.json")
        val districts = repo.districtsOf("3401")
        assertTrue(districts.isNotEmpty())
        assertEquals("瑶海区", districts.first().name)
    }

    @Test
    fun resolveHefeiReturnsAnhuiThenHefeiThenCentreDistrict() {
        val regionRepo = RegionRepository.fromAsset("src/main/assets/regions.json")
        val provinceRepo = ProvinceRepository.fromAsset("src/main/assets/provinces.json")
        // 合肥市中心坐标
        val snap = regionRepo.resolve(117.227, 31.821, provinceRepo)
        assertEquals("34", snap.provinceCode)
        assertEquals("安徽省", snap.provinceName)
        assertEquals("3401", snap.cityCode)
        assertEquals("合肥市", snap.cityName)
        // 县级在质心点上返回的应该是距中心点最近的一个，但一定会命中
        assertNotNull(snap.districtCode)
        assertTrue(snap.districtCode.isNotEmpty())
        println("合肥定位 → ${snap.display()}")
    }
}