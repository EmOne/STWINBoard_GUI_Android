package com.st.STWINBoard_Gui.Utils;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.st.BlueSTSDK.HSDatalog.Sensor;
import com.st.clab.stwin.gui.R;

import java.util.List;


public class SensorViewAdapter extends RecyclerView.Adapter<SensorViewAdapter.ViewHolder> {

    //Sensor List
    List<Sensor> mSensorList;
    //Activity Context
    Context mContext;
    //Layout Inflater
    LayoutInflater mInflater;
    //the layout resource file for the list items
    int mResource;

    private static OnSensorSwitchClickedListener mSensorSwitchClickedListener;
    private static OnSensorSpinnerValueSelectedListener mSensorSpinnerListener;
    private static OnSensorEditTextChangedListener mSensorEditTextListener;
    private static OnSubSensorIconClickedListener mSubSensorIconListener;
    private static OnSubSensorSpinnerValueSelectedListener mSubSensorSpinnerListener;
    private static OnSubSensorEditTextChangedListener mSubSensorEditTextListener;

    public interface OnSwitchClickedListener{
        void onSensorSwitchClicked(int sensorId);
    }
    public interface OnSensorSwitchClickedListener extends OnSwitchClickedListener {}

    public interface OnSpinnerValueSelectedListener{
        /**
         * function call when a spinner value is selected by the user
         * @param paramName selected param name
         * @param value selected spinner value
         */
        void onSpinnerValueSelected(int sensorId, @NonNull String paramName, @NonNull String value);
    }
    public interface OnSensorSpinnerValueSelectedListener extends OnSpinnerValueSelectedListener{}

    public interface OnEditTextChangedListener{
        /**
         * function call when a spinner value is selected by the user
         * @param
         */
        void onEditTextValueChanged(int sensorId, @NonNull String paramName, @NonNull String value);
    }
    public interface OnSensorEditTextChangedListener extends OnEditTextChangedListener{ }

    public interface OnSubSpinnerValueSelectedListener{
        /**
         * function call when a spinner value is selected by the user
         * @param paramName selected param name
         * @param value selected spinner value
         */
        void onSpinnerValueSelected(int sensorId, Integer subSensorId, @NonNull String paramName, @NonNull String value);
    }
    public interface OnSubSensorSpinnerValueSelectedListener extends OnSubSpinnerValueSelectedListener{}

    public interface OnIconClickedListener{

        void onSubSensorIconClicked(int sensorId, @NonNull Integer subSensorId);
    }
    public interface OnSubSensorIconClickedListener extends OnIconClickedListener{}

    public interface OnSubEditTextChangedListener{
        /**
         * function call when a spinner value is selected by the user
         * @param
         */
        void onSubSensorEditTextValueChanged(int sensorId, Integer subSensorId, @NonNull String paramName, @NonNull String value);
    }
    public interface OnSubSensorEditTextChangedListener extends OnSubEditTextChangedListener{}

    public SensorViewAdapter(@NonNull Context context,
                             int resource,
                             @NonNull List<Sensor> sensorList,
                             OnSensorSwitchClickedListener sensorSwitchClickedListener,
                             OnSensorSpinnerValueSelectedListener sensorSpinnerListener,
                             OnSensorEditTextChangedListener sensorEditTextListener,
                             OnSubSensorIconClickedListener subSensorIconClickedListener,
                             OnSubSensorSpinnerValueSelectedListener subSensorSpinnerValueSelectedListener,
                             OnSubSensorEditTextChangedListener subSensorEditTextChangedListener) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mResource = resource;
        this.mSensorList = sensorList;
        mSensorSwitchClickedListener = sensorSwitchClickedListener;
        mSensorSpinnerListener = sensorSpinnerListener;
        mSensorEditTextListener = sensorEditTextListener;
        mSubSensorIconListener = subSensorIconClickedListener;
        mSubSensorSpinnerListener = subSensorSpinnerValueSelectedListener;
        mSubSensorEditTextListener = subSensorEditTextChangedListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(mResource,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Sensor s = mSensorList.get(position);
        holder.mSensor = s;
        holder.mSensorName.setText(s.getSensorDescriptor().getName());
        holder.mSensorId.setText(s.getId().toString());
        SensorParamsViewAdapter sensorParamsAdapter = new SensorParamsViewAdapter(
                mContext,
                s,
                mSensorEditTextListener,
                mSensorSpinnerListener);
        holder.mSensorParamListView.setAdapter(sensorParamsAdapter);

        SubSensorViewAdapter subSensorParamsAdapter = new SubSensorViewAdapter(
                mContext,
                s,
                mSubSensorIconListener,
                mSubSensorEditTextListener,
                mSubSensorSpinnerListener);
        holder.mSubSensorListView.setAdapter(subSensorParamsAdapter);

        manageSensorStatus(s, holder.mSensorName, holder.mSensorCardMask);
        //subSensorParamsAdapter.notifyDataSetChanged();
        //sensorParamsAdapter.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mSensorList.size();
    }

    private void manageSensorStatus(Sensor sensor, Switch sensorName, CardView layoutMask){
        if(sensor.getSensorStatus().isActive()){
            sensorName.setChecked(true);
            layoutMask.setClickable(false);
            layoutMask.setVisibility(View.INVISIBLE);
        } else {
            sensorName.setChecked(false);
            layoutMask.setClickable(true);
            layoutMask.setVisibility(View.VISIBLE);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CardView mSensorCard;
        CardView mSensorCardMask;
        Switch mSensorName;
        TextView mSensorId;
        LinearLayout mSensorParamsLayout;
        RecyclerView mSensorParamListView;
        RecyclerView mSubSensorListView;

        Sensor mSensor;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mSensorCard = itemView.findViewById(R.id.sensor_card);
            mSensorCardMask = itemView.findViewById(R.id.sensor_card_mask);
            mSensorName = itemView.findViewById(R.id.sensorName);
            mSensorId = itemView.findViewById(R.id.sensorId);
            mSensorParamsLayout = itemView.findViewById(R.id.sensor_param_layout);
            mSensorParamListView = itemView.findViewById(R.id.sensorParamList);
            mSubSensorListView = itemView.findViewById(R.id.subSensorList);

            mSensorName.setOnClickListener(view -> {
                int position = getAdapterPosition();
                mSensorSwitchClickedListener.onSensorSwitchClicked(position);
                manageSensorStatus(mSensor, mSensorName, mSensorCardMask);
            });

            mSensorCardMask.setOnClickListener(view -> {
                int position = getAdapterPosition();
                mSensorSwitchClickedListener.onSensorSwitchClicked(position);
                mSensorCardMask.setVisibility(View.INVISIBLE);
                mSensorCardMask.setClickable(false);
                mSensorName.setChecked(true);
            });
        }
    }
}
