package com.woshiwangnima.healthdietpro.model.region

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProvinceRepositoryTest {

    @Test
    fun loadsAll34ProvincesFromBundledJson() {
        val repo = ProvinceRepository.fromAsset("src/main/assets/location/provinces.json")
        assertEquals(34, repo.all().size)
    }

    @Test
    fun beijingHasCode11AndCorrectName() {
        val repo = ProvinceRepository.fromAsset("src/main/assets/location/provinces.json")
        val bj = repo.findByCode("11")
        assertNotNull(bj)
        assertEquals("北京市", bj!!.name)
    }

    @Test
    fun beijingTiananmenFallsIntoBeijing() {
        val repo = ProvinceRepository.fromAsset("src/main/assets/location/provinces.json")
        val p = repo.findByPoint(116.397, 39.916)
        assertNotNull("天安门应落在某个省份", p)
        assertEquals("北京市", p?.name)
    }

    @Test
    fun guangzhouFallsIntoGuangdong() {
        val repo = ProvinceRepository.fromAsset("src/main/assets/location/provinces.json")
        val p = repo.findByPoint(113.264, 23.129)
        assertNotNull(p)
        assertEquals("广东省", p?.name)
        assertEquals("44", p?.code)
    }
}
