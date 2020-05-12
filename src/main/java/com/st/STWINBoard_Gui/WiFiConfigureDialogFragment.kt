package com.st.STWINBoard_Gui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.st.BlueSTSDK.Features.highSpeedDataLog.FeatureHSDataLogConfig
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.WifSettings
import com.st.BlueSTSDK.Manager
import com.st.BlueSTSDK.Node
import com.st.clab.stwin.gui.R

internal class WiFiConfigureDialogFragment : DialogFragment(){

    companion object{
        private val NODE_TAG_EXTRA = WiFiConfigureDialogFragment::class.java.name + ".NodeTag"
        fun newInstance(node: Node):DialogFragment{
            return WiFiConfigureDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(NODE_TAG_EXTRA,node.tag)
                }
            }
        }
    }

    private var mSSIDText:TextView? = null
    private var mPasswordText:TextView? = null
    private var mEnableWifi:CompoundButton? = null

    private fun buildCustomView(): View {
        val layoutInflater = LayoutInflater.from(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_wifi_conf,null)
        view?.apply {
            mEnableWifi= findViewById(R.id.wifiConf_enable_switch)
            mPasswordText = findViewById(R.id.wifiConf_password_value)
            mSSIDText = findViewById(R.id.wifiConf_ssid_value)
        }
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.apply {
            setTitle(R.string.wificonf_title)
            setView(buildCustomView())
            setPositiveButton(R.string.confDialog_set_button){ dialog, _ ->
                sendWifiSetting()
                dialog.dismiss()
            }
            setNegativeButton(R.string.confDialog_cancel_button){ dialog, _ ->
                dialog.dismiss()
            }
        }
        return dialogBuilder.create()
    }

    private val node:Node?
    get() = arguments?.getString(NODE_TAG_EXTRA)?.let {
        Manager.getSharedInstance().getNodeWithTag(it)
    }

    private fun sendWifiSetting() {
        val configFeature = node?.getFeature(FeatureHSDataLogConfig::class.java)
        val settings = WifSettings(
                enable = mEnableWifi?.isChecked ?: false,
                ssid = mSSIDText?.text,
                password = mPasswordText?.text
        )
        configFeature?.setWifiSettings(settings)
    }

}