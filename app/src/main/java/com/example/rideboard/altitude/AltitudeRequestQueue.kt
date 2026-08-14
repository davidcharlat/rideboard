package com.example.rideboard.altitude

import com.example.rideboard.altitude.database.AltitudeRepository
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

object AltitudeRequestQueue {
    private val queue = mutableListOf<QueuedAltitudeRequest>()

    // thread-safe add
    @Synchronized
    fun add(request: QueuedAltitudeRequest) {
        queue.add(request)
    }

    // expose copy for UI/debugj
    @Synchronized
    fun snapshot(): List<QueuedAltitudeRequest> = queue.toList()

    // processing : on injecte repository + map des providers pour réexécution
    suspend fun processAll(
        repository: AltitudeRepository,
        providers: Map<String, AltitudeProvider>
    ) {
        val iterator = queue.iterator()
        while (iterator.hasNext()) {
            val r = iterator.next()
            if (r.attempts >= r.maxAttempts) {
                // give up (ou log/archiver)
                iterator.remove()
                continue
            }

            try {
                r.attempts += 1
                val provider = providers[r.providerName]
                    ?: throw IllegalStateException("No provider ${r.providerName}")

                // Appelle le provider — on suppose qu'il tente réseau+DB
                val alt = provider.getAltitude(r.latitude, r.longitude)
                if (alt != null && alt > -500.0) {
                    repository.insertAltitude(r.providerName, r.latitude, r.longitude, alt)
                    // tout est OK : on retire de la queue (provider a déjà sauvé en DB si c'est sa responsabilité)
                    iterator.remove()
                } else {
                    // provider a renvoyé null (ex: pas d'altitude) -> on peut choisir de retirer aussi
                    iterator.remove()
                }
            } catch (e: Exception) {
                // échec réseau ou autre : on garde l'entrée (tentatives augmentées)
                // pause légère pour ne pas spammer
            }
            kotlinx.coroutines.delay(1000L)
        }
    }

    suspend fun processOne(
        repository: AltitudeRepository,
        provider: AltitudeProvider?
    ) {
        if (provider == null) return

        val iterator = queue.iterator()

        while (iterator.hasNext()) {
            val r = iterator.next()

            if (
                r.providerName == provider.name ||
                (provider.name == "IGN" &&
                        (r.providerName == "L_MNT" || r.providerName == "L_MNS"))
            ) {
                if (r.attempts >= r.maxAttempts) {
                    // give up (ou log/archiver)
                    iterator.remove()
                }
                try {
                    r.attempts += 1

                    // Appelle le provider — on suppose qu'il tente réseau+DB
                    val alt = provider.getAltitude(r.latitude, r.longitude)

                    if (alt != null && alt > -500.0) {
                        // tout est OK : on retire de la queue (provider a déjà sauvé en DB si c'est sa responsabilité)
                        iterator.remove()
                    } else {
                        // provider a renvoyé null (ex: pas d'altitude) -> on peut choisir de retirer aussi
                        iterator.remove()
                    }
                } catch (e: Exception) {
                    // échec réseau ou autre : on garde l'entrée (tentatives augmentées)
                    // pause légère pour ne pas spammer
                }
                break
            }
        }
        return
    }
}