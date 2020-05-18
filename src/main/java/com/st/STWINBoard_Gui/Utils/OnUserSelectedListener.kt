package com.st.STWINBoard_Gui.Utils

import android.view.MotionEvent
import android.view.View
import android.widget.AbsSpinner
import android.widget.AdapterView

internal class OnUserSelectedListener (private val callback:AdapterView.OnItemSelectedListener):
        AdapterView.OnItemSelectedListener,
        View.OnTouchListener {
    private var userSelect = false

    override fun onNothingSelected(parent: AdapterView<*>?) = callback.onNothingSelected(parent)

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (!userSelect) {
            return
        }
        callback.onItemSelected(parent,view, position, id)
        userSelect = false;
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        userSelect = true;
        return false;
    }

}

internal var AbsSpinner.onUserSelectedItemListener:OnUserSelectedListener?
    get() = onItemSelectedListener as? OnUserSelectedListener
    set(value) {
        setOnTouchListener(value)
        onItemSelectedListener = value
    }
