package com.orderMate.fragment

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.orderMate.R
import com.orderMate.modals.ReferralInfo
import com.orderMate.utils.FirebaseConfigManager
import com.orderMate.utils.MyApp

/**
 * Dialog for entering referral partner information (#81)
 * 
 * Only shown to Owners who haven't already submitted a referral.
 * Saves referral info to Firebase at: merchants/{merchantId}/referrals/{referralId}/
 */
class ReferralPartnerDialog : DialogFragment() {

    private var onSaveListener: ((String) -> Unit)? = null
    private lateinit var firebaseManager: FirebaseConfigManager

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_referral_partner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        firebaseManager = FirebaseConfigManager.getInstance()
        
        val partnerNameLayout = view.findViewById<TextInputLayout>(R.id.partnerNameLayout)
        val partnerNameInput = view.findViewById<TextInputEditText>(R.id.partnerNameInput)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)
        
        btnCancel.setOnClickListener {
            dismiss()
        }
        
        btnSave.setOnClickListener {
            val partnerName = partnerNameInput.text?.toString()?.trim() ?: ""
            
            if (partnerName.isEmpty()) {
                partnerNameLayout.error = "Please enter a partner name"
                return@setOnClickListener
            }
            
            partnerNameLayout.error = null
            saveReferral(partnerName)
        }
    }

    override fun onStart() {
        super.onStart()
        // Set dialog width to 90% of screen
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun saveReferral(partnerName: String) {
        val myApp = MyApp.getInstance()
        val merchantId = myApp.getMerchantId()
        val employeeId = myApp.getEmployeeId()
        
        if (merchantId.isNullOrEmpty()) {
            Toast.makeText(context, "Error: Merchant ID not found", Toast.LENGTH_SHORT).show()
            return
        }
        
        val referral = ReferralInfo.create(
            partnerName = partnerName,
            employeeId = employeeId ?: "unknown"
        )
        
        firebaseManager.saveReferral(merchantId, referral) { success ->
            if (success) {
                onSaveListener?.invoke(partnerName)
                dismiss()
            } else {
                Toast.makeText(context, "Failed to save referral. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setOnSaveListener(listener: (String) -> Unit) {
        onSaveListener = listener
    }

    companion object {
        const val TAG = "ReferralPartnerDialog"
        
        fun newInstance(): ReferralPartnerDialog {
            return ReferralPartnerDialog()
        }
    }
}
