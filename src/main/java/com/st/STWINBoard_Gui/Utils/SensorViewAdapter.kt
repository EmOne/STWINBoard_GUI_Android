package com.st.STWINBoard_Gui.Utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
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
typealias OnSubSensorSampleChange = (sensor:Sensor,subSensor: SubSensorDescriptor, newSampleValue:Double)->Unit

class SensorViewAdapter(//Activity Context
        private val onSubSubSensorEnableStatusChange: OnSubSensorEnableStatusChange,
        private val onSubSensorODRChange: OnSubSensorODRChange,
        private val onSubSensorFullScaleChange: OnSubSensorFullScaleChange,
        private val onSubSensorSampleChange: OnSubSensorSampleChange) :
        ListAdapter<Sensor,SensorViewAdapter.ViewHolder>(SensorDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_sensor, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val s = getItem(position)
        holder.mSensor = s
        holder.mSensorName.text = s.name
        holder.mSensorId.text = s.id.toString()

        val subSensorParamsAdapter = SubSensorViewAdapter(
                s,
                onSubSubSensorEnableStatusChange,
                onSubSensorODRChange,
                onSubSensorFullScaleChange,
                onSubSensorSampleChange)

        holder.mSubSensorListView.adapter = subSensorParamsAdapter
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var mSensorCard: CardView
        var mSensorCardMask: CardView
        var mSensorName: Switch
        var mSensorId: TextView
        var mSensorParamsLayout: LinearLayout
        var mSensorParamListView: RecyclerView
        var mSubSensorListView: RecyclerView
        var mSensor: Sensor? = null

        init {
            mSensorCard = itemView.findViewById(R.id.sensor_card)
            mSensorCardMask = itemView.findViewById(R.id.sensor_card_mask)
            mSensorName = itemView.findViewById(R.id.sensorName)
            mSensorId = itemView.findViewById(R.id.sensorId)
            mSensorParamsLayout = itemView.findViewById(R.id.sensor_param_layout)
            mSensorParamListView = itemView.findViewById(R.id.sensorParamList)
            mSubSensorListView = itemView.findViewById(R.id.subSensorList)
        }
    }

}

private class SensorDiffCallback : DiffUtil.ItemCallback<Sensor>(){
    override fun areItemsTheSame(oldItem: Sensor, newItem: Sensor): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Sensor, newItem: Sensor): Boolean {
        return oldItem == newItem
    }

}