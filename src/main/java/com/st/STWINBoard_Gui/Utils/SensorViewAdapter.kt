package com.st.STWINBoard_Gui.Utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.DeviceModel.Sensor
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.DeviceModel.SubSensorDescriptor
import com.st.clab.stwin.gui.R


typealias OnSubSensorEnableStatusChange = (sensor:Sensor,subSensor: SubSensorDescriptor, newState:Boolean)->Unit
typealias OnSubSensorODRChange = (sensor:Sensor,subSensor: SubSensorDescriptor, newOdrValue:Double)->Unit
typealias OnSubSensorFullScaleChange = (sensor:Sensor,subSensor: SubSensorDescriptor, newFSValue:Double)->Unit
typealias OnSubSensorSampleChange = (sensor:Sensor,subSensor: SubSensorDescriptor, newSampleValue:Int)->Unit

internal class SensorViewAdapter(
        private val mCallback: SensorInteractionCallback,
        private val onSubSubSensorEnableStatusChange: OnSubSensorEnableStatusChange,
        private val onSubSensorODRChange: OnSubSensorODRChange,
        private val onSubSensorFullScaleChange: OnSubSensorFullScaleChange,
        private val onSubSensorSampleChange: OnSubSensorSampleChange) :
        ListAdapter<SensorViewData,SensorViewAdapter.ViewHolder>(SensorDiffCallback()) {

    interface SensorInteractionCallback {
        fun onSensorCollapsed(selected: SensorViewData)
        fun onSensorExpanded(selected: SensorViewData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_sensor, parent, false)
        return ViewHolder(mCallback,view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val s = getItem(position)
        holder.bind(s)
        holder.mSensor = s.sensor
        holder.mSensorName.text = s.sensor.name
        holder.mSensorId.text = s.sensor.id.toString()
        if(s.isCollapsed){
            holder.mSensorParamsLayout.visibility = View.GONE
            holder.mSensorArrowBtn.setBackgroundResource(R.drawable.ic_arrow_down)
        }else{
            holder.mSensorParamsLayout.visibility = View.VISIBLE
            holder.mSensorArrowBtn.setBackgroundResource(R.drawable.ic_arrow_up)
        }

        val subSensorParamsAdapter = SubSensorViewAdapter(
                s.sensor,
                onSubSubSensorEnableStatusChange,
                onSubSensorODRChange,
                onSubSensorFullScaleChange,
                onSubSensorSampleChange)

        holder.mSubSensorListView.adapter = subSensorParamsAdapter
    }

    inner class ViewHolder(mCallback: SensorInteractionCallback, itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var currentData: SensorViewData? = null

        var mSensorName: TextView = itemView.findViewById(R.id.sensorName)
        var mSensorArrowBtn: ImageView = itemView.findViewById(R.id.sensorArrowBtn)
        var mSensorId: TextView = itemView.findViewById(R.id.sensorId)
        var mSensorParamsLayout: LinearLayout = itemView.findViewById(R.id.sensor_param_layout)
        var mSubSensorListView: RecyclerView = itemView.findViewById(R.id.subSensorList)
        var mSensor: Sensor? = null

        init {

            mSensorArrowBtn.setOnClickListener {
                val sensor = currentData ?: return@setOnClickListener
                if(sensor.isCollapsed){
                    mCallback.onSensorExpanded(sensor)
                } else {
                    mCallback.onSensorCollapsed(sensor)
                }
            }
        }

        fun bind(sensor: SensorViewData) {
            currentData = sensor
        }
    }
}

private class SensorDiffCallback : DiffUtil.ItemCallback<SensorViewData>(){
    override fun areItemsTheSame(oldItem: SensorViewData, newItem: SensorViewData): Boolean {
        return oldItem.sensor.id == newItem.sensor.id
    }

    override fun areContentsTheSame(oldItem: SensorViewData, newItem: SensorViewData): Boolean {
        return oldItem == newItem
    }
}

