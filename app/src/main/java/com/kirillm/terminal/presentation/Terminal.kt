package com.kirillm.terminal.presentation

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
                    modifier = modifier,
                    terminalState = terminalState,
                    currentPrice = it.close,
                )
            }

            TimeFrames(currentTimeFrame = currentState.timeFrame) {
                viewModel.loadContent(it)
            }

            val barForInfo = currentState.barForInfo
            if (barForInfo != null) {

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
                        verticalArrangement = Arrangement.Center
                    ) {
                        val index = currentState.barsList.indexOf(barForInfo)
                        Text(
                            text = index.toString(),
                            color = Color.White
                        )
                        Text(
                            text = barForInfo.open.toString(),
                            color = Color.White
                        )
                        Text(
                            barForInfo.close.toString(),
                            color = Color.White
                        )
                        Text(
                            barForInfo.high.toString(),
                            color = Color.White
                        )
                        Text(
                            barForInfo.low.toString(),
                            color = Color.White
                        )
                        val day = barForInfo.calendarDate.get(Calendar.DAY_OF_MONTH)
                        val month = barForInfo.calendarDate.getDisplayName(
                            Calendar.MONTH,
                            Calendar.SHORT,
                            Locale.getDefault()
                        )
                        val hour = barForInfo.calendarDate.get(Calendar.HOUR_OF_DAY)
                        Text(
                            String.format("%d %s, %02d:00", day, month, hour),
                            color = Color.White
                        )
                    }
                }
            }
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
private fun TimeFrames(
    currentTimeFrame: TimeFrame,
    onTimeFrameChanged: (TimeFrame) -> Unit,
) {
    Row(
        modifier = Modifier
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

        onTerminalStateChanged(
            currentState.copy(
                visibleBarsCount = visibleBarsCount,
                scrolledBy = scrolledBy
            )
        )
    }

    val timeMeasurer = rememberTextMeasurer()

    val clickOffsetX = rememberSaveable() {
        mutableStateOf(0f)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(
                vertical = 32.dp
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
                    clickOffsetX.value = it.x
                }
            }

    ) {
        with(currentState) {
            translate(left = scrolledBy) {
                barList.forEachIndexed { index, bar ->
                    val offsetX = size.width - index * barWidth
//                    Log.d("Terminal", offsetX.toString())
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
                    val clickOffset = (size.width - clickOffsetX.value)
                    if (clickOffset in ((offsetX - barWidth / 4)..(offsetX + barWidth / 4))) {
                        Log.d("Terminal", "Click: $clickOffset")
                        onClickListener(bar)
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
    drawDashLine(
        start = Offset(0f, minPriceOffsetY),
        end = Offset(size.width, minPriceOffsetY)
    )
    drawTextPrice(textMeasurer, min, minPriceOffsetY)

}

fun DrawScope.drawTextPrice(
    textMeasurer: TextMeasurer,
    price: Float,
    offsetY: Float,
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
        topLeft = Offset(size.width - textLayoutResult.size.width - 4.dp.toPx(), offsetY)
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

