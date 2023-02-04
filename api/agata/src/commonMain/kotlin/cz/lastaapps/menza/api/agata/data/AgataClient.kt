/*
 *    Copyright 2023, Petr Laštovička as Lasta apps, All rights reserved
 *
 *     This file is part of Menza.
 *
 *     Menza is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Menza is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Menza.  If not, see <https://www.gnu.org/licenses/>.
 */

package cz.lastaapps.menza.api.agata.data

import cz.lastaapps.menza.api.agata.domain.model.AgataBEConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@JvmInline
internal value class AgataClient(val client: HttpClient)

@OptIn(ExperimentalSerializationApi::class)
internal fun createAgataClient(
    httpClient: HttpClient,
    beConfig: AgataBEConfig,
) = AgataClient(httpClient.config {
    install(DefaultRequest) {
        url {
            protocol = beConfig.protocol
            host = beConfig.host
            path(beConfig.apiPath)
            parameters.append("api", beConfig.apiKey)
        }
    }

    install(ContentNegotiation) {
        json(Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            explicitNulls = true
        })
    }
})
