package com.woshiwangnima.healthdietpro.ui.test

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.model.container.CircleShape
import com.woshiwangnima.healthdietpro.model.container.CrossSection
import com.woshiwangnima.healthdietpro.model.container.CrossSectionProfile
import com.woshiwangnima.healthdietpro.model.container.IrregularShape
import com.woshiwangnima.healthdietpro.model.container.RectangleShape
import com.woshiwangnima.healthdietpro.model.container.SquareShape
import com.woshiwangnima.healthdietpro.model.container.toDto
import com.woshiwangnima.healthdietpro.ui.container.CrossSectionPreview
import com.woshiwangnima.healthdietpro.ui.container.CrossSectionProfileEditor
import com.woshiwangnima.healthdietpro.ui.container.CrossSectionShapeSelector
import com.woshiwangnima.healthdietpro.ui.container.PreviewAnnotation
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Dev-only UI test screen for the 2D cross-section shape system. */
@Composable
internal fun CrossSectionUiTestScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var profile by remember {
        mutableStateOf(
            CrossSectionProfile(
                points = listOf(
                    CrossSection(0.0, CircleShape(8.0)),
                    CrossSection(6.0, RectangleShape(18.0, 12.0)),
                    CrossSection(11.0, CircleShape(6.0)),
                ),
                totalHeightCm = 12.0,
            ),
        )
    }
    val json = remember { Json { prettyPrint = true; encodeDefaults = true } }
    val jsonText = remember(profile) { json.encodeToString(profile.toDto()) }

    BaseScreen(title = "截面形状系统测试", onBack = onBack, includeStatusBarPadding = false) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("静态形状预览", style = MaterialTheme.typography.titleMedium)
            Text("圆形：直径 / 半径；正方形：边长 / 对角线；长方形无对角线；异形为水渍状。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CrossSectionPreview(CircleShape(8.0), annotation = PreviewAnnotation.DIAMETER)
                CrossSectionPreview(CircleShape(8.0), annotation = PreviewAnnotation.RADIUS)
                CrossSectionPreview(SquareShape(6.0), annotation = PreviewAnnotation.SIDE)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CrossSectionPreview(SquareShape(6.0), annotation = PreviewAnnotation.DIAGONAL)
                CrossSectionPreview(RectangleShape(18.0, 12.0))
                CrossSectionPreview(IrregularShape(28.3, 18.5))
            }

            HorizontalDivider()

            Text("形状切换器", style = MaterialTheme.typography.titleMedium)
            CrossSectionShapeSelector(
                selected = profile.points.last().shape.kind,
                onSelected = {},
            )

            HorizontalDivider()

            CrossSectionProfileEditor(
                initial = profile,
                onProfileChanged = { profile = it },
            )

            HorizontalDivider()

            Text("序列化 JSON（实时）", style = MaterialTheme.typography.titleMedium)
            Text(
                text = jsonText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(8.dp),
            )
        }
    }
}
