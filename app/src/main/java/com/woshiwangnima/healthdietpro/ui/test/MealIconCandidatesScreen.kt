package com.woshiwangnima.healthdietpro.ui.test

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BreakfastDining
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RiceBowl
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen

private data class VectorCandidate(val label: String, val icon: ImageVector)

private data class DrawableCandidate(@param:DrawableRes val iconRes: Int, val label: String)

@Composable
internal fun MealIconCandidatesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vectorCandidates = listOf(
        VectorCandidate("BreakfastDining - breakfast plate", Icons.Filled.BreakfastDining),
        VectorCandidate("LunchDining - burger", Icons.Filled.LunchDining),
        VectorCandidate("DinnerDining - dinner place setting", Icons.Filled.DinnerDining),
        VectorCandidate("RiceBowl - bowl of rice", Icons.Filled.RiceBowl),
        VectorCandidate("RamenDining - bowl with noodles", Icons.Filled.RamenDining),
        VectorCandidate("SetMeal - meal set", Icons.Filled.SetMeal),
        VectorCandidate("Restaurant - dining", Icons.Filled.Restaurant),
        VectorCandidate("Fastfood - fast food", Icons.Filled.Fastfood),
    )
    val drawableCandidates = listOf(
        DrawableCandidate(R.drawable.ic_diet, "Existing: ic_diet"),
        DrawableCandidate(R.drawable.ic_food_dish, "Existing: ic_food_dish"),
        DrawableCandidate(R.drawable.ic_nav_nutrition, "Existing: ic_nav_nutrition"),
        DrawableCandidate(R.drawable.ic_food_ingredient, "Existing: ic_food_ingredient"),
    )

    BaseScreen(title = "Meal icon candidates", onBack = onBack, includeStatusBarPadding = false) { padding ->
        Column(
            modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Material dining icons", style = MaterialTheme.typography.titleLarge)
            vectorCandidates.forEach { VectorCandidateRow(it) }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Existing app drawables", style = MaterialTheme.typography.titleLarge)
            drawableCandidates.forEach { DrawableCandidateRow(it) }
        }
    }
}

@Composable
private fun VectorCandidateRow(candidate: VectorCandidate) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(imageVector = candidate.icon, contentDescription = candidate.label, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurface)
        Text(candidate.label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun DrawableCandidateRow(candidate: DrawableCandidate) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(painter = painterResource(candidate.iconRes), contentDescription = candidate.label, modifier = Modifier.size(48.dp))
        Text(candidate.label, style = MaterialTheme.typography.bodyLarge)
    }
}
