package com.st.STWINBoard_Gui.Utils

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
internal data class SaveSettings(
        var storeLocalCopy:Boolean = false,
        var setAsDefault:Boolean = false
): Parcelable