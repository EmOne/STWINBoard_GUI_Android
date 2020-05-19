package com.st.STWINBoard_Gui.Utils

import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.DeviceModel.Sensor

internal data class SensorViewData(
        var sensor: Sensor,
        var isCollapsed:Boolean)
