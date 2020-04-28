package com.st.STWINBoard_Gui.Utils

import androidx.annotation.StringRes
import com.google.android.material.textfield.TextInputLayout
import com.st.BlueSTSDK.gui.util.InputChecker.InputChecker

/*
* check that the user input is a nmber inside a specific range
* @param textInputLayout layout containing the textView
* @param errorMessageId error to display if the user input is wrong
* @param min min accepted value
* @param max max accepted value */
class CheckDoubleNumberRange (textInputLayout: TextInputLayout?, @StringRes errorMessageId: Int,
    min: Double?,
    max: Double?) : InputChecker(textInputLayout, errorMessageId) {

    private val validRange:ClosedRange<Double>

    init {
        val minValue = min ?: Double.NEGATIVE_INFINITY
        val maxValue = max ?: Double.POSITIVE_INFINITY
        validRange = minValue..maxValue
    }


    override fun validate(input: String): Boolean {
        return try {
            val value = input.toDouble()
            validRange.contains(value)
        } catch (e: java.lang.NumberFormatException) {
            false
        }
    }
}