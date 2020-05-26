package com.st.STWINBoard_Gui.Utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.Switch
import androidx.recyclerview.widget.RecyclerView
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.DeviceModel.Sensor
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.DeviceModel.SensorType
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.DeviceModel.SubSensorDescriptor
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.DeviceModel.SubSensorStatus
import com.st.clab.stwin.gui.R

internal class SubSensorPreviewViewAdapter(
        private val sensor: Sensor,
        private val onSubSensorEnableStatusChange: OnSubSensorEnableStatusChange
        ) : RecyclerView.Adapter<SubSensorPreviewViewAdapter.ViewHolder>() {

    //SubParam List
    private val mSubSensorList: List<SubSensorDescriptor> = sensor.sensorDescriptor.subSensorDescriptors
    private val mSubStatusList: List<SubSensorStatus> = sensor.sensorStatus.subSensorStatusList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_sub_sensor_preview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subSensorDescriptor = mSubSensorList[position]
        val subSensorStatus = mSubStatusList[position]

        holder.bind(subSensorDescriptor,subSensorStatus)
    }

    override fun getItemCount(): Int {
        return mSubSensorList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mIcon: ImageView = itemView.findViewById(R.id.subSensor_icon)
        private val mEnabledSwitch:Switch = itemView.findViewById(R.id.subSensor_enable)

        private var mSubSensor:SubSensorDescriptor? = null
        private var mSubSensorStatus:SubSensorStatus? = null

        private val onCheckedChangeListener  = object : CompoundButton.OnCheckedChangeListener{
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                val subSensor = mSubSensor ?: return
                onSubSensorEnableStatusChange(sensor,subSensor,isChecked)
            }
        }

        fun bind(subSensor:SubSensorDescriptor, status: SubSensorStatus){
            mSubSensor = subSensor
            mSubSensorStatus = status
            setSensorData(subSensor.sensorType)
            setEnableState(status.isActive)
        }

        private fun setEnableState(newState:Boolean){
            mEnabledSwitch.setOnCheckedChangeListener(null)
            mEnabledSwitch.isChecked = newState
            mEnabledSwitch.setOnCheckedChangeListener(onCheckedChangeListener)
        }

        private fun setSensorData(sensorType: SensorType) {
            mIcon.setImageResource(sensorType.imageResource)
        }
    }

    private val SensorType.imageResource:Int
    get() = when(this){
        SensorType.Accelerometer -> R.drawable.sensor_type_accelerometer
        SensorType.Magnetometer -> R.drawable.sensor_type_compass
        SensorType.Gyroscope -> R.drawable.sensor_type_gyroscope
        SensorType.Temperature -> R.drawable.sensor_type_temperature
        SensorType.Humidity -> R.drawable.sensor_type_humidity
        SensorType.Pressure -> R.drawable.sensor_type_pressure
        SensorType.Microphone -> R.drawable.sensor_type_microphone
        SensorType.MLC -> R.drawable.sensor_type_mlc
        SensorType.Unknown -> R.drawable.sensor_type_unknown
    }
}