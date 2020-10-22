package com.clearkeep.ui.widget

import androidx.compose.*
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.Measurable
import androidx.ui.core.Modifier
import androidx.ui.foundation.Text
import androidx.ui.foundation.TextField
import androidx.ui.foundation.TextFieldValue
import androidx.ui.foundation.currentTextStyle
import androidx.ui.graphics.Color
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.padding
import androidx.ui.material.Divider
import androidx.ui.material.FilledTextField
import androidx.ui.text.TextStyle
import androidx.ui.text.style.TextDecoration
import androidx.ui.unit.dp
import androidx.ui.unit.ipx

@Composable
fun FilledTextInputComponent(
    lable: String,
    placeholder: String,
    textValue: MutableState<String>
) {
    FilledTextField(
        value = textValue.value,
        onValueChange = { textValue.value = it },
        label = { Text(lable) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.padding(16.dp) + Modifier.fillMaxWidth(),
        activeColor = Color.Gray
    )
}

@Composable
fun HintEditText(
    hintText: String,
    modifier: Modifier,
    textStyle: TextStyle = currentTextStyle(), textValue: MutableState<TextFieldValue>
) {
    val inputField = @Composable {
        TextField(
            value = textValue.value,
            modifier = modifier,
            onValueChange = { textValue.value = it },
            textStyle = textStyle.merge(TextStyle(textDecoration = TextDecoration.None))
        )
    }

    Layout(
        children = @Composable {
            inputField()
            Text(
                text = hintText,
                modifier = modifier,
                style = textStyle.merge(TextStyle(color = Color.Gray))
            )
            Divider(color = Color.Black, thickness = 2.dp)
        },
        measureBlock = { measurables: List<Measurable>, constraints: Constraints, _ ->
            val inputFieldPlace = measurables[0].measure(constraints)
            val hintEditPlace = measurables[1].measure(constraints)
            val dividerEditPlace = measurables[2].measure(
                Constraints(constraints.minWidth, constraints.maxWidth, 2.ipx, 2.ipx)
            )
            layout(
                inputFieldPlace.width,
                inputFieldPlace.height + dividerEditPlace.height
            ) {
                inputFieldPlace.place(0.ipx, 0.ipx)
                if (textValue.value.text.isEmpty())
                    hintEditPlace.place(0.ipx, 0.ipx)
                dividerEditPlace.place(0.ipx, inputFieldPlace.height)
            }
        })
}