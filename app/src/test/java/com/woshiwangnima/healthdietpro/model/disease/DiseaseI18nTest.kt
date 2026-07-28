package com.woshiwangnima.healthdietpro.model.disease

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class DiseaseI18nTest {

    private val repository = DiseaseRepository.fromAsset("src/main/assets/diseases.json")

    @Test fun displayNameUsesRequestedLanguageLabel() {
        val disease = repository.findById("hypertension")!!

        assertEquals("高血压", disease.displayName(Locale.SIMPLIFIED_CHINESE))
        assertEquals("Hypertension", disease.displayName(Locale.ENGLISH))
    }

    @Test fun searchMatchesStableIdIcd11CodeAndLocalizedAlias() {
        assertEquals("type2_diabetes", repository.search("type2_diabetes").single().id)
        assertEquals("type2_diabetes", repository.findByIcd11Code("5A11").single().id)
        assertEquals("fatty_liver", repository.search("脂肪肝", Locale.SIMPLIFIED_CHINESE).single().id)
        assertEquals("pcos", repository.search("PCOS", Locale.ENGLISH).single().id)
    }

    @Test fun classificationAndDepartmentQueriesUseCatalogReferences() {
        assertTrue(repository.getByCategory("endocrine_metabolic").any { it.id == "type2_diabetes" })
        assertTrue(repository.getByCourse(DiseaseCourse.EPISODIC).any { it.id == "migraine" })
        assertTrue(repository.getByDepartment("cardiology").any { it.id == "hypertension" })
        assertEquals("内分泌与代谢", repository.findCategoryById("endocrine_metabolic")!!.displayName(Locale.SIMPLIFIED_CHINESE))
        assertEquals("Endocrinology", repository.findDepartmentById("endocrinology")!!.displayName(Locale.ENGLISH))
    }

    @Test fun sexApplicabilityUsesAnatomicalTraitsInsteadOfGenderIdentity() {
        val pcos = repository.findById("pcos")!!

        assertTrue(pcos.applicability.allows(setOf(AnatomicalTrait.OVARIES)))
        assertFalse(pcos.applicability.allows(setOf(AnatomicalTrait.TESTES)))
    }

    @Test fun catalogUsesGlobalDepartmentReferences() {
        val diabetes = repository.findById("type2_diabetes")!!

        assertEquals(listOf("endocrinology", "general_medicine", "nutrition"), diabetes.careDepartmentIds)
        assertEquals(ClinicalKind.RISK_STATE, repository.findById("dyslipidemia")!!.clinicalKind)
        assertEquals(ClinicalKind.LABORATORY_ABNORMALITY, repository.findById("hyperuricemia")!!.clinicalKind)
    }
}
