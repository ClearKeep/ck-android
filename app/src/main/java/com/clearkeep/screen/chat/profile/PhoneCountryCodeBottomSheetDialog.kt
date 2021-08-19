package com.clearkeep.screen.chat.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.base.CKSearchBox
import com.clearkeep.components.separatorDarkNonOpaque
import com.clearkeep.utilities.countryCodesToNames
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun PhoneCountryCodeBottomSheetDialog(onPick: (countryCode: String) -> Unit) {
    val context = LocalContext.current
    val searchQuery = remember { mutableStateOf("") }
    val searchResult = remember { mutableStateOf(countryCodesToNames) }

    Column(Modifier.padding(start = 12.dp, end = 23.dp)) {
        Spacer(Modifier.height(50.dp))
        Text(stringResource(R.string.country_codes), fontSize = 20.sp)
        Spacer(Modifier.height(25.dp))
        CKSearchBox(searchQuery)
        Spacer(Modifier.height(60.dp))
        LazyColumn {
            itemsIndexed(searchResult.value) { _: Int, item: Pair<Int?, String> ->
                val countryCode = if (item.first == null) "" else "+${item.first}"
                PhoneCountryCodeItem(
                    Modifier.padding(vertical = 16.dp),
                    item.second,
                    countryCode
                ) {
                    printlnCK("Pick country code $countryCode")
                    onPick.invoke(countryCode)
                }
                Divider(Modifier.height(1.dp), separatorDarkNonOpaque)
            }
        }
    }

    LaunchedEffect(searchQuery) {
        snapshotFlow { searchQuery.value }
            .distinctUntilChanged()
            .collect { query ->
                if (query.isBlank()) {
                    searchResult.value = countryCodesToNames
                }
                searchResult.value = countryCodesToNames.filter {
                    it.second.contains(query, true)
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
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(50.dp)
        )
    }
}
