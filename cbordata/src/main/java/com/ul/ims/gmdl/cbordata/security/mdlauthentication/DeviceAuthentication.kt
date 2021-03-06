/*
 * Copyright (C) 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.ul.ims.gmdl.cbordata.security.mdlauthentication

import android.util.Log
import co.nstant.`in`.cbor.CborBuilder
import co.nstant.`in`.cbor.CborDecoder
import co.nstant.`in`.cbor.CborEncoder
import co.nstant.`in`.cbor.CborException
import co.nstant.`in`.cbor.model.*
import co.nstant.`in`.cbor.model.Array
import com.ul.ims.gmdl.cbordata.doctype.DocType
import com.ul.ims.gmdl.cbordata.doctype.IDoctype
import com.ul.ims.gmdl.cbordata.generic.AbstractCborStructure
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Serializable

class DeviceAuthentication private constructor (
    val sessionTransript: SessionTranscript?,
    val docType: IDoctype?,
    val deviceNameSpaces : DeviceNameSpaces?
) : AbstractCborStructure(), Serializable{

    companion object {
        private const val LOG_TAG = "DeviceAuthentication"
        private const val LABEL = "DeviceAuthentication"
        const val CBOR_TAG = 24L
    }

    override fun encode(): ByteArray {
        val outputStream = ByteArrayOutputStream()
        var builder = CborBuilder()
        var arrayBuilder = builder.addArray()

        arrayBuilder = arrayBuilder.add(toDataItem(LABEL))

        if (sessionTransript == null) {
            throw CborException("SessionTranscript cannot be null")
        }

        arrayBuilder = arrayBuilder.add(encodeSessionTranscript(sessionTransript))
        if (docType?.docType == null) {
            throw CborException("DocType cannot be null")
        }
        arrayBuilder = arrayBuilder.add(docType.docType)
        if (deviceNameSpaces == null) {
            throw CborException("DeviceNameSpaces cannot be null")
        }

        val deviceNspaces = toDataItem(deviceNameSpaces)
        deviceNspaces.tag = Tag(CBOR_TAG)
        arrayBuilder = arrayBuilder.add(deviceNspaces)

        builder = arrayBuilder.end()
        CborEncoder(outputStream).encode(builder.build())
        return outputStream.toByteArray()
    }

    private fun encodeSessionTranscript(st: SessionTranscript): DataItem {
        val array = Array()
        if (st.deviceEngagement == null || st.readerKey == null) {
            throw CborException("Neither DeviceEngagement nor ReaderKey can be null in SessionTranscript.")
        }

        val de = toDataItem(st.deviceEngagement)
        de.tag = Tag(CBOR_TAG)
        array.add(de)

        val rk = toDataItem(st.readerKey)
        rk.tag = Tag(CBOR_TAG)
        array.add(rk)

        return array
    }

    class Builder {
        private var sessionTranscript : SessionTranscript? = null
        private var docType : IDoctype? = null
        private var deviceNameSpaces : DeviceNameSpaces? = null

        fun setDocType(string: String?) = apply {
            string?.let {
                docType = DocType(string)
            }
        }

        fun setSessionTranscript(sessionTransript: SessionTranscript?) = apply {
            sessionTransript?.let {
                sessionTranscript = sessionTransript
            }
        }

        fun setDeviceNameSpaces(nameSpaces: DeviceNameSpaces?) = apply {
            nameSpaces?.let {
                deviceNameSpaces = nameSpaces
            }
        }

        fun decode(data : ByteArray) = apply {
            try {
                val stream = ByteArrayInputStream(data)
                val dataItems = CborDecoder(stream).decode()
                if (dataItems.size > 0) {
                    val structureItems : Array? = dataItems[0] as? Array
                    structureItems?.let {
                        decode(it)
                    }
                }

            } catch (ex: CborException) {
                Log.e(LOG_TAG, ex.message, ex)
            }
        }

        private fun decode(array: Array) {
            if (array.dataItems.size == 4) {
                decodeLabel(array.dataItems[0])
                val sessionTransript : Array? = array.dataItems[1] as? Array
                decodeSessionTranscript(sessionTransript)
                decodeDocType((array.dataItems[2] as? UnicodeString)?.string)
                decodeDeviceNameSpaces(array.dataItems[3])
            }
        }

        private fun decodeDeviceNameSpaces(dataItem: DataItem?) {
            dataItem?.let {
                if (dataItem.majorType == MajorType.MAP) {
//                    deviceNameSpaces = DeviceNameSpaces.Builder().decode(dataItem as Map).build()
                }
            }
        }

        private fun decodeDocType(string: String?) {
            string?.let {
                docType = DocType(string)
            }
        }

        private fun decodeSessionTranscript(sessionTransript: Array?) {
            sessionTranscript = SessionTranscript.Builder().decode(sessionTransript).build()
        }

        private fun decodeLabel(dataItem: DataItem?) {
            val label = dataItem as? UnicodeString
            label?.let {
                if (label.string != LABEL) {
                    throw CborException("Invalid DeviceAuthentication label.")
                }
            }
        }

        fun build() = DeviceAuthentication(sessionTranscript, docType, deviceNameSpaces)
    }
}