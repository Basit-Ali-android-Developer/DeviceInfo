package com.example.deviceinfo.about

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import com.example.deviceinfo.databinding.DialogAboutBinding

class AboutDialog(context: Context) : Dialog(context) {

    private lateinit var binding: DialogAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setCancelable(true)

        binding.tvAppName.text = "Device Info"
        binding.tvVersion.text = "Version 1.0"
        binding.tvDeveloper.text = "Developed by You 😉"


        val width = (context.resources.displayMetrics.widthPixels * 0.9).toInt()
        window?.setLayout(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Close button
        binding.btnClose.setOnClickListener {
            dismiss()
        }


    }
}