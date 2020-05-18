package com.st.STWINBoard_Gui.Utils

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.DeviceModel.*
import com.st.clab.stwin.gui.R

class SubSensorViewAdapter(
        private val sensor: Sensor,
        private val onSubSensorEnableStatusChange: OnSubSensorEnableStatusChange,
        private val onSubSensorODRChange: OnSubSensorODRChange,
        private val onSubSensorFullScaleChange: OnSubSensorFullScaleChange,
        private val onSubSensorSampleChange: OnSubSensorSampleChange) : RecyclerView.Adapter<SubSensorViewAdapter.ViewHolder>() {

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
        return ViewHolder(view)
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

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mIcon: ImageView = itemView.findViewById(R.id.subSensor_icon)
        private val mName: TextView = itemView.findViewById(R.id.subSensor_name)
        private val mParamViews:View = itemView.findViewById(R.id.subSensor_paramViews)
        private val mEnabledSwitch:Switch = itemView.findViewById(R.id.subSensor_enable)
        private val mOdrSelector:Spinner = itemView.findViewById(R.id.subSensor_odrSelector)
        private val mFsSelector:Spinner = itemView.findViewById(R.id.subSensor_fsSelector)
        private val mFsUnit:TextView = itemView.findViewById(R.id.subSensor_fsUnit)
        private val mSampleTSValue:TextInputEditText = itemView.findViewById(R.id.subSensor_sampleTSValue)
        private val mSampleTSLayout:TextInputLayout = itemView.findViewById(R.id.subSensor_sampleTSLayout)

        private var mSubSensor:SubSensorDescriptor? = null
        private var mSubSensorStatus:SubSensorStatus? = null

        private val onCheckedChangeListener  = object : CompoundButton.OnCheckedChangeListener{
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                val subSensor = mSubSensor ?: return
                displayParamViews(isChecked)
                onSubSensorEnableStatusChange(sensor,subSensor,isChecked)
            }
        }

        init {

            mOdrSelector.onUserSelectedItemListener = OnUserSelectedListener(object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) { }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedValue = parent?.getItemAtPosition(position) as Double
                    val subSensor = mSubSensor ?: return
                    onSubSensorODRChange(sensor,subSensor,selectedValue)
                }
            })

            mFsSelector.onUserSelectedItemListener = OnUserSelectedListener(object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) { }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedValue = parent?.getItemAtPosition(position) as Double
                    val subSensor = mSubSensor ?: return
                    onSubSensorFullScaleChange(sensor,subSensor,selectedValue)
                    Log.d("SubSensor","FS onItemChange $selectedValue")
                }
            })

            mSampleTSValue.setOnEditorActionListener { v: TextView, actionId: Int, event: KeyEvent? ->
                val subSensor = mSubSensor ?: return@setOnEditorActionListener false
                val newValue = v.text.toString().toIntOrNull() ?: return@setOnEditorActionListener false
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE-> {
                        onSubSensorSampleChange(sensor,subSensor,newValue)
                        v.clearFocus()
                    }
                }
                false
            }

        }

        fun bind(subSensor:SubSensorDescriptor, status: SubSensorStatus){
            mSubSensor = subSensor
            mSubSensorStatus = status

            setSensorData(subSensor.sensorType)

            setEnableState(status.isActive)
            setOdr(subSensor.odr,status.odr)
            setFullScale(subSensor.fs,status.fs)
            setFullScaleUnit(subSensor.unit)
            setSample(subSensor.samplesPerTs,status.samplesPerTs)
        }

        private fun setFullScaleUnit(unit: String?) {
            if (unit!= null)
                mFsUnit.text = mFsUnit.context.getString(R.string.subSensor_fullScaleUnitFormat, unit)
            else {
                mFsUnit.text = ""
            }
        }

        private fun setOdr(odrValues: List<Double>?, currentValue: Double?) {
            mOdrSelector.isEnabled = odrValues!=null
            if(odrValues == null) {
                mOdrSelector.visibility = View.GONE
                return
            }

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

        private fun setFullScale(fsValues: List<Double>?, currentValue: Double?) {
            mFsSelector.isEnabled = fsValues!=null
            if(fsValues == null) {
                mFsSelector.visibility = View.INVISIBLE;
                return
            }

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
            displayParamViews(newState)
            mEnabledSwitch.setOnCheckedChangeListener(onCheckedChangeListener)
        }

        private fun displayParamViews(showIt:Boolean){
            mParamViews.visibility = if(showIt){
                View.VISIBLE
            }else{
                View.GONE
            }
        }

        private fun setSample(settings:SamplesPerTs,currentValue: Int?){
            //val errorMessage = mSampleTSLayout.context.getString(R.string.subSensor_sampleErrorFromat,settings.min,settings.max)
            val inputChecker = CheckIntNumberRange(mSampleTSLayout, R.string.subSensor_sampleErrorFromat, settings.min,
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
        SensorType.Accelerometer -> R.drawable.sensor_type_accelerometer
        SensorType.Magnetometer -> R.drawable.sensor_type_compass
        SensorType.Gyroscope -> R.drawable.sensor_type_gyroscope
        SensorType.Temperature -> R.drawable.sensor_type_temperature
        SensorType.Humidity -> R.drawable.sensor_type_humidity
        SensorType.Pressure -> R.drawable.sensor_type_pressure
        SensorType.Microphone -> R.drawable.sensor_type_microphone
        SensorType.Unknown -> R.drawable.sensor_type_unknown
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