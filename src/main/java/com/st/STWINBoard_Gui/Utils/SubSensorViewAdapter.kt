package com.st.STWINBoard_Gui.Utils

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.st.BlueSTSDK.HSDatalog.Sensor
import com.st.BlueSTSDK.HSDatalog.SubSensorDescriptor
import com.st.BlueSTSDK.HSDatalog.SubSensorStatus
import com.st.STWINBoard_Gui.Utils.SensorViewAdapter.*
import com.st.clab.stwin.gui.R

class SubSensorViewAdapter(
        sensor: Sensor,
        private val subSensorIconClickedListener: OnSubSensorIconClickedListener?,
        private val subSensorEditTextListener: OnSubSensorEditTextChangedListener) : RecyclerView.Adapter<SubSensorViewAdapter.ViewHolder>() {
    private val sID: Int = sensor.id

    //SubParam List
    private val mSubSensorList: List<SubSensorDescriptor> = sensor.sensorDescriptor.subSensorDescriptors
    private val mSubStatusList: List<SubSensorStatus> = sensor.sensorStatus.subSensorStatusList

    companion object {

        /**
         * filter for convert a color image in a gray scale one
         */
        val sToGrayScale: ColorMatrixColorFilter by lazy{
            val temp = ColorMatrix()
            temp.setSaturation(0.0f)
            ColorMatrixColorFilter(temp)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_sub_sensor, parent, false)
        return ViewHolder(sID,view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subSensorDescriptor = mSubSensorList[position]
        val subSensorStatus = mSubStatusList[position]

        holder.bind(subSensorDescriptor,subSensorStatus)

        //manageSubSensorStatus(subSensorStatus, holder.mSubSensorIcon, holder.mSubSensorRowLayoutMask)
    }

    override fun getItemCount(): Int {
        return mSubSensorList.size
    }

    private fun manageSubSensorStatus(subSensorStatus: SubSensorStatus, subSensorIcon: ImageView, layoutMask: LinearLayout) {
        if (subSensorStatus.isActive) {
            subSensorIcon.clearColorFilter()
            layoutMask.isClickable = false
            layoutMask.visibility = View.INVISIBLE
        } else {
            subSensorIcon.colorFilter = sToGrayScale
            layoutMask.isClickable = true
            layoutMask.visibility = View.VISIBLE
        }
    }

    inner class ViewHolder(sensorId:Int,itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mSubSensorIcon: ImageView = itemView.findViewById(R.id.subSensor_icon)
        private val mSubSensorType: TextView = itemView.findViewById(R.id.subSensor_name)
        private val mSubSensorListView: RecyclerView = itemView.findViewById(R.id.subSensor_paramsList)
        private val mSubSensorEnabled:Switch = itemView.findViewById(R.id.subSensor_enable)
        private var mSubSensor:SubSensorDescriptor? = null
        private var mSubSensorStatus:SubSensorStatus? = null

        private val onCheckedChangeListener  = object : CompoundButton.OnCheckedChangeListener{
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                val subSensor = mSubSensor ?: return
                subSensorIconClickedListener?.onSubSensorChangeActivationStatus(sensorId,subSensor.id,isChecked)
            }
        }

        fun bind(subSensor:SubSensorDescriptor,subSensorStatus: SubSensorStatus){
            mSubSensor = subSensor
            mSubSensorStatus = subSensorStatus

            setSensorData(subSensor.sensorType)

            mSubSensorListView.adapter = DescriptorParamViewAdapter(
                    subSensor.subDescriptorParams,
                    subSensorStatus.params){ descriptorParam, newValue ->
                    subSensorEditTextListener.onSubSensorEditTextValueChanged(sID,subSensor.id,descriptorParam.name,newValue)
            }

            mSubSensorEnabled.setOnCheckedChangeListener(null)
            mSubSensorEnabled.isChecked = subSensorStatus.isActive
            mSubSensorEnabled.setOnCheckedChangeListener(onCheckedChangeListener)
        }

        private fun setSensorData(sensorType: String) {
            val subSensorIcon:Int
            val subSensorTypeLabel:String
            when (sensorType) {
                "ACC" -> {
                    subSensorIcon = R.drawable.ic_accelerometer
                    subSensorTypeLabel = "Accelerometer"
                }
                "MAG" -> {
                    subSensorIcon = R.drawable.ic_compass
                    subSensorTypeLabel = "Magnetometer"
                }
                "GYRO" -> {
                    subSensorIcon = R.drawable.ic_gyroscope
                    subSensorTypeLabel = "Gyroscope"
                }
                "TEMP" -> {
                    subSensorIcon = R.drawable.ic_temperature
                    subSensorTypeLabel = "Temperature"
                }
                "HUM" -> {
                    subSensorIcon =  R.drawable.ic_humidity
                    subSensorTypeLabel = "Humidity"
                }
                "PRESS" -> {
                    subSensorIcon =  R.drawable.ic_pressure
                    subSensorTypeLabel = "Pressure"
                }
                "MIC" -> {
                    subSensorIcon = R.drawable.ic_microphone
                    subSensorTypeLabel = "Microphone"
                }
                else ->{
                    subSensorTypeLabel = ""
                    subSensorIcon = R.drawable.ic_st_placeholder
                }
            }
            mSubSensorIcon.setImageResource(subSensorIcon)
            mSubSensorType.text = subSensorTypeLabel
        }

    }


}