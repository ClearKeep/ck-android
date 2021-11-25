package com.clearkeep.common.presentation.components.base

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.clearkeep.common.R
import com.clearkeep.common.presentation.components.grayscale4
import com.clearkeep.common.presentation.components.primaryDefault

@Composable
fun CKRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = RadioButtonDefaults.colors(
        selectedColor = primaryDefault,
        unselectedColor = grayscale4,
    )
    val radioColor by colors.radioColor(enabled, selected)
    Surface(
        shape = CircleShape,
        modifier = modifier
            .size(RadioButtonSize),
        color = radioColor
    ) {
        if (selected) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_done),
                    contentDescription = null,
                    tint = MaterialTheme.colors.onPrimary,
                )
            }
        }
    }
}

private val RadioButtonSize = 32.dp