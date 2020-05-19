package com.st.STWINBoard_Gui.Utils

import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.DeviceModel.Sensor

internal data class SensorViewData(
        val sensor: Sensor,
        val isCollapsed:Boolean)
