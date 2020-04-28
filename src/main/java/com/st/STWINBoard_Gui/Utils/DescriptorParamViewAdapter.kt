package com.st.STWINBoard_Gui.Utils

import com.st.BlueSTSDK.HSDatalog.StatusParam

import android.text.InputType
import android.view.*
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.st.BlueSTSDK.HSDatalog.DescriptorParam
import com.st.clab.stwin.gui.R

class DescriptorParamViewAdapter(
        private val mParamList: List<DescriptorParam>,
        private val mParamStatus: List<StatusParam>,
        private val mParamValuesChange: (DescriptorParam, String)->Unit) : RecyclerView.Adapter<DescriptorParamViewAdapter.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return if (mParamList[position].hasListOfValues) SELECTABLE_PARAM else EDITABLE_PARAM
    }

    override fun getItemCount(): Int {
        return mParamList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == EDITABLE_PARAM) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.stwin_param_editable, parent, false)
            ViewHolderEditable(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.stwin_param_selectable, parent, false)
            ViewHolderSelectable(view)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val type = getItemViewType(position)
        val descriptorParam = mParamList[position]

        if(type == EDITABLE_PARAM){
            (holder as ViewHolderEditable).bind(descriptorParam)
        }else{
            (holder as ViewHolderSelectable).bind(descriptorParam)
        }

    }

    open inner class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
        lateinit var descriptorParam: DescriptorParam

        open fun bind(param:DescriptorParam){
            descriptorParam = param
        }
    }

    inner class ViewHolderEditable(itemView: View) : ViewHolder(itemView) {

        private val mParamValue: TextInputEditText = itemView.findViewById(R.id.param_editable_value)
        private val mParamLayout: TextInputLayout = itemView.findViewById(R.id.param_editable_layout)
        private val mParamUnit: TextView = itemView.findViewById(R.id.param_editable_unit)

        init {
            mParamValue.setOnEditorActionListener { v: TextView, actionId: Int, event: KeyEvent? ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE, EditorInfo.IME_ACTION_NEXT, EditorInfo.IME_ACTION_PREVIOUS -> {
                        /*Log.e("TEST","-->Edit finished!");
                        Log.e("TEST","-->" + v.getText().toString());*/v.clearFocus()
                        return@setOnEditorActionListener true
                    }
                }
                false
            }
            mParamValue.onFocusChangeListener = OnFocusChangeListener { v: View, hasFocus: Boolean ->
                if (!hasFocus) {
                    // code to execute when EditText loses focus
                    //Log.e("TEST","-->Focus finished!");
                    //Log.e("TEST","-->" + );
                    val newValue = (v as EditText).text.toString()
                    mParamValuesChange(descriptorParam, newValue)
                }
            }
        }

        override fun bind(param: DescriptorParam) {
            super.bind(param)
            if (param.hasTextValue)
                mParamValue.inputType = InputType.TYPE_CLASS_TEXT
            else if (param.hasNumericValue) {
                mParamValue.inputType = InputType.TYPE_CLASS_NUMBER
                val inputChecker = CheckDoubleNumberRange(mParamLayout, R.string.hsdl_error_writeError,param.min,param.max)
                mParamValue.addTextChangedListener(inputChecker)
            }
            mParamLayout.hint = param.name
            val currentStatus = mParamStatus.find { it.name == param.name }
            if(currentStatus!=null) {
                mParamValue.setText(currentStatus.value)
            }else {
                mParamValue.setText(param.min?.toString())
            }

            mParamUnit.text = "??"
        }
    }

    inner class ViewHolderSelectable(itemView: View) : ViewHolder(itemView) {

        private val mParamName:TextView = itemView.findViewById(R.id.param_selectable_name)
        private val mParamUnit:TextView = itemView.findViewById(R.id.param_selectable_unit)
        private val mParamValues:AbsSpinner = itemView.findViewById(R.id.param_selectable_values)

        init {

            val onUserItemSelectedByUser = object : OnItemSelectedListener, View.OnTouchListener {
                private var userSelect = false

                override fun onNothingSelected(parent: AdapterView<*>?) {}

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (!userSelect) {
                        return
                    }
                    val selectedValue = mParamValues.getItemAtPosition(position).toString()
                    mParamValuesChange(descriptorParam, selectedValue)
                    userSelect = false;
                }

                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    userSelect = true;
                    return false;
                }

            }

            mParamValues.setOnTouchListener(onUserItemSelectedByUser)
            mParamValues.onItemSelectedListener = onUserItemSelectedByUser
        }

        override fun bind(param: DescriptorParam) {
            super.bind(param)
            mParamName.text = param.name
            mParamUnit.text = "??"
            val values = param.values?.toTypedArray() ?: return

            val valuesAdapter = ArrayAdapter(mParamValues.context, android.R.layout.simple_spinner_item, values)
            valuesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            mParamValues.adapter = valuesAdapter
            var currParamValueIndex = -1
            val currentParamStatus = mParamStatus.find { it.name == param.name }
            if (currentParamStatus != null) {
                currParamValueIndex = values.indexOf(currentParamStatus.value.toDouble())
            }
            //if value not found or status not found
            if (currParamValueIndex == -1) {
                currParamValueIndex = 0
                mParamValues.setBackgroundColor(mParamValues.resources.getColor(R.color.colorAccent))
            }
            mParamValues.setSelection(currParamValueIndex)
        }
    }

    companion object {
        private const val SELECTABLE_PARAM = 0
        private const val EDITABLE_PARAM = 1
    }
}
