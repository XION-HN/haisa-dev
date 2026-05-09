package com.haisa.sdk.service

import com.haisa.sdk.network.GitHubApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkProvider {

    private const val BASE_URL = "https://api.github.com/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun provideApiService(): GitHubApiService {
        return retrofit.create(GitHubApiService::class.java)
    }
}