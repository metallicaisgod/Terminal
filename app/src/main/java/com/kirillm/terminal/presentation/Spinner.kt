package com.kirillm.terminal.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kirillm.terminal.data.Ticker

@Composable
fun SpinnerSample(
    list: List<Ticker>,
    preselected: Ticker,
    onSelectionChanged: (ticker: Ticker) -> Unit,
    modifier: Modifier = Modifier,
) {

    var selected by remember { mutableStateOf(preselected) }
    var expanded by remember { mutableStateOf(false) } // initial value

    OutlinedCard(
        modifier = modifier.clickable {
            expanded = !expanded
        },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black,
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, Color.White)

    ) {


        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.wrapContentWidth()
        ) {

            Text(
                text = selected.toString(),
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Icon(Icons.Outlined.ArrowDropDown, null, modifier = Modifier.padding(8.dp))

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .wrapContentWidth()
                    .background(Color.Black)   // delete this modifier and use .wrapContentWidth() if you would like to wrap the dropdown menu around the content
            ) {
                list.forEach { listEntry ->

                    DropdownMenuItem(
                        onClick = {
                            selected = listEntry
                            expanded = false
                            onSelectionChanged(selected)
                        },
                        text = {
                            if (listEntry == selected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                                        .align(Alignment.Start),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = listEntry.toString(),
                                        modifier = Modifier
                                            .fillMaxSize(),  //optional instad of fillMaxWidth,
                                        color = Color.White
                                    )
                                }
                            } else {
                                Text(
                                    text = listEntry.toString(),
                                    modifier = Modifier
                                        .fillMaxSize()  //optional instad of fillMaxWidth
                                        .align(Alignment.Start),
                                    color = Color.White
                                )
                            }

                        }
                    )
                }
            }

        }
    }
}