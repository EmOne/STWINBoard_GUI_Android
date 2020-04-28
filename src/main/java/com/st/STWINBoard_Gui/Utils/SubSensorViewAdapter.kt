package com.st.STWINBoard_Gui.Utils

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.st.BlueSTSDK.HSDatalog.Sensor
import com.st.BlueSTSDK.HSDatalog.SubSensorDescriptor
import com.st.BlueSTSDK.HSDatalog.SubSensorStatus
import com.st.STWINBoard_Gui.Utils.SensorViewAdapter.*
import com.st.clab.stwin.gui.R

class SubSensorViewAdapter(//Activity Context
        var mContext: Context,
        sensor: Sensor,
        private val subSensorIconClickedListener: OnSubSensorIconClickedListener?,
        private val subSensorEditTextListener: OnSubSensorEditTextChangedListener,
        private val subSensorSpinnerListener: OnSubSensorSpinnerValueSelectedListener) : RecyclerView.Adapter<SubSensorViewAdapter.ViewHolder>() {
    var sID: Int
    var mIsActive: Boolean

    //SubParam List
    var mSubSensorList: List<SubSensorDescriptor>
    var mSubStatusList: List<SubSensorStatus>

    //Layout Inflater
    var mInflater: LayoutInflater

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
        val view = mInflater.inflate(R.layout.sub_sensor_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subSensorDescriptor = mSubSensorList[position]
        val subSensorStatus = mSubStatusList[position]
        val mSSCSensorType = subSensorDescriptor.sensorType
        val subSensorTypeLabel: String
        var subSensorIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_st_placeholder)
        when (mSSCSensorType) {
            "ACC" -> {
                subSensorIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_accelerometer)
                subSensorTypeLabel = "Accelerometer"
            }
            "MAG" -> {
                subSensorIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_compass)
                subSensorTypeLabel = "Magnetometer"
            }
            "GYRO" -> {
                subSensorIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_gyroscope)
                subSensorTypeLabel = "Gyroscope"
            }
            "TEMP" -> {
                subSensorIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_temperature)
                subSensorTypeLabel = "Temperature"
            }
            "HUM" -> {
                subSensorIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_humidity)
                subSensorTypeLabel = "Humidity"
            }
            "PRESS" -> {
                subSensorIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_pressure)
                subSensorTypeLabel = "Pressure"
            }
            "MIC" -> {
                subSensorIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_microphone)
                subSensorTypeLabel = "Microphone"
            }
            else -> subSensorTypeLabel = ""
        }
        holder.mSubSensorType.text = subSensorTypeLabel
        holder.mSubSensorIcon.setImageDrawable(subSensorIcon)

        val subSensorParamsAdapter = DescriptorParamViewAdapter(
                subSensorDescriptor.getSubDescriptorParams(),
                subSensorStatus.params){ descriptorParam, newValue ->
            subSensorEditTextListener.onSubSensorEditTextValueChanged(sID,subSensorDescriptor.id,descriptorParam.name,newValue)

        }

        holder.mSubSensorListView.setAdapter(subSensorParamsAdapter);
        manageSubSensorStatus(subSensorStatus, holder.mSubSensorIcon, holder.mSubSensorRowLayoutMask)
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

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var mSubSensorRowLayout: LinearLayout
        var mSubSensorRowLayoutMask: LinearLayout
        var mSubSensorIcon: ImageView
        var mSubSensorType: TextView
        var mSubSensorListView: RecyclerView

        init {
            mSubSensorRowLayout = itemView.findViewById(R.id.subSensorRow)
            mSubSensorRowLayoutMask = itemView.findViewById(R.id.subSensorRowMask)
            mSubSensorIcon = itemView.findViewById(R.id.subSensorIcon)
            mSubSensorType = itemView.findViewById(R.id.subSensorType)
            mSubSensorListView = itemView.findViewById(R.id.subParamsListView)
            mSubSensorIcon.setOnClickListener { view: View? ->
                if (subSensorIconClickedListener != null) {
                    val position = adapterPosition
                    subSensorIconClickedListener.onSubSensorIconClicked(sID, mSubSensorList[position].id)
                    val subSensorStatus = mSubStatusList[position]
                    manageSubSensorStatus(subSensorStatus, mSubSensorIcon, mSubSensorRowLayoutMask)
                }
            }
            mSubSensorRowLayoutMask.setOnClickListener { view: View? ->
                if (subSensorIconClickedListener != null) {
                    val position = adapterPosition
                    subSensorIconClickedListener.onSubSensorIconClicked(sID, mSubSensorList[position].id)
                    mSubSensorRowLayoutMask.visibility = View.INVISIBLE
                    mSubSensorRowLayoutMask.isClickable = false
                    mSubSensorIcon.clearColorFilter()
                }
            }
        }
    }

    init {
        mInflater = LayoutInflater.from(mContext)
        sID = sensor.id
        mIsActive = sensor.sensorStatus.isActive
        mSubSensorList = sensor.sensorDescriptor.subSensorDescriptors
        mSubStatusList = sensor.sensorStatus.subSensorStatusList
    }
}