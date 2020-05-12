package com.st.STWINBoard_Gui.Utils

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.DeviceModel.Sensor
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.DeviceModel.SubSensorDescriptor
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.DeviceModel.SubSensorStatus
import com.st.STWINBoard_Gui.Utils.SensorViewAdapter.*
import com.st.clab.stwin.gui.R

class SubSensorViewAdapter(
        sensor: Sensor,
        private val sensorEnableChangeListener: OnSubSensorIconClickedListener?,
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
        private val mIcon: ImageView = itemView.findViewById(R.id.subSensor_icon)
        private val mName: TextView = itemView.findViewById(R.id.subSensor_name)
        private val mEnabledSwitch:Switch = itemView.findViewById(R.id.subSensor_enable)
        private val mOdrSelector:Spinner = itemView.findViewById(R.id.subSensor_odrSelector)
        private val mFsSelector:Spinner = itemView.findViewById(R.id.subSensor_fsSelector)
        private val mFsUnit:TextView = itemView.findViewById(R.id.subSensor_fsUnit)
        private val sampleSelector:TextInputEditText = itemView.findViewById(R.id.subSensor_sampleValue)
        private val sampleSelectorLayout:TextInputLayout = itemView.findViewById(R.id.subSensor_sampleLayout)

        private var mSubSensor:SubSensorDescriptor? = null
        private var mSubSensorStatus:SubSensorStatus? = null

        private val onCheckedChangeListener  = object : CompoundButton.OnCheckedChangeListener{
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                val subSensor = mSubSensor ?: return
                sensorEnableChangeListener?.onSubSensorChangeActivationStatus(sensorId,subSensor.id,isChecked)
            }
        }

        init {
            mOdrSelector.onUserSelectedItemListener = OnUserSelectedListener(object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) { }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedValue = parent?.getItemAtPosition(position) as Double
                    Log.d("SubSensor","ODR onItemChange $selectedValue")
                }
            })

            mFsSelector.onUserSelectedItemListener = OnUserSelectedListener(object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) { }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedValue = parent?.getItemAtPosition(position) as Int
                    Log.d("SubSensor","FS onItemChange $selectedValue")
                }
            })
        }

        fun bind(subSensor:SubSensorDescriptor, status: SubSensorStatus){
            mSubSensor = subSensor
            mSubSensorStatus = status

            mFsUnit.text = subSensor.unit
            setSensorData(subSensor.sensorType)

            setEnableState(status.isActive)
            setOdr(subSensor.odr,status.odr)
            setFS(subSensor.fs,status.fs)
        }

        private fun setOdr(odrValues: List<Double>?, currentValue: Double?) {
            mOdrSelector.isEnabled = odrValues!=null
            if(odrValues == null)
                return

            val selectedIndex = if(currentValue !=null) {
                val index = odrValues.indexOf(currentValue)
                if(index >0) index else 0
            }else{
                0
            }

            val spinnerAdapter = ArrayAdapter(mOdrSelector.context,
                    android.R.layout.simple_spinner_item, odrValues).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            mOdrSelector.adapter = spinnerAdapter
            mOdrSelector.setSelection(selectedIndex)
        }

        private fun setFS(fsValues: List<Int>?, currentValue: Int?) {
            mFsSelector.isEnabled = fsValues!=null
            if(fsValues == null)
                return

            val selectedIndex = if(currentValue !=null) {
                val index = fsValues.indexOf(currentValue)
                if(index >0) index else 0
            }else{
                0
            }

            val spinnerAdapter = ArrayAdapter(mFsSelector.context,
                    android.R.layout.simple_spinner_item, fsValues).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            mFsSelector.adapter = spinnerAdapter
            mFsSelector.setSelection(selectedIndex)
        }

        private fun setEnableState(newState:Boolean){
            mEnabledSwitch.setOnCheckedChangeListener(null)
            mEnabledSwitch.isChecked = newState
            mEnabledSwitch.setOnCheckedChangeListener(onCheckedChangeListener)
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
            mIcon.setImageResource(subSensorIcon)
            mName.text = subSensorTypeLabel
        }

    }


}