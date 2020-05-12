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
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.DeviceModel.*
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
        private val mSampleTSValue:TextInputEditText = itemView.findViewById(R.id.subSensor_sampleValue)
        private val mSampleTSLayout:TextInputLayout = itemView.findViewById(R.id.subSensor_sampleLayout)

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
            setSample(subSensor.samplesPerTs,status.samplesPerTs)
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

        private fun setSample(settings:SamplesPerTs,currentValue: Int?){
            val errorMessage = mSampleTSLayout.context.getString(R.string.subSensor_sampleErrorFromat,settings.min,settings.max)
            val inputChecker = CheckIntNumberRange(mSampleTSLayout, errorMessage, settings.min,
                    settings.max)
            mSampleTSValue.addTextChangedListener(inputChecker)
            val value = currentValue ?: settings.min
            mSampleTSValue.setText(value.toString())
        }

        private fun setSensorData(sensorType: SensorType) {
            mIcon.setImageResource(sensorType.imageResource)
            mName.setText(sensorType.nameResource)
        }

    }

    private val SensorType.imageResource:Int
    get() = when(this){
        SensorType.Accelerometer -> R.drawable.ic_accelerometer
        SensorType.Magnetometer -> R.drawable.ic_compass
        SensorType.Gyroscope -> R.drawable.ic_gyroscope
        SensorType.Temperature -> R.drawable.ic_temperature
        SensorType.Humidity -> R.drawable.ic_humidity
        SensorType.Pressure -> R.drawable.ic_pressure
        SensorType.Microphone -> R.drawable.ic_microphone
        SensorType.Unknown -> R.drawable.ic_st_placeholder
    }

    private val SensorType.nameResource:Int
    get() = when(this){
        SensorType.Accelerometer -> R.string.subSensor_type_acc
        SensorType.Magnetometer -> R.string.subSensor_type_mag
        SensorType.Gyroscope -> R.string.subSensor_type_gyro
        SensorType.Temperature -> R.string.subSensor_type_temp
        SensorType.Humidity -> R.string.subSensor_type_hum
        SensorType.Pressure -> R.string.subSensor_type_press
        SensorType.Microphone -> R.string.subSensor_type_mic
        SensorType.Unknown -> R.string.subSensor_type_unknown
    }

}