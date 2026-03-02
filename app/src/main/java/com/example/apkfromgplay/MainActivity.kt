package com.example.apkfromgplay

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apkfromgplay.data.ApkDownloadResolver.ApkDownloadCheckException
import com.example.apkfromgplay.databinding.ActivityMainBinding
import com.example.apkfromgplay.di.ServiceLocator
import com.example.apkfromgplay.install.ApkInstaller
import com.example.apkfromgplay.install.ApkInstaller.InstallResult
import com.example.apkfromgplay.ui.AppListAdapter
import com.example.apkfromgplay.ui.MainViewModel
import com.example.apkfromgplay.ui.MainViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var apkInstaller: ApkInstaller
    private val pendingDownloadIds = mutableSetOf<Long>()
    private var receiverRegistered = false

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(ServiceLocator.provideRepository())
    }

    private val appAdapter = AppListAdapter { app ->
        lifecycleScope.launch {
            runCatching {
                val downloadUrl = viewModel.resolveDownloadUrl(app.packageName)
                val downloadId = apkInstaller.enqueueDownload(app, downloadUrl)
                pendingDownloadIds += downloadId
                toast(getString(R.string.download_started))
            }.onFailure { throwable ->
                when (throwable) {
                    is ApkDownloadCheckException -> {
                        toast(getString(R.string.download_precheck_failed, throwable.failureSummary))
                    }

                    else -> toast(getString(R.string.download_failed))
                }
            }
        }
    }

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (downloadId <= 0L || !pendingDownloadIds.remove(downloadId)) return

            if (!apkInstaller.isDownloadSuccessful(downloadId)) {
                val reason = apkInstaller.getDownloadFailureReason(downloadId)
                if (reason != null) {
                    toast(getString(R.string.download_failed_with_code, reason))
                } else {
                    toast(getString(R.string.download_failed))
                }
                return
            }

            when (apkInstaller.installDownload(downloadId)) {
                InstallResult.STARTED -> Unit
                InstallResult.NEED_UNKNOWN_SOURCE_PERMISSION -> {
                    toast(getString(R.string.install_permission_needed))
                    apkInstaller.openUnknownSourceSettings()
                }
                InstallResult.FAILED -> toast(getString(R.string.download_failed))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        apkInstaller = ApkInstaller(applicationContext)

        binding.appsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = appAdapter
        }

        binding.searchButton.setOnClickListener {
            viewModel.onQueryChanged(binding.queryEditText.text?.toString().orEmpty())
            viewModel.searchApps()
        }
        binding.queryEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.onQueryChanged(binding.queryEditText.text?.toString().orEmpty())
                viewModel.searchApps()
                true
            } else {
                false
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.messageTextView.text = state.message.orEmpty()
                    appAdapter.submitList(state.apps)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerDownloadReceiver()
    }

    override fun onStop() {
        if (receiverRegistered) {
            unregisterReceiver(downloadReceiver)
            receiverRegistered = false
        }
        super.onStop()
    }

    private fun registerDownloadReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(
            this,
            downloadReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiverRegistered = true
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
