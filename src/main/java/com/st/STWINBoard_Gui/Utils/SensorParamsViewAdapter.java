package com.st.STWINBoard_Gui.Utils;

import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.st.BlueMS.R;
import com.st.BlueSTSDK.HSDatalog.DescriptorParam;
import com.st.BlueSTSDK.HSDatalog.Sensor;
import com.st.BlueSTSDK.HSDatalog.SensorStatus;
import com.st.BlueSTSDK.HSDatalog.StatusParam;

import java.util.List;

public class SensorParamsViewAdapter extends RecyclerView.Adapter<SensorParamsViewAdapter.ViewHolder> {

    //Param List
    int sID;
    List<DescriptorParam> mParamList;
    SensorStatus mSensorStatus;
    //Activity Context
    Context mContext;
    //Layout Inflater
    LayoutInflater mInflater;

    static final int SELECTABLE_PARAM = 0;
    static final int EDITABLE_PARAM = 1;

    private static SensorViewAdapter.OnSensorEditTextChangedListener mSensorEditTextListener;
    private static SensorViewAdapter.OnSensorSpinnerValueSelectedListener mSensorSpinnerListener;

    public SensorParamsViewAdapter(@NonNull Context context,
                                   Sensor sensor,
                                   SensorViewAdapter.OnSensorEditTextChangedListener sensorEditTextListener,
                                   SensorViewAdapter.OnSensorSpinnerValueSelectedListener sensorSpinnerListener) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.sID = sensor.getId();
        this.mParamList = sensor.getSensorDescriptor().getDescriptorParams();
        this.mSensorStatus = sensor.getSensorStatus();
        mSensorEditTextListener = sensorEditTextListener;
        mSensorSpinnerListener = sensorSpinnerListener;
    }

    private static int getPositionInList(List<Double> valueList, Double value){
        for(int i = 0; i<valueList.size(); i++){
            if(valueList.get(i).equals(value))
                return i;
        }
        return -1;
    }

    @Override
    public int getItemViewType(int position) {
        return mParamList.get(position).getValues() != null ? SELECTABLE_PARAM : EDITABLE_PARAM;
    }

    @Override
    public int getItemCount() {
        return mParamList.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == EDITABLE_PARAM) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.stwin_param_editable, parent, false);
            return new ViewHolderEditable(view);
        }
        else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.stwin_param_selectable, parent, false);
            return new ViewHolderSelectable(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int type = getItemViewType(position);
        DescriptorParam descriptorParam = mParamList.get(position);
        holder.descriptorParam = descriptorParam;
        //TODO remove (temporary fix)
        String pName = descriptorParam.getName();
        if(pName.equals("fs") || pName.equals("odr"))
            pName = pName.toUpperCase();
        holder.mParamName.setText(pName);

        if(type == EDITABLE_PARAM){
            if(descriptorParam.getDataType() != null){
                if(descriptorParam.getDataType().contains("string")) {
                    ((EditText)holder.mParamValue).setInputType(InputType.TYPE_CLASS_TEXT);
                } else {
                    if(descriptorParam.getDataType().contains("int") || descriptorParam.getDataType().contains("float"))
                        ((EditText)holder.mParamValue).setInputType(InputType.TYPE_CLASS_NUMBER);
                    ((EditText)holder.mParamValue).setFilters(new InputFilter[]{ new InputFilterMinMax(descriptorParam.getMin(), descriptorParam.getMax())});
                }

                String paramStatusValue = "";
                for(StatusParam sp : mSensorStatus.getStatusParams()){
                    if(sp.getName().equals(descriptorParam.getName())) {
                        if(Double.valueOf(sp.getValue()) >= descriptorParam.getMin() &&
                                Double.valueOf(sp.getValue()) <= descriptorParam.getMax()){
                            paramStatusValue = sp.getValue();
                        } else {
                            paramStatusValue = String.valueOf(descriptorParam.getMin());
                            ((EditText)holder.mParamValue).setTextColor(mContext.getResources().getColor(R.color.colorAccent));
                        }
                    }
                }
                //NOTE check status vs descriptor


                ((EditText)holder.mParamValue).setText(paramStatusValue);
            }
        }else{
            ArrayAdapter<Double> valuesAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item, descriptorParam.getValues());
            valuesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            ((Spinner)holder.mParamValue).setAdapter(valuesAdapter);
            String currParamValue = "";
            for(StatusParam sp : mSensorStatus.getStatusParams()){
                if(sp.getName().equals(descriptorParam.getName())) {
                    if(descriptorParam.getValues().contains(Double.valueOf(sp.getValue()))) {
                        currParamValue = sp.getValue();
                    } else {
                        currParamValue = descriptorParam.getValues().get(0).toString();
                        ((Spinner)holder.mParamValue).setBackgroundColor(mContext.getResources().getColor(R.color.colorAccent));
                    }
                }
            }
            ((Spinner)holder.mParamValue).setSelection(getPositionInList(descriptorParam.getValues(),Double.valueOf(currParamValue)));
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        LinearLayout mParamLayout;
        TextView mParamName;
        View mParamValue;

        DescriptorParam descriptorParam;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mParamLayout = itemView.findViewById(R.id.paramLayout);
            mParamName = itemView.findViewById(R.id.paramName);

            itemView.setOnTouchListener((v, event) -> {
                mParamValue.clearFocus();
                itemView.performClick();
                return true;
            });

        }
    }

    public class ViewHolderEditable extends ViewHolder{

        public ViewHolderEditable(@NonNull View itemView) {
            super(itemView);
            mParamValue = itemView.findViewById(R.id.paramValue);

            ((EditText) mParamValue).setOnEditorActionListener((v, actionId, event) -> {
                switch (actionId){
                    case EditorInfo.IME_ACTION_DONE:
                    case EditorInfo.IME_ACTION_NEXT:
                    case EditorInfo.IME_ACTION_PREVIOUS:
                        /*Log.e("TEST","-->Edit finished!");
                        Log.e("TEST","-->" + v.getText().toString());*/
                        v.clearFocus();
                        return true;
                }
                return false;
            });

            mParamValue.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    // code to execute when EditText loses focus
                    //Log.e("TEST","-->Focus finished!");
                    //Log.e("TEST","-->" + ((EditText)v).getText().toString());
                    mSensorEditTextListener.onEditTextValueChanged(sID, descriptorParam.getName(), ((EditText)v).getText().toString());
                }
            });
        }
    }

    public class ViewHolderSelectable extends ViewHolder{

        public ViewHolderSelectable(@NonNull View itemView) {
            super(itemView);
            mParamValue = itemView.findViewById(R.id.paramValue);
            ((Spinner) mParamValue).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    mSensorSpinnerListener.onSpinnerValueSelected(sID, descriptorParam.getName(), ((Spinner) mParamValue).getItemAtPosition(i).toString());
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
        }
    }

    static class InputFilterMinMax implements InputFilter {

        private double min, max;

        public InputFilterMinMax(double min, double max) {
            this.min = min;
            this.max = max;
        }

        public InputFilterMinMax(String min, String max) {
            this.min = Double.parseDouble(min);
            this.max = Double.parseDouble(max);
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            try {
                int input = Integer.parseInt(dest.toString() + source.toString());
                if (isInRange(min, max, input))
                    return null;
            } catch (NumberFormatException nfe) { }
            return "";
        }

        private boolean isInRange(double a, double b, double c) {
            return b > a ? c >= a && c <= b : c >= b && c <= a;
        }
    }
}
