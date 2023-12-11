package com.kirillm.terminal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kirillm.terminal.data.ApiFactory
import com.kirillm.terminal.data.Bar
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
        loadContent()
    }

    fun loadContent(timeFrame: TimeFrame = TimeFrame.HOUR_1) {
        lastState = _screenState.value
        _screenState.value = TerminalScreenState.Loading
        viewModelScope.launch(exeptionHandler) {
            val result = api.loadStockes(timeFrame.value)
            _screenState.value = TerminalScreenState.Content(barsList = result.barsList, timeFrame)
        }
    }

    fun showBarInfo(bar: Bar?) {
        val currentState = _screenState.value as TerminalScreenState.Content
        _screenState.value = TerminalScreenState.Content(currentState.barsList, currentState.timeFrame, bar)
    }
}

sealed class TerminalScreenState {

    object Initial : TerminalScreenState()

    object Loading : TerminalScreenState()

    data class Content(val barsList: List<Bar>, val timeFrame: TimeFrame, val barForInfo: Bar? = null) : TerminalScreenState()
}