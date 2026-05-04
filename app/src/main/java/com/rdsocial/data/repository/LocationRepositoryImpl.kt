package com.rdsocial.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.rdsocial.domain.repository.LocationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationProviderClient: FusedLocationProviderClient,
) : LocationRepository {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentCityNameOrNull(): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val location = withTimeout(LOCATION_TIMEOUT_MS) { obtainBestLocation() }
            val city = resolveCityName(location) ?: error("CITY_NOT_FOUND")
            Result.success(city)
        } catch (_: TimeoutCancellationException) {
            Result.failure(LocationRepositoryException("LOCATION_TIMEOUT"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private suspend fun obtainBestLocation(): Location {
        fusedLocationProviderClient
            .getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token,
            )
            .await()
            ?.let { return it }

        fusedLocationProviderClient.lastLocation.await()?.let { return it }

        return requestSingleLocationUpdate()
    }

    private suspend fun requestSingleLocationUpdate(): Location = suspendCancellableCoroutine { cont ->
        val request = LocationRequest.Builder(2_000L)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMinUpdateIntervalMillis(500L)
            .setMaxUpdates(1)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                try {
                    fusedLocationProviderClient.removeLocationUpdates(this)
                } catch (_: Exception) {
                }
                val loc = result.lastLocation ?: result.locations.lastOrNull()
                if (cont.isActive) {
                    if (loc != null) {
                        cont.resume(loc)
                    } else {
                        cont.resumeWithException(LocationRepositoryException("LOCATION_UNAVAILABLE"))
                    }
                }
            }
        }

        cont.invokeOnCancellation {
            try {
                fusedLocationProviderClient.removeLocationUpdates(callback)
            } catch (_: Exception) {
            }
        }

        try {
            fusedLocationProviderClient.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper(),
            )
        } catch (e: SecurityException) {
            cont.resumeWithException(e)
        }
    }

    private suspend fun resolveCityName(location: Location): String? {
        val lat = location.latitude
        val lng = location.longitude
        getCityNameFromCoordinates(Geocoder(context, Locale.getDefault()), lat, lng)?.let { return it }
        if (Locale.getDefault() != Locale.US) {
            getCityNameFromCoordinates(Geocoder(context, Locale.US), lat, lng)?.let { return it }
        }
        return null
    }

    private suspend fun getCityNameFromCoordinates(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double,
    ): String? {
        return suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocation(
                latitude,
                longitude,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (addresses.isNotEmpty()) {
                            val address = addresses[0]
                            val city = address.locality ?: address.subAdminArea ?: address.adminArea

                            if (continuation.isActive) {
                                continuation.resume(city)
                            }
                        } else {
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }
                    }

                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                },
            )
        }
    }

    private class LocationRepositoryException(message: String) : Exception(message)

    private companion object {
        const val LOCATION_TIMEOUT_MS = 30_000L
    }
}
