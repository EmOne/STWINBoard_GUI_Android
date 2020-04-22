package com.st.STWINBoard_Gui.Utils;

import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
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
import com.st.BlueSTSDK.HSDatalog.StatusParam;
import com.st.BlueSTSDK.HSDatalog.SubSensorDescriptor;
import com.st.BlueSTSDK.HSDatalog.SubSensorStatus;

import java.util.List;

public class SubSensorParamsViewAdapter extends RecyclerView.Adapter<SubSensorParamsViewAdapter.ViewHolder> {
    int sID;
    int ssID;
    //Param List
    SubSensorStatus mSubSensorStatus;
    SubSensorDescriptor mSubSensorDescriptor;
    List<DescriptorParam> mParamList;
    //activity context
    Context mContext;

    static final int SELECTABLE_PARAM = 0;
    static final int EDITABLE_PARAM = 1;

    private static SensorViewAdapter.OnSubSensorEditTextChangedListener mSubSensorEditTextListener;
    private static SensorViewAdapter.OnSubSensorSpinnerValueSelectedListener mSubSensorSpinnerListener;

    public SubSensorParamsViewAdapter(@NonNull Context context,
                                   int sID,
                                   int ssID,
                                   List<DescriptorParam> paramList,
                                   SubSensorStatus subSensorStatus,
                                   SubSensorDescriptor subSensorDescriptor,
                                   SensorViewAdapter.OnSubSensorEditTextChangedListener sensorEditTextListener,
                                   SensorViewAdapter.OnSubSensorSpinnerValueSelectedListener sensorSpinnerListener) {
        this.mContext = context;
        this.sID = sID;
        this.ssID = ssID;
        this.mParamList = paramList;
        //this.statusList = statusList;
        this.mSubSensorStatus = subSensorStatus;
        this.mSubSensorDescriptor = subSensorDescriptor;
        mSubSensorEditTextListener = sensorEditTextListener;
        mSubSensorSpinnerListener = sensorSpinnerListener;
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

        //TODO when FW dev_model_update is completed
        //holder.mParamUnit.setText(mSubSensorDescriptor.getUnit());

        if(type == EDITABLE_PARAM){
            if(descriptorParam.getDataType() != null){
                if(descriptorParam.getDataType().contains("string")) {
                    ((EditText)holder.mParamValue).setInputType(InputType.TYPE_CLASS_TEXT);
                } else {
                    if(descriptorParam.getDataType().contains("int") || descriptorParam.getDataType().contains("float"))
                        ((EditText)holder.mParamValue).setInputType(InputType.TYPE_CLASS_NUMBER);
                    ((EditText)holder.mParamValue).setFilters(new InputFilter[]{ new SensorParamsViewAdapter.InputFilterMinMax(descriptorParam.getMin(), descriptorParam.getMax())});
                }

                String paramStatusValue = "";
                for(StatusParam sp : mSubSensorStatus.getParams()){
                    if(sp.getName().equals(descriptorParam.getName())){
                        if(Double.valueOf(sp.getValue()) >= descriptorParam.getMin() &&
                                Double.valueOf(sp.getValue()) <= descriptorParam.getMax()){
                            paramStatusValue = sp.getValue();
                        } else {
                            paramStatusValue = String.valueOf(descriptorParam.getMin());
                            ((EditText)holder.mParamValue).setTextColor(mContext.getResources().getColor(R.color.colorAccent));
                        }
                    }
                }
                ((EditText)holder.mParamValue).setText(paramStatusValue);
            }
        }else{
            ArrayAdapter<Double> valuesAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item, descriptorParam.getValues());
            valuesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            ((Spinner)holder.mParamValue).setAdapter(valuesAdapter);
            String currParamValue = "";
            for(StatusParam sp : mSubSensorStatus.getParams()){
                if(sp.getName().equals(descriptorParam.getName())){
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

    static class ViewHolder extends RecyclerView.ViewHolder{
        LinearLayout mParamLayout;
        TextView mParamName;
        TextView mParamUnit;
        View mParamValue;

        DescriptorParam descriptorParam;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mParamLayout = itemView.findViewById(R.id.paramLayout);
            mParamName = itemView.findViewById(R.id.paramName);
            //mParamUnit = itemView.findViewById(R.id.paramUnit);

            itemView.setOnTouchListener((v, event) -> {
                mParamValue.clearFocus();
                itemView.performClick();
                return true;
            });
        }
    }

    public class ViewHolderEditable extends ViewHolder {

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
                    mSubSensorEditTextListener.onSubSensorEditTextValueChanged(sID, ssID, descriptorParam.getName(), ((EditText)v).getText().toString());
                }
            });
        }
    }

    public class ViewHolderSelectable extends ViewHolder {

        public ViewHolderSelectable(@NonNull View itemView) {
            super(itemView);
            mParamValue = itemView.findViewById(R.id.paramValue);
            ((Spinner) mParamValue).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    mSubSensorSpinnerListener.onSpinnerValueSelected(sID, ssID, descriptorParam.getName(), ((Spinner) mParamValue).getItemAtPosition(i).toString());
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
        }
    }
}
