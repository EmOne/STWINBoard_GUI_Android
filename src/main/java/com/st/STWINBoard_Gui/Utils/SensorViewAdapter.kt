package com.st.STWINBoard_Gui.Utils

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
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
typealias OnSubSensorOpenMLCConf = (sensor:Sensor,subSensor: SubSensorDescriptor)->Unit

internal class SensorViewAdapter(
        private val mCallback: SensorInteractionCallback,
        private val onSubSubSensorEnableStatusChange: OnSubSensorEnableStatusChange,
        private val onSubSensorODRChange: OnSubSensorODRChange,
        private val onSubSensorFullScaleChange: OnSubSensorFullScaleChange,
        private val onSubSensorSampleChange: OnSubSensorSampleChange,
        private val onSubSensorOpenMLCConf: OnSubSensorOpenMLCConf) :
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
        holder.mSensorName.text = s.sensor.name
        holder.mSensorId.text = s.sensor.id.toString()

        if(s.isCollapsed){
            holder.mSubSensorListView.visibility = View.GONE
            holder.mSensorArrowBtn.setBackgroundResource(R.drawable.ic_expand_view)
            holder.mSubSensorPreview.visibility = View.VISIBLE
        }else{
            holder.mSubSensorListView.visibility = View.VISIBLE
            holder.mSensorArrowBtn.setBackgroundResource(R.drawable.ic_collaps_view)
            holder.mSubSensorPreview.visibility = View.GONE
        }

        val subSensorPreviewAdapter = SubSensorPreviewViewAdapter(
                s.sensor,
                onSubSubSensorEnableStatusChange
        )
        holder.mSubSensorPreview.adapter = subSensorPreviewAdapter

        val subSensorParamsAdapter = SubSensorViewAdapter(
                s.sensor,
                onSubSubSensorEnableStatusChange,
                onSubSensorODRChange,
                onSubSensorFullScaleChange,
                onSubSensorSampleChange,
                onSubSensorOpenMLCConf)

        holder.mSubSensorListView.adapter = subSensorParamsAdapter
    }

    inner class ViewHolder(mCallback: SensorInteractionCallback, itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var currentData: SensorViewData? = null

        val mSensorName: TextView = itemView.findViewById(R.id.sensorItem_nameLabel)
        val mSensorArrowBtn: ImageView = itemView.findViewById(R.id.sensorItem_expandImage)
        val mSensorId: TextView = itemView.findViewById(R.id.sensorItem_idLabel)
        val mSubSensorPreview: RecyclerView = itemView.findViewById(R.id.sensorItem_subSensorPreview)
        val mSubSensorListView: RecyclerView = itemView.findViewById(R.id.sensorItem_subSensorList)

        init {

            itemView.findViewById<View>(R.id.sensorItem_headerLayout).setOnClickListener {
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

