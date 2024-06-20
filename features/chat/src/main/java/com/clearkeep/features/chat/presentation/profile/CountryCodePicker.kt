package com.clearkeep.features.chat.presentation.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.*
import com.clearkeep.features.chat.R
import com.clearkeep.common.presentation.components.LocalColorMapping
import com.clearkeep.common.presentation.components.base.CKSearchBox
import com.clearkeep.common.presentation.components.base.CKText
import com.clearkeep.common.presentation.components.separatorDarkNonOpaque
import com.clearkeep.common.utilities.countryCodesToNames
import com.clearkeep.common.utilities.sdp
import com.clearkeep.common.utilities.toNonScalableTextSize
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

@ExperimentalMaterialApi
@Composable
fun CountryCodePicker(onPick: (countryCode: String) -> Unit, onCloseView: () -> Unit) {
    val context = LocalContext.current
    val searchQuery = rememberSaveable { mutableStateOf("") }
    val searchResult = remember { mutableStateOf(countryCodesToNames) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 12.sdp(), end = 23.sdp())
    ) {
        Spacer(Modifier.height(30.sdp()))
        Image(
            painter = painterResource(id = R.drawable.ic_cross),
            contentDescription = null, modifier = Modifier
                .clickable {
                    onCloseView.invoke()
                },
            alignment = Alignment.CenterStart,
            colorFilter = LocalColorMapping.current.closeIconFilter
        )
        Spacer(Modifier.height(50.sdp()))
        CKText(stringResource(R.string.country_codes), fontSize = 20.sdp().toNonScalableTextSize(), color = LocalColorMapping.current.headerText)
        Spacer(Modifier.height(25.sdp()))
        CKSearchBox(searchQuery)
        Spacer(Modifier.height(25.sdp()))
        LazyColumn {
            itemsIndexed(searchResult.value.sortedBy { it.second }) { _: Int, item: Pair<Int?, String> ->
                val countryCode = if (item.first == null) "" else "+${item.first}"
                PhoneCountryCodeItem(
                    Modifier.padding(vertical = 16.sdp()),
                    item.second,
                    countryCode
                ) {
                    onPick.invoke(countryCode)
                }
                Divider(Modifier.height(1.sdp()), separatorDarkNonOpaque)
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
    ConstraintLayout(
        modifier.then(
            Modifier
                .fillMaxWidth()
                .clickable {
                    onSelect()
                })
    ) {
        val (countryText, codeText) = createRefs()
        CKText(
            country,
            modifier = Modifier.constrainAs(countryText) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(codeText.start, 8.dp)
                width = Dimension.fillToConstraints
            },
            color = LocalColorMapping.current.bodyTextAlt
        )
        CKText(
            code,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier
                .width(50.sdp())
                .constrainAs(codeText) {
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                },
            color = LocalColorMapping.current.bodyTextAlt
        )
    }
}
