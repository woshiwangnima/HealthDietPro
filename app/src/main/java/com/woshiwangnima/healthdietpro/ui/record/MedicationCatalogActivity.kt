package com.woshiwangnima.healthdietpro.ui.record

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.AppInputLabel
import com.woshiwangnima.healthdietpro.common.ui.AppInputTextFieldColors
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.model.medication.MedicationCatalogItem
import com.woshiwangnima.healthdietpro.model.medication.MedicationPrefs
import com.woshiwangnima.healthdietpro.model.unit.UnitCategory
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.util.UnitConverter
import java.io.File
import java.io.FileOutputStream

class MedicationCatalogActivity : BaseBackActivity() {
    companion object { const val EXTRA_CATALOG_ID = "catalog_id" }
    override fun getTitleText(): String = getString(R.string.medication_catalog_heading)

    private var editingId: String? = null
    private var state by mutableStateOf(CatalogFormState())
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            runCatching {
                contentResolver.openInputStream(it)?.use { input -> BitmapFactory.decodeStream(input) }
            }.getOrNull()?.let(::saveImage)
        }
    }
    private val categories: List<UnitCategory>
        get() = UnitConverter.getRepository()?.getCategories()
            ?.filter { it.id in setOf(UnitCategoryType.Weight.id, UnitCategoryType.Volume.id) }.orEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UnitConverter.init(this)
        editingId = intent.getStringExtra(EXTRA_CATALOG_ID)
        editingId?.let { id -> MedicationPrefs.getCatalog(this).find { it.id == id } }?.let { item ->
            state = CatalogFormState(item.name, item.specValue.takeIf { it > 0f }?.toString().orEmpty(), item.specUnitCategory, item.specUnitId, item.manufacturer, item.defaultMethod, item.imagePath, item.imagePath?.let(::loadImage), item.archived)
        }
        setContent { HealthDietProTheme { BaseScreen(getTitleText(), ::finish) { padding ->
            CatalogEditor(state, padding, categories, { state = it }, { galleryLauncher.launch("image/*") }, ::save)
        } } }
    }

    private fun saveImage(bitmap: Bitmap) {
        val dir = File(filesDir, "medication_catalog_images").apply { mkdirs() }
        val name = "medicine_${System.currentTimeMillis()}.jpg"
        FileOutputStream(File(dir, name)).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        state = state.copy(imagePath = "medication_catalog_images/$name", image = bitmap)
    }
    private fun loadImage(path: String) = File(filesDir, path).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
    private fun save() {
        if (state.name.trim().isEmpty()) { Toast.makeText(this, R.string.medication_catalog_name_required, Toast.LENGTH_SHORT).show(); return }
        MedicationPrefs.upsertCatalogItem(this, MedicationCatalogItem(editingId ?: "med_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}", state.name.trim(), state.specValue.toFloatOrNull() ?: 0f, state.categoryId, state.unitId, state.manufacturer.trim(), state.method.trim(), state.imagePath, state.archived))
        setResult(Activity.RESULT_OK); finish()
    }
}

private data class CatalogFormState(val name: String = "", val specValue: String = "", val categoryId: String = "", val unitId: String = "", val manufacturer: String = "", val method: String = "", val imagePath: String? = null, val image: Bitmap? = null, val archived: Boolean = false)

@androidx.compose.runtime.Composable
private fun CatalogEditor(state: CatalogFormState, padding: PaddingValues, categories: List<UnitCategory>, onChange: (CatalogFormState) -> Unit, onPickImage: () -> Unit, onSave: () -> Unit) {
    val category = categories.find { it.id == state.categoryId }
    val units = category?.units?.filter { !it.hidden }.orEmpty()
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { OutlinedTextField(state.name, { onChange(state.copy(name = it)) }, Modifier.fillMaxWidth(), label = { AppInputLabel(stringResource(R.string.medication_catalog_name)) }, colors = AppInputTextFieldColors()) }
        item { OutlinedTextField(state.specValue, { onChange(state.copy(specValue = it)) }, Modifier.fillMaxWidth(), label = { AppInputLabel(stringResource(R.string.medication_catalog_specification)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = AppInputTextFieldColors()) }
        item { AppDropdownField(stringResource(R.string.medication_record_spec_category_select), category?.displayName().orEmpty(), categories.map { AppDropdownOption(it.id, it.displayName()) }, { onChange(state.copy(categoryId = it.id, unitId = categories.find { c -> c.id == it.id }?.baseUnit.orEmpty())) }) }
        item { AppDropdownField(stringResource(R.string.medication_record_spec_unit_select), units.find { it.id == state.unitId }?.symbol().orEmpty(), units.map { AppDropdownOption(it.id, it.symbol()) }, { onChange(state.copy(unitId = it.id)) }, enabled = category != null) }
        item { OutlinedTextField(state.manufacturer, { onChange(state.copy(manufacturer = it)) }, Modifier.fillMaxWidth(), label = { AppInputLabel(stringResource(R.string.medication_catalog_manufacturer)) }, colors = AppInputTextFieldColors()) }
        item { AppDropdownField(stringResource(R.string.medication_catalog_default_method), state.method, stringArrayResource(R.array.medication_record_default_methods).map { AppDropdownOption(it, it) }, { onChange(state.copy(method = it.id)) }) }
        item { AppIconTextButton(stringResource(R.string.medication_catalog_pick_image), R.drawable.ic_photo, onPickImage) }
        state.image?.let { bitmap -> item { Image(bitmap.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = ContentScale.Fit) } }
        item { FilterChip(state.archived, { onChange(state.copy(archived = !state.archived)) }, label = { Text(stringResource(R.string.medication_catalog_archive)) }) }
        item { AppIconTextButton(stringResource(R.string.body_record_save), R.drawable.ic_save, onSave, Modifier.fillMaxWidth()) }
    }
}
