package com.kirillm.terminal.data

import com.google.gson.annotations.SerializedName

data class Result(
    @SerializedName("results") val barsList: List<Bar>,
)