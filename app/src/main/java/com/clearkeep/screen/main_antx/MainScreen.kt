package com.clearkeep.screen.main_antx

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.clearkeep.R
import com.clearkeep.components.backgroundGradientEnd
import com.clearkeep.screen.main_antx.composes.CircleAvatarWorkSpace
import com.clearkeep.screen.main_antx.item.WorkSpaceEntity

@Composable
fun menuView(mainViewModel: MainViewModel) {
    val workSpaces = mainViewModel.listWorkSpace.observeAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.size(20.dp))
        Card(
            modifier = Modifier
                .weight(0.66f)
                ,shape = (
                    RoundedCornerShape(topEnd = 30.dp)
                )
        ) {
            Column {
                workSpaces.value?.let { item ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 30.dp,
                            end = 20.dp,
                            start = 20.dp,
                            bottom = 30.dp
                        ),
                    ) {
                        itemsIndexed(item) { _, workSpace ->
                            itemListWorkSpace(workSpace)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.size(2.dp))

        Column(
            modifier = Modifier
                .height(98.dp)
                .fillMaxWidth()
                .background(backgroundGradientEnd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier.background(
                    shape = CircleShape,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Red,
                            Color.Red
                        )
                    )
                ),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    null,
                    Modifier.size(50.dp),
                    alignment = Alignment.Center
                )
            }
        }

    }


}

@Composable
fun itemListWorkSpace(item: WorkSpaceEntity) {
    Box(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .border(1.dp, Color.Red)
            .size(56.dp)
    ) {
        CircleAvatarWorkSpace(url = item.urlIAvatar, name = item.name)
    }
}