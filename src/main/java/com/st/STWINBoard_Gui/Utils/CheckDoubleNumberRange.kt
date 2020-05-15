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
class CheckIntNumberRange (textInputLayout: TextInputLayout, errorMessage:Int,
    min: Int?,
    max: Int?) : InputChecker(textInputLayout, errorMessage) {

    private val validRange:ClosedRange<Int>

    init {
        val minValue = min ?: Int.MIN_VALUE
        val maxValue = max ?: Int.MAX_VALUE
        validRange = minValue..maxValue
    }


    override fun validate(input: String): Boolean {
        return try {
            val value = input.toInt()
            validRange.contains(value)
        } catch (e: java.lang.NumberFormatException) {
            false
        }
    }
}