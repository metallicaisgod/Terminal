package com.kirillm.terminal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kirillm.terminal.data.ApiFactory
import com.kirillm.terminal.data.Bar
import com.kirillm.terminal.data.Ticker
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val api = ApiFactory.apiService

    private val _screenState = MutableStateFlow<TerminalScreenState>(TerminalScreenState.Initial)
    val screenState
        get() = _screenState.asStateFlow()

    private var lastState: TerminalScreenState = TerminalScreenState.Initial

    private val exeptionHandler = CoroutineExceptionHandler { _, throwable ->
        _screenState.value = lastState
    }

    init {
        loadTickers()
    }

    private fun loadTickers() {
        viewModelScope.launch(exeptionHandler) {
            val result = api.loadAllTickers()
            _screenState.value = TerminalScreenState.Content(
                tickerList = result.tickerList,
                result.tickerList[0],
                listOf(),
                TimeFrame.HOUR_1
            )
            loadContent(result.tickerList[0])
        }
    }

    fun loadContent(ticker: Ticker, timeFrame: TimeFrame = TimeFrame.HOUR_1) {
        lastState = _screenState.value
        val currentState = _screenState.value as TerminalScreenState.Content
        _screenState.value = TerminalScreenState.Loading
        viewModelScope.launch(exeptionHandler) {
            val result = api.loadStockes(timeFrame.value, ticker.tickerId)
            _screenState.value = TerminalScreenState.Content(
                tickerList = currentState.tickerList,
                barsList = result.barsList,
                timeFrame = timeFrame,
                currentTicker = ticker
            )
        }
    }

    fun showBarInfo(bar: Bar?) {
        val currentState = _screenState.value as TerminalScreenState.Content
        _screenState.value = TerminalScreenState.Content(
            tickerList = currentState.tickerList,
            barsList = currentState.barsList,
            timeFrame = currentState.timeFrame,
            currentTicker = currentState.currentTicker,
            barForInfo = bar
        )
    }
}

sealed class TerminalScreenState {

    object Initial : TerminalScreenState()

    object Loading : TerminalScreenState()

    data class Content(
        val tickerList: List<Ticker>,
        val currentTicker: Ticker,
        val barsList: List<Bar>,
        val timeFrame: TimeFrame,
        val barForInfo: Bar? = null,
    ) : TerminalScreenState()
}