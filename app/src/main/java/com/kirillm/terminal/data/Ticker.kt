package com.kirillm.terminal.data

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Ticker(
    @SerializedName("ticker") val tickerId: String,
    @SerializedName("name") val name: String
): Parcelable {

    override fun toString(): String {
        return "($tickerId) $name"
    }
}
