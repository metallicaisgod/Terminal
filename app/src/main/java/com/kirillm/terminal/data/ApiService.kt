package com.kirillm.terminal.data

import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {

    @GET("v2/aggs/ticker/{tickerId}/range/{timeFrame}/2022-01-09/2023-01-09?adjusted=true&sort=desc&limit=50000&apiKey=POWmBhpJnApppbWsvO8tlZfM6z5gtGNH")
    suspend fun loadStockes(
        @Path("timeFrame") timeFrame: String,
        @Path("tickerId") tickerId: String,
    ): Result

    @GET("v3/reference/tickers?active=true&apiKey=POWmBhpJnApppbWsvO8tlZfM6z5gtGNH")
    suspend fun loadAllTickers(): ResultTickers
}