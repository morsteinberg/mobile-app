package com.even.map.terraexplorer.constants

import com.skyline.teapi81.AttributeTypeCode

internal enum class Attribute(val type: Int) {
    ICON(AttributeTypeCode.AT_TEXT),
    LABEL(AttributeTypeCode.AT_TEXT);

    fun toClassification(): String {
        return "<Classification FuncType=\"0\"><Class><Value>[$name]</Value></Class></Classification>"
    }

    companion object {
        fun stringifyValues(values: List<Any?>) =
            values.joinToString(separator = ";") {
                if (it != null && it.toString().isNotBlank()) it.toString() else " "
            }
    }
}
