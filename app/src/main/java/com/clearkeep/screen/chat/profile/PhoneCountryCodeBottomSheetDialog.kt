package com.clearkeep.screen.chat.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.components.base.CKSearchBox
import com.clearkeep.components.separatorDarkNonOpaque
import com.clearkeep.utilities.countryCodesToNames

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun PhoneCountryCodeBottomSheetDialog(onPick: (countryCode: String) -> Unit) {
    val context = LocalContext.current
    val searchQuery = remember { mutableStateOf("") }

    Column(Modifier.padding(start = 12.dp, end = 23.dp)) {
        Spacer(Modifier.height(50.dp))
        Text(stringResource(com.clearkeep.R.string.country_codes), fontSize = 20.sp)
        Spacer(Modifier.height(25.dp))
        CKSearchBox(searchQuery)
        Spacer(Modifier.height(60.dp))
        LazyColumn {
            itemsIndexed(countryCodesToNames) { _: Int, item: Pair<Int, String> ->
                PhoneCountryCodeItem(
                    Modifier.padding(vertical = 16.dp),
                    item.second,
                    "+${item.first}"
                ) {
                    onPick.invoke(item.first.toString())
                }
                Divider(Modifier.height(1.dp), separatorDarkNonOpaque)
            }
        }
    }
}

@Composable
fun PhoneCountryCodeItem(
    modifier: Modifier = Modifier,
    country: String,
    code: String,
    onSelect: () -> Unit
) {
    Box(
        modifier.then(
            Modifier
                .fillMaxWidth()
                .clickable {
                    onSelect()
                })
    ) {
        Text(
            country,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        Text(
            code,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.align(Alignment.CenterEnd).width(50.dp)
        )
    }
}
