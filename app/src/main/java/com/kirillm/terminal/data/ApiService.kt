package com.kirillm.terminal.data

import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {

    @GET("aggs/ticker/AAPL/range/{timeFrame}/2022-01-09/2023-01-09?adjusted=true&sort=desc&limit=50000&apiKey=POWmBhpJnApppbWsvO8tlZfM6z5gtGNH")
    suspend fun loadStockes(
        @Path("timeFrame") timeFrame: String,
    ): Result
}