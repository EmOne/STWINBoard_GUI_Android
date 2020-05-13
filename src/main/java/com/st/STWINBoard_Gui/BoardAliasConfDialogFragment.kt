package com.st.STWINBoard_Gui

import androidx.appcompat.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.st.BlueSTSDK.Features.highSpeedDataLog.FeatureHSDataLogConfig
import com.st.BlueSTSDK.Manager
import com.st.BlueSTSDK.Node
import com.st.clab.stwin.gui.R

internal class BoardAliasConfDialogFragment : DialogFragment(){

    companion object{
        private val NODE_TAG_EXTRA = BoardAliasConfDialogFragment::class.java.name + ".NodeTag"
        fun newInstance(node: Node):DialogFragment{
            return BoardAliasConfDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(NODE_TAG_EXTRA,node.tag)
                }
            }
        }
    }

    private var mBoardName: TextView? = null

    private fun buildCustomView(): View {
        val layoutInflater = LayoutInflater.from(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_board_alias_conf,null)
        view?.apply {
            mBoardName = findViewById(R.id.aliasConf_alias_value)
        }
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.apply {
            setTitle(R.string.aliasConf_title)
            setView(buildCustomView())
            setPositiveButton(R.string.confDialog_set_button){ dialog, _ ->
                setBoardAlias()
                dialog.dismiss()
            }
            setNegativeButton(R.string.confDialog_cancel_button){ dialog, _ ->
                dialog.dismiss()
            }
        }
        return dialogBuilder.create()
    }
    private val node: Node?
        get() = arguments?.getString(NODE_TAG_EXTRA)?.let {
            Manager.getSharedInstance().getNodeWithTag(it)
        }

    private fun setBoardAlias() {
        val configFeature = node?.getFeature(FeatureHSDataLogConfig::class.java)
        val name = mBoardName?.text ?: return
        configFeature?.setBoardAlias(name.toString())
    }


}