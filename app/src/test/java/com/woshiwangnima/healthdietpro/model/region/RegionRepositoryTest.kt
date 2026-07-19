package com.woshiwangnima.healthdietpro.model.region

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionRepositoryTest {

    private val provinces = ProvinceRepository.fromAsset("src/main/assets/location/provinces.json")
    private val regions = RegionRepository.fromAsset("src/main/assets/location/regions.json")

    @Test
    fun bundledLocationDataCoversNationalCityAndDistrictIndexes() {
        assertEquals(34, provinces.all().size)
        assertTrue(regions.citiesOf("44").isNotEmpty())
        assertTrue(regions.districtsOf("4401").isNotEmpty())
        assertTrue(regions.citiesOf("34").isNotEmpty())
        assertTrue(regions.districtsOf("3401").isNotEmpty())
    }

    @Test
    fun resolvesKnownCoordinatesToProvinceCityAndDistrict() {
        val beijing = regions.resolve(116.397, 39.916, provinces)
        assertEquals("11", beijing.provinceCode)
        assertEquals("北京市", beijing.cityName)
        assertTrue(beijing.districtName.isNotBlank())

        val guangzhou = regions.resolve(113.324, 23.135, provinces)
        assertEquals("44", guangzhou.provinceCode)
        assertEquals("广州市", guangzhou.cityName)
        assertTrue(guangzhou.districtName.isNotBlank())

        val hefei = regions.resolve(117.315, 31.859, provinces)
        assertEquals("34", hefei.provinceCode)
        assertEquals("合肥市", hefei.cityName)
        assertEquals("瑶海区", hefei.districtName)
    }
}
