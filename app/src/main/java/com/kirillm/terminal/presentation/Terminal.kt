package com.kirillm.terminal.presentation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect.Companion.dashPathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kirillm.terminal.R
import com.kirillm.terminal.data.Bar
import java.util.*
import kotlin.math.roundToInt

const val MIN_VISIBLE_COUNT = 20

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Terminal(
    modifier: Modifier = Modifier,
) {

    val viewModel: MainViewModel = viewModel()
    val screenState = viewModel.screenState.collectAsState()
    when (val currentState = screenState.value) {
        is TerminalScreenState.Content -> {
            val terminalState = rememberTerminalState(currentState.barsList)

            Chart(
                modifier = modifier,
                terminalState = terminalState,
                timeFrame = currentState.timeFrame,
                onTerminalStateChanged = {
                    terminalState.value = it
                },
                onClickListener = {
                    viewModel.showBarInfo(it)
                }
            )


            currentState.barsList.firstOrNull()?.let {
                Prices(
                    modifier = Modifier.padding(top = 32.dp),
                    terminalState = terminalState,
                    currentPrice = it.close,
                )
            }

            TimeFrames(
                modifier = Modifier.padding(top = 32.dp),
                currentTimeFrame = currentState.timeFrame
            ) {
                viewModel.loadContent(currentState.currentTicker, it)
            }

            val barForInfo = currentState.barForInfo
            if (barForInfo != null) {
                BarInfo(barForInfo) {
                    viewModel.showBarInfo(null)
                }
            }

            SpinnerSample(
                modifier = Modifier.padding(top=8.dp),
                list = currentState.tickerList,
                preselected = currentState.currentTicker,
                onSelectionChanged = { ticker ->
                    viewModel.loadContent(ticker, currentState.timeFrame)
                }
            )
        }
        TerminalScreenState.Initial -> {}
        TerminalScreenState.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun BarInfo(
    bar: Bar,
    onCloseClickListener: () -> Unit,
) {
    Box(
        modifier = Modifier
            .wrapContentSize()
            .padding(top = 100.dp, start = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Color.Black)
                .border(BorderStroke(1.dp, Color.White), RoundedCornerShape(10.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val day = bar.calendarDate.get(Calendar.DAY_OF_MONTH)
            val month = bar.calendarDate.getDisplayName(
                Calendar.MONTH,
                Calendar.SHORT,
                Locale.getDefault()
            )
            val hour = bar.calendarDate.get(Calendar.HOUR_OF_DAY)
            Text(
                String.format("%d %s, %02d:00", day, month, hour),
                color = Color.White
            )
            Info(stringResource(R.string.open_label), bar.open)
            Info(stringResource(R.string.close_label), bar.close)
            Info(stringResource(R.string.high_label), bar.high)
            Info(stringResource(R.string.low_label), bar.low)
        }
    }
    Icon(
        imageVector = Icons.Default.Close,
        contentDescription = null,
        modifier = Modifier
            .padding(top = 90.dp, start = 6.dp)
            .size(20.dp)
            .background(Color.Black, CircleShape)
            .border(1.dp, color = Color.White, CircleShape)
            .clickable {
                onCloseClickListener()
            },
        tint = Color.White
    )
}

@Composable
fun Info(
    name: String,
    value: Float,
) {
    Row(
        modifier = Modifier
            .width(150.dp)
            .wrapContentHeight()
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = "$name:",
            color = Color.White
        )
        Text(
            modifier = Modifier.weight(1f),
            text = "$value",
            color = Color.White,
            textAlign = TextAlign.Start
        )
    }

}

@Composable
private fun TimeFrames(
    modifier: Modifier = Modifier,
    currentTimeFrame: TimeFrame,
    onTimeFrameChanged: (TimeFrame) -> Unit,
) {
    Row(
        modifier = modifier
            .wrapContentHeight()
            .padding(top = 16.dp, start = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TimeFrame.values().forEach { timeFrame ->
            val stringId = when (timeFrame) {
                TimeFrame.MIN_5 -> R.string.minute_5
                TimeFrame.MIN_15 -> R.string.minute_15
                TimeFrame.MIN_30 -> R.string.minute_30
                TimeFrame.HOUR_1 -> R.string.hour_1
            }
            AssistChip(
                onClick = { onTimeFrameChanged(timeFrame) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (currentTimeFrame == timeFrame) Color.White else Color.Black,
                    labelColor = if (currentTimeFrame == timeFrame) Color.Black else Color.White
                ),
                label = {
                    Text(text = stringResource(id = stringId))
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun Chart(
    modifier: Modifier,
    terminalState: State<TerminalState>,
    timeFrame: TimeFrame,
    onTerminalStateChanged: (TerminalState) -> Unit,
    onClickListener: (Bar) -> Unit,
) {
    val currentState = terminalState.value
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val visibleBarsCount =
            (currentState.visibleBarsCount / zoomChange).roundToInt()
                .coerceIn(MIN_VISIBLE_COUNT, currentState.barList.size)

        val scrolledBy = (currentState.scrolledBy + panChange.x).coerceIn(
            0f,
            currentState.barList.size * currentState.barWidth - currentState.terminalWidth
        )
//        Log.d("Terminal", "Scroll: $scrolledBy")

        onTerminalStateChanged(
            currentState.copy(
                visibleBarsCount = visibleBarsCount,
                scrolledBy = scrolledBy
            )
        )
    }

    val timeMeasurer = rememberTextMeasurer()

    val clickOffsetX = rememberSaveable {
        mutableStateOf(0f)
    }
    val wasClick = rememberSaveable {
        mutableStateOf(false)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(
                top = 64.dp,
                bottom = 32.dp
            )
            .transformable(transformableState)
            .onSizeChanged {
                onTerminalStateChanged(
                    currentState.copy(
                        terminalWidth = it.width.toFloat(),
                        terminalHeight = it.height.toFloat()
                    )
                )
            }
            .pointerInput(key1 = Unit) {
                detectTapGestures {
//                    Log.d("Terminal", "Click: $clickOffset Scroll: ${currentState.scrolledBy}")
                    clickOffsetX.value = it.x
                    wasClick.value = true
                }
            }

    ) {
        with(currentState) {
            translate(left = scrolledBy) {
                val clickOffset = clickOffsetX.value - scrolledBy
                barList.forEachIndexed { index, bar ->
                    val offsetX = size.width - index * barWidth
//                    Log.d("Terminal", "Offset: $offsetX")
                    drawTimeDelimiters(
                        bar = bar,
                        nextBar = if (index < barList.size - 1) {
                            barList[index + 1]
                        } else {
                            null
                        },
                        offsetX = offsetX,
                        timeFrame = timeFrame,
                        textMeasurer = timeMeasurer
                    )

                    drawLine(
                        color = Color.White,
                        start = Offset(
                            offsetX,
                            size.height - (bar.low - min) * pointsOnHeight
                        ),
                        end = Offset(
                            offsetX,
                            size.height - (bar.high - min) * pointsOnHeight
                        ),
                        strokeWidth = 2f
                    )

                    val color =
                        if (bar.open > bar.close) Color.Red else Color.Green
                    drawLine(
                        color = color,
                        start = Offset(
                            offsetX,
                            size.height - (bar.open - min) * pointsOnHeight
                        ),
                        end = Offset(
                            offsetX,
                            size.height - (bar.close - min) * pointsOnHeight
                        ),
                        strokeWidth = barWidth / 2
                    )

                    if (wasClick.value && clickOffset in ((offsetX - barWidth / 4)..(offsetX + barWidth / 4))) {
                        onClickListener(bar)
                        wasClick.value = false
                    }
                }
            }
        }

    }
}

@Composable
private fun Prices(
    modifier: Modifier,
    terminalState: State<TerminalState>,
    currentPrice: Float,
) {
    val currentState = terminalState.value
    val textMeasurer = rememberTextMeasurer()

    val max = currentState.max
    val min = currentState.min
    val pointsOnHeight = currentState.pointsOnHeight
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 32.dp)
    ) {
        drawLines(
            max = max,
            min = min,
            pointsOnHeight = pointsOnHeight,
            currentPrice = currentPrice,
            textMeasurer = textMeasurer
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun DrawScope.drawTimeDelimiters(
    bar: Bar,
    nextBar: Bar?,
    offsetX: Float,
    textMeasurer: TextMeasurer,
    timeFrame: TimeFrame,
) {
    val calendar = bar.calendarDate

    val minute = calendar.get(Calendar.MINUTE)
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val needDrawDelimiter = when (timeFrame) {
        TimeFrame.MIN_5 -> {
            minute == 0
        }
        TimeFrame.MIN_15 -> {
            minute == 0 && hour % 2 == 0
        }
        TimeFrame.MIN_30, TimeFrame.HOUR_1 -> {
            val nextBarDay = nextBar?.calendarDate?.get(Calendar.DAY_OF_MONTH)
            day != nextBarDay
        }
    }

    if (!needDrawDelimiter) return

    drawLine(
        color = Color.White.copy(alpha = 0.5f),
        start = Offset(offsetX, 0f),
        end = Offset(offsetX, size.height),
        strokeWidth = 1.dp.toPx(),
        pathEffect = dashPathEffect(
            listOf(4.dp.toPx(), 4.dp.toPx()).toFloatArray()
        )
    )

    val nameOfMonth =
        calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT_FORMAT, Locale.getDefault())
    val text = when (timeFrame) {
        TimeFrame.MIN_5, TimeFrame.MIN_15 -> {
            String.format("%02d:00", hour)
        }
        TimeFrame.MIN_30, TimeFrame.HOUR_1 -> {
            String.format("%d %s", day, nameOfMonth)
        }
    }

    val textLayoutResult = textMeasurer.measure(
        text = text,
        style = TextStyle(
            color = Color.White,
            fontSize = 12.sp
        )
    )
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(offsetX - textLayoutResult.size.width / 2, size.height)
    )
}


fun DrawScope.drawLines(
    max: Float,
    min: Float,
    currentPrice: Float,
    pointsOnHeight: Float,
    textMeasurer: TextMeasurer,
) {
    //max
    val maxPriceOffsetY = 0f
    drawDashLine(
        start = Offset(0f, maxPriceOffsetY),
        end = Offset(size.width, maxPriceOffsetY)
    )
    drawTextPrice(textMeasurer, max, maxPriceOffsetY)
//current
    val currentPriceOffsetY = size.height - (currentPrice - min) * pointsOnHeight
    drawDashLine(
        start = Offset(0f, currentPriceOffsetY),
        end = Offset(size.width, currentPriceOffsetY)
    )
    drawTextPrice(textMeasurer, currentPrice, currentPriceOffsetY)
    //min
    val minPriceOffsetY = size.height
    drawTextPrice(textMeasurer, min, minPriceOffsetY, true)

    drawDashLine(
        start = Offset(0f, minPriceOffsetY),
        end = Offset(size.width, minPriceOffsetY)
    )

}

fun DrawScope.drawTextPrice(
    textMeasurer: TextMeasurer,
    price: Float,
    offsetY: Float,
    isMin: Boolean = false
) {
    val textLayoutResult = textMeasurer.measure(
        text = price.toString(),
        style = TextStyle(
            color = Color.White,
            fontSize = 12.sp
        )
    )
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(size.width - textLayoutResult.size.width - 4.dp.toPx(), offsetY - if(isMin) textLayoutResult.size.height  else 0)
    )
}

fun DrawScope.drawDashLine(
    color: Color = Color.White,
    start: Offset,
    end: Offset,
    strokeWidth: Float = 1f,
) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth,
        pathEffect = dashPathEffect(
            listOf(4.dp.toPx(), 4.dp.toPx()).toFloatArray()
        )
    )
}

