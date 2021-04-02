package updated.mysterium.vpn.ui.base

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import network.mysterium.vpn.databinding.PopUpTopUpAccountBinding
import network.mysterium.vpn.databinding.PopUpWiFiErrorBinding
import org.koin.android.ext.android.inject
import updated.mysterium.vpn.ui.top.up.amount.TopUpAmountActivity

abstract class BaseActivity : AppCompatActivity() {

    private val viewModel: BaseViewModel by inject()
    private lateinit var alertDialogBuilder: AlertDialog.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alertDialogBuilder = AlertDialog.Builder(this)
        viewModel.balanceRunningOut.observe(this, {
            balanceRunningOutPopUp()
        })
        handleInternetConnection()
    }

    fun createPopUp(popUpView: View, cancelable: Boolean): AlertDialog {
        alertDialogBuilder.apply {
            setView(popUpView)
            setCancelable(cancelable)
            create().apply {
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window?.setLayout(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
            }
        }
        val dialog = alertDialogBuilder.create()
        dialog.apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setLayout(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
        }
        return dialog
    }

    private fun handleInternetConnection() {
        if (!isInternetAvailable()) {
            wifiNetworkErrorPopUp()
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork
        val activeNetworksCapabilities = connectivityManager.getNetworkCapabilities(networkCapabilities)
        return if (activeNetworksCapabilities != null) {
            when {
                activeNetworksCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetworksCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetworksCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            false
        }
    }

    private fun balanceRunningOutPopUp() {
        val bindingPopUp = PopUpTopUpAccountBinding.inflate(layoutInflater)
        val dialog = createPopUp(bindingPopUp.root, true)
        bindingPopUp.topUpButton.setOnClickListener {
            startActivity(Intent(this, TopUpAmountActivity::class.java))
        }
        bindingPopUp.continueButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun wifiNetworkErrorPopUp() {
        val bindingPopUp = PopUpWiFiErrorBinding.inflate(layoutInflater)
        val dialog = createPopUp(bindingPopUp.root, false)
        bindingPopUp.retryButton.setOnClickListener {
            dialog.dismiss()
            handleInternetConnection()
        }
        dialog.show()
    }
}