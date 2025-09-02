package com.example.finedu.di

import com.example.finedu.api.FinnhubService
import com.example.finedu.api.CoinMarketCapService
import com.example.finedu.repository.PriceRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.InstallIn

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideFinnhubService(): FinnhubService = FinnhubService()

    @Provides
    fun provideCoinGeckoService(): CoinMarketCapService = CoinMarketCapService()

    @Provides
    fun providePriceRepository(
        finnhubService: FinnhubService,
        CoinMarketCapService: CoinMarketCapService
    ): PriceRepository = PriceRepository(finnhubService, CoinMarketCapService)
}
