/*
 * Copyright 2019-2026, Tomasz Żebrowski
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.obd.graphs.bl.datalogger.dtc

import org.obd.graphs.commons.R
import org.obd.metrics.api.model.DiagnosticTroubleCode

// The failure type byte, the "-14" in "P0120-14" (SAE J2012 / ISO 15031-6): the high nibble is the category,
// the whole byte the specific fault. ObdMetrics' own table is empty (every code reads "Unknown Subtype"), so the
// generic one lives here, translated with the app.
internal val FAILURE_TYPES: Map<Int, Int> =
    mapOf(
        0x11 to R.string.dtc_ftb_11,
        0x12 to R.string.dtc_ftb_12,
        0x13 to R.string.dtc_ftb_13,
        0x14 to R.string.dtc_ftb_14,
        0x15 to R.string.dtc_ftb_15,
        0x16 to R.string.dtc_ftb_16,
        0x17 to R.string.dtc_ftb_17,
        0x18 to R.string.dtc_ftb_18,
        0x19 to R.string.dtc_ftb_19,
        0x1A to R.string.dtc_ftb_1A,
        0x1B to R.string.dtc_ftb_1B,
        0x1C to R.string.dtc_ftb_1C,
        0x1D to R.string.dtc_ftb_1D,
        0x1E to R.string.dtc_ftb_1E,
        0x1F to R.string.dtc_ftb_1F,
        0x21 to R.string.dtc_ftb_21,
        0x22 to R.string.dtc_ftb_22,
        0x23 to R.string.dtc_ftb_23,
        0x24 to R.string.dtc_ftb_24,
        0x25 to R.string.dtc_ftb_25,
        0x26 to R.string.dtc_ftb_26,
        0x27 to R.string.dtc_ftb_27,
        0x28 to R.string.dtc_ftb_28,
        0x29 to R.string.dtc_ftb_29,
        0x2A to R.string.dtc_ftb_2A,
        0x2B to R.string.dtc_ftb_2B,
        0x2F to R.string.dtc_ftb_2F,
        0x31 to R.string.dtc_ftb_31,
        0x32 to R.string.dtc_ftb_32,
        0x33 to R.string.dtc_ftb_33,
        0x34 to R.string.dtc_ftb_34,
        0x35 to R.string.dtc_ftb_35,
        0x36 to R.string.dtc_ftb_36,
        0x37 to R.string.dtc_ftb_37,
        0x38 to R.string.dtc_ftb_38,
        0x39 to R.string.dtc_ftb_39,
        0x3A to R.string.dtc_ftb_3A,
        0x41 to R.string.dtc_ftb_41,
        0x42 to R.string.dtc_ftb_42,
        0x43 to R.string.dtc_ftb_43,
        0x44 to R.string.dtc_ftb_44,
        0x45 to R.string.dtc_ftb_45,
        0x46 to R.string.dtc_ftb_46,
        0x47 to R.string.dtc_ftb_47,
        0x48 to R.string.dtc_ftb_48,
        0x49 to R.string.dtc_ftb_49,
        0x4A to R.string.dtc_ftb_4A,
        0x4B to R.string.dtc_ftb_4B,
        0x51 to R.string.dtc_ftb_51,
        0x52 to R.string.dtc_ftb_52,
        0x53 to R.string.dtc_ftb_53,
        0x54 to R.string.dtc_ftb_54,
        0x55 to R.string.dtc_ftb_55,
        0x61 to R.string.dtc_ftb_61,
        0x62 to R.string.dtc_ftb_62,
        0x63 to R.string.dtc_ftb_63,
        0x64 to R.string.dtc_ftb_64,
        0x65 to R.string.dtc_ftb_65,
        0x66 to R.string.dtc_ftb_66,
        0x67 to R.string.dtc_ftb_67,
        0x68 to R.string.dtc_ftb_68,
        0x71 to R.string.dtc_ftb_71,
        0x72 to R.string.dtc_ftb_72,
        0x73 to R.string.dtc_ftb_73,
        0x74 to R.string.dtc_ftb_74,
        0x75 to R.string.dtc_ftb_75,
        0x76 to R.string.dtc_ftb_76,
        0x77 to R.string.dtc_ftb_77,
        0x78 to R.string.dtc_ftb_78,
        0x79 to R.string.dtc_ftb_79,
        0x7A to R.string.dtc_ftb_7A,
        0x7B to R.string.dtc_ftb_7B,
        0x81 to R.string.dtc_ftb_81,
        0x82 to R.string.dtc_ftb_82,
        0x83 to R.string.dtc_ftb_83,
        0x84 to R.string.dtc_ftb_84,
        0x85 to R.string.dtc_ftb_85,
        0x86 to R.string.dtc_ftb_86,
        0x87 to R.string.dtc_ftb_87,
        0x88 to R.string.dtc_ftb_88,
        0x91 to R.string.dtc_ftb_91,
        0x92 to R.string.dtc_ftb_92,
        0x93 to R.string.dtc_ftb_93,
        0x94 to R.string.dtc_ftb_94,
        0x95 to R.string.dtc_ftb_95,
        0x96 to R.string.dtc_ftb_96,
        0x97 to R.string.dtc_ftb_97,
        0x98 to R.string.dtc_ftb_98
    )

// By the high nibble: what a byte missing from FAILURE_TYPES still says. 0xA0-0xEF are reserved.
internal val FAILURE_TYPE_CATEGORIES: Map<Int, Int> =
    mapOf(
        0x0 to R.string.dtc_ftb_category_0,
        0x1 to R.string.dtc_ftb_category_1,
        0x2 to R.string.dtc_ftb_category_2,
        0x3 to R.string.dtc_ftb_category_3,
        0x4 to R.string.dtc_ftb_category_4,
        0x5 to R.string.dtc_ftb_category_5,
        0x6 to R.string.dtc_ftb_category_6,
        0x7 to R.string.dtc_ftb_category_7,
        0x8 to R.string.dtc_ftb_category_8,
        0x9 to R.string.dtc_ftb_category_9,
        0xF to R.string.dtc_ftb_category_F
    )

// What ObdMetrics puts in failureType.description when it has no text for the byte.
private const val OBD_METRICS_UNKNOWN_SUBTYPE = "Unknown Subtype"

// The table text for a failure type byte ("14", "1a"), null for anything that is not one hex byte.
// fallback: the decoder's own text, used for a byte the table has no specific entry for.
fun failureTypeText(
    code: String?,
    fallback: String?,
    getString: (Int) -> String
): String? {
    val byte = code?.trim()?.takeIf { it.length == 2 }?.toIntOrNull(16) ?: return null
    FAILURE_TYPES[byte]?.let { return getString(it) }
    fallback?.trim()?.takeUnless { it.isEmpty() || it.equals(OBD_METRICS_UNKNOWN_SUBTYPE, ignoreCase = true) }?.let { return it }
    return FAILURE_TYPE_CATEGORIES[byte shr 4]?.let(getString)
}

// "Failure type: 14 · Circuit short to ground or open", null when the code carries no (known) failure type.
fun DiagnosticTroubleCode.failureTypeLine(getString: (Int) -> String): String? {
    val type = failureType ?: return null
    val code = type.code?.trim()?.uppercase() ?: return null
    val text = failureTypeText(code, type.description, getString) ?: return null
    return getString(R.string.dtc_ftb_line).format(code, text)
}
