package com.st.STWINBoard_Gui.Utils;

import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.st.BlueMS.R;
import com.st.BlueSTSDK.HSDatalog.Sensor;
import com.st.BlueSTSDK.HSDatalog.SubSensorDescriptor;
import com.st.BlueSTSDK.HSDatalog.SubSensorStatus;

import java.util.List;

public class SubSensorViewAdapter extends RecyclerView.Adapter<SubSensorViewAdapter.ViewHolder> {

    int sID;
    boolean mIsActive;
    //SubParam List
    List<SubSensorDescriptor> mSubSensorList;
    List<SubSensorStatus> mSubStatusList;
    //Activity Context
    Context mContext;
    //Layout Inflater
    LayoutInflater mInflater;

    private static SensorViewAdapter.OnSubSensorIconClickedListener mSubSensorIconListener;
    private static SensorViewAdapter.OnSubSensorEditTextChangedListener mSubSensorEditTextListener;
    private static SensorViewAdapter.OnSubSensorSpinnerValueSelectedListener mSubSensorSpinnerListener;

    public SubSensorViewAdapter(@NonNull Context context,
                                Sensor sensor,
                                SensorViewAdapter.OnSubSensorIconClickedListener subSensorIconClickedListener,
                                SensorViewAdapter.OnSubSensorEditTextChangedListener subSensorEditTextListener,
                                SensorViewAdapter.OnSubSensorSpinnerValueSelectedListener subSensorSpinnerListener) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.sID = sensor.getId();
        this.mIsActive = sensor.getSensorStatus().isActive();
        this.mSubSensorList = sensor.getSensorDescriptor().getSubSensorDescriptors();
        this.mSubStatusList = sensor.getSensorStatus().getSubSensorStatusList();
        mSubSensorIconListener = subSensorIconClickedListener;
        mSubSensorEditTextListener = subSensorEditTextListener;
        mSubSensorSpinnerListener = subSensorSpinnerListener;
    }

    /**
     * filter for convert a color image in a gray scale one
     */
    public static ColorMatrixColorFilter sToGrayScale;

    static{
        ColorMatrix temp = new ColorMatrix();
        temp.setSaturation(0.0f);
        sToGrayScale = new ColorMatrixColorFilter(temp);
    }//static initializer

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.sub_sensor_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubSensorDescriptor subSensorDescriptor = mSubSensorList.get(position);
        SubSensorStatus subSensorStatus = mSubStatusList.get(position);
        String mSSCSensorType = subSensorDescriptor.getSensorType();
        String subSensorTypeLabel;
        Drawable subSensorIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_st_sensor_placeholder);
        switch (mSSCSensorType) {
            case "ACC":
                subSensorIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_accelerometer);
                subSensorTypeLabel = "Accelerometer";
                break;
            case "MAG":
                subSensorIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_compass);
                subSensorTypeLabel = "Magnetometer";
                break;
            case "GYRO":
                subSensorIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_gyroscope);
                subSensorTypeLabel = "Gyroscope";
                break;
            case "TEMP":
                subSensorIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_temperature);
                subSensorTypeLabel = "Temperature";
                break;
            case "HUM":
                subSensorIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_humidity);
                subSensorTypeLabel = "Humidity";
                break;
            case "PRESS":
                subSensorIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_pressure);
                subSensorTypeLabel = "Pressure";
                break;
            case "MIC":
                subSensorIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_microphone);
                subSensorTypeLabel = "Microphone";
                break;
            default:
                subSensorTypeLabel = "";
                break;
        }

        holder.mSubSensorType.setText(subSensorTypeLabel);
        holder.mSubSensorIcon.setImageDrawable(subSensorIcon);
        SubSensorParamsViewAdapter subSensorParamsAdapter = new SubSensorParamsViewAdapter(
                mContext,
                sID,
                subSensorDescriptor.getId(),
                subSensorDescriptor.getSubDescriptorParams(),
                subSensorStatus,
                subSensorDescriptor,
                mSubSensorEditTextListener,
                mSubSensorSpinnerListener);

        holder.mSubSensorListView.setAdapter(subSensorParamsAdapter);
        manageSubSensorStatus(subSensorStatus, holder.mSubSensorIcon, holder.mSubSensorRowLayoutMask);
    }

    @Override
    public int getItemCount() {
        return mSubSensorList.size();
    }

    private void manageSubSensorStatus(SubSensorStatus subSensorStatus, AppCompatImageView subSensorIcon, LinearLayout layoutMask){
        if (subSensorStatus.isActive()) {
            subSensorIcon.clearColorFilter();
            layoutMask.setClickable(false);
            layoutMask.setVisibility(View.INVISIBLE);
        } else {
            subSensorIcon.setColorFilter(sToGrayScale);
            layoutMask.setClickable(true);
            layoutMask.setVisibility(View.VISIBLE);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        LinearLayout mSubSensorRowLayout;
        LinearLayout mSubSensorRowLayoutMask;
        AppCompatImageView mSubSensorIcon;
        TextView mSubSensorType;
        RecyclerView mSubSensorListView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mSubSensorRowLayout = itemView.findViewById(R.id.subSensorRow);
            mSubSensorRowLayoutMask = itemView.findViewById(R.id.subSensorRowMask);
            mSubSensorIcon = itemView.findViewById(R.id.subSensorIcon);
            mSubSensorType = itemView.findViewById(R.id.subSensorType);
            mSubSensorListView = itemView.findViewById(R.id.subParamsListView);

            mSubSensorIcon.setOnClickListener(view -> {
                if(mSubSensorIconListener != null){
                    int position = getAdapterPosition();
                    mSubSensorIconListener.onSubSensorIconClicked(sID,mSubSensorList.get(position).getId());
                    SubSensorStatus subSensorStatus = mSubStatusList.get(position);
                    manageSubSensorStatus(subSensorStatus,mSubSensorIcon,mSubSensorRowLayoutMask);
                }
            });

            mSubSensorRowLayoutMask.setOnClickListener(view -> {
                if(mSubSensorIconListener != null){
                    int position = getAdapterPosition();
                    mSubSensorIconListener.onSubSensorIconClicked(sID,mSubSensorList.get(position).getId());
                    mSubSensorRowLayoutMask.setVisibility(View.INVISIBLE);
                    mSubSensorRowLayoutMask.setClickable(false);
                    mSubSensorIcon.clearColorFilter();
                }
            });
        }
    }
}
