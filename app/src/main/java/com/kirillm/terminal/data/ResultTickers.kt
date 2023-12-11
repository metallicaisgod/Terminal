package com.kirillm.terminal.data

import com.google.gson.annotations.SerializedName

data class ResultTickers(
    @SerializedName("results") val tickerList: List<Ticker>,
)