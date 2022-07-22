package com.clearkeep.components.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.components.LocalColorMapping
import com.clearkeep.components.grayscale2
import com.clearkeep.components.grayscale3
import com.clearkeep.components.primaryDefault
import com.clearkeep.utilities.sdp
import com.clearkeep.utilities.toNonScalableTextSize

@Composable
fun CKSetting(
    modifier: Modifier,
    name: String,
    description: String = "",
    checked: Boolean,
    onCheckChange: (Boolean) -> Unit
) {
    Column(modifier) {
        Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
            CKHeaderText(
                name,
                modifier = Modifier.weight(0.66f),
                color = LocalColorMapping.current.inputLabel
            )
            Column(
                modifier = Modifier.clickable { },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = primaryDefault, checkedTrackColor = primaryDefault,
                        uncheckedThumbColor = grayscale3, uncheckedTrackColor = grayscale3
                    ),
                    modifier = Modifier
                        .width(64.sdp())
                        .height(36.sdp())
                )
            }
        }
        if (description.isNotBlank()) {
            Text(
                text = description, style = MaterialTheme.typography.body1.copy(
                    color = LocalColorMapping.current.descriptionTextAlt,
                    fontSize = 14.sdp().toNonScalableTextSize(),
                    fontWeight = FontWeight.Normal
                ), modifier = Modifier.fillMaxWidth(0.8f)
            )
        }
    }
}