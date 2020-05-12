package com.st.STWINBoard_Gui.Utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.DeviceModel.Sensor
import com.st.clab.stwin.gui.R

class SensorViewAdapter(//Activity Context
        private val mSensorList: List<Sensor>,
        private val mSensorSwitchClickedListener: OnSensorSwitchClickedListener,
        private val mSensorEditTextListener: OnSensorEditTextChangedListener,
        private val mSubSensorIconClickedListener: OnSubSensorIconClickedListener,
        private val mSubSensorEditTextChangedListener: OnSubSensorEditTextChangedListener) : RecyclerView.Adapter<SensorViewAdapter.ViewHolder>() {


    interface OnSwitchClickedListener {
        fun onSensorSwitchClicked(sensorId: Int)
    }

    interface OnSensorSwitchClickedListener : OnSwitchClickedListener

    interface OnEditTextChangedListener {
        /**
         * function call when a spinner value is selected by the user
         * @param
         */
        fun onEditTextValueChanged(sensorId: Int, paramName: String, value: String)
    }

    interface OnSensorEditTextChangedListener : OnEditTextChangedListener

    interface SubSensorActiveStatusListener {
        fun onSubSensorChangeActivationStatus(sensorId: Int, subSensorId: Int,newActiveState:Boolean)
    }

    interface OnSubSensorIconClickedListener : SubSensorActiveStatusListener
    interface OnSubEditTextChangedListener {
        /**
         * function call when a spinner value is selected by the user
         * @param
         */
        fun onSubSensorEditTextValueChanged(sensorId: Int, subSensorId: Int?, paramName: String, value: String)
    }

    interface OnSubSensorEditTextChangedListener : OnSubEditTextChangedListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_sensor, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val s = mSensorList[position]
        holder.mSensor = s
        holder.mSensorName.text = s.name
        holder.mSensorId.text = s.id.toString()

        val subSensorParamsAdapter = SubSensorViewAdapter(
                s,
                mSubSensorIconClickedListener,
                mSubSensorEditTextChangedListener)

        holder.mSubSensorListView.adapter = subSensorParamsAdapter
        manageSensorStatus(s, holder.mSensorName, holder.mSensorCardMask)
        //subSensorParamsAdapter.notifyDataSetChanged();
        //sensorParamsAdapter.notifyDataSetChanged();
    }

    override fun getItemCount(): Int {
        return mSensorList.size
    }

    private fun manageSensorStatus(sensor: Sensor?, sensorName: Switch, layoutMask: CardView) {
        if (true) {
            sensorName.isChecked = true
            layoutMask.isClickable = false
            layoutMask.visibility = View.INVISIBLE
        } else {
            sensorName.isChecked = false
            layoutMask.isClickable = true
            layoutMask.visibility = View.VISIBLE
        }
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
            mSensorName.setOnClickListener { view: View? ->
                val position = adapterPosition
                mSensorSwitchClickedListener.onSensorSwitchClicked(position)
                manageSensorStatus(mSensor, mSensorName, mSensorCardMask)
            }
            mSensorCardMask.setOnClickListener { view: View? ->
                val position = adapterPosition
                mSensorSwitchClickedListener.onSensorSwitchClicked(position)
                mSensorCardMask.visibility = View.INVISIBLE
                mSensorCardMask.isClickable = false
                mSensorName.isChecked = true
            }
        }
    }

}