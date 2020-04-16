/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.service

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.runBlocking
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.license.model.components.Link
import org.readium.r2.lcp.LCPError
import java.io.Serializable
import java.util.*

internal class DeviceService(private val repository: DeviceRepository, private val network: NetworkService, val context: Context):Serializable {

    val preferences: SharedPreferences = context.getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

    val id: String
        get() {
            val deviceId = preferences.getString("lcp_device_id", UUID.randomUUID().toString())!!
            preferences.edit().putString("lcp_device_id", deviceId).apply()
            return deviceId
        }
    val name: String
        get() {
            val deviceName = BluetoothAdapter.getDefaultAdapter()
            return deviceName?.name?.let  {
                it
            }?: run {
               "Android Unknown"
            }
        }

    val asQueryParameters: MutableList<Pair<String, Any?>>
        get() = mutableListOf("id" to id, "name" to name)


    fun registerLicense(license: LicenseDocument, link: Link, completion: (ByteArray?) -> Unit) {
        runBlocking {
            val registered = repository.isDeviceRegistered(license)
            if (registered) {
                completion(null)
            } else {
                // TODO templated url
                val url = link.url(asQueryParameters).toString()

                network.fetch(url, method = NetworkService.Method.post, params = asQueryParameters) { status, data ->
                    if (status != 200) {
                        completion(null)
                    }

                    repository.registerDevice(license)
                    completion(data)
                }
            }
        }
    }

}
