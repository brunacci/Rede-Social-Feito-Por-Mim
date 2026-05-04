package com.rdsocial.domain.repository

interface LocationRepository {
    suspend fun getCurrentCityNameOrNull(): Result<String?>
}
