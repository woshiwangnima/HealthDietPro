package com.woshiwangnima.healthdietpro.model.archive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 附件引用换算的护栏。
 *
 * 起因是一次真实的导入失败：用药记录里的照片沿用老式键
 * （`medication_photos/xxx.jpg`），而校验按 `"/attachments/"` 切分，
 * 切不到就判成「附件缺失」，整份档案导入被拦下——
 * 明明图片就在包内，只是换了目录。
 *
 * 这里把换算规则钉死：新式键照旧，老式键映射到档案内的副本，
 * 认不出来的引用返回 null（表示「这条引用不指向档案附件」），由调用方决定怎么处理。
 */
class ArchiveAttachmentPathTest {

    @Test
    fun newStylePathKeepsRelativeForm() {
        assertEquals(
            "attachments/medications/records/med_1.jpg",
            archiveAttachmentPathOf("user_archives/1784392406758/attachments/medications/records/med_1.jpg"),
        )
    }

    @Test
    fun legacyCatalogKeyMapsToArchiveCopy() {
        assertEquals(
            "attachments/medications/catalog/medicine_1.jpg",
            archiveAttachmentPathOf("medication_catalog_images/medicine_1.jpg"),
        )
    }

    @Test
    fun legacyPhotoKeyMapsToArchiveCopy() {
        assertEquals(
            "attachments/medications/records/med_1.jpg",
            archiveAttachmentPathOf("medication_photos/med_1.jpg"),
        )
    }

    @Test
    fun customFoodImageKeyMapsToArchiveCopy() {
        assertEquals(
            "attachments/foods/abc.jpg",
            archiveAttachmentPathOf("user:user_archives/1784392406758/attachments/foods/abc.jpg"),
        )
    }

    @Test
    fun unrelatedReferenceHasNoArchivePath() {
        assertNull(archiveAttachmentPathOf("https://example.com/photo.jpg"))
        assertNull(archiveAttachmentPathOf(""))
    }

    @Test
    fun legacyKeyWithoutDirectoryIsNotTreatedAsArchivePath() {
        // 只有一个裸文件名：既不是老式目录，也不是档案路径。
        assertNull(archiveAttachmentPathOf("med_1.jpg"))
    }
}
