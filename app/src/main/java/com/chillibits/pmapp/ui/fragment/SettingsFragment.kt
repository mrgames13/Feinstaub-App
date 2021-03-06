/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.chillibits.pmapp.ui.fragment

import android.app.AlertDialog
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.text.util.Linkify.addLinks
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.chillibits.pmapp.model.ServerInfo
import com.chillibits.pmapp.network.ServerMessagingUtils
import com.chillibits.pmapp.network.loadServerInfo
import com.chillibits.pmapp.service.SyncJobService
import com.chillibits.pmapp.tool.Constants
import com.chillibits.pmapp.tool.StorageUtils
import com.chillibits.pmapp.ui.view.ProgressDialog
import com.mrgames13.jimdo.feinstaubapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.UnstableDefault

class SettingsFragment : PreferenceFragmentCompat() {

    // Variables as objects
    private lateinit var smu: ServerMessagingUtils

    @UnstableDefault
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref, rootKey)
        val activity = requireActivity()

        // Initialize util packages
        val su = StorageUtils(activity)
        smu = ServerMessagingUtils(activity)

        // SyncCycle
        val syncCycle = findPreference<EditTextPreference>("sync_cycle")
        syncCycle?.setOnBindEditTextListener { editText -> applyEditTextAttributes(editText, 5) }
        syncCycle?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            String.format(getString(R.string.seconds), preference.text)
        }
        syncCycle?.setOnPreferenceChangeListener { _, newValue ->
            newValue.toString().isNotEmpty() && newValue.toString().toInt() >= Constants.MIN_SYNC_CYCLE
        }

        // SyncCycleBackground
        val syncCycleBackground = findPreference<EditTextPreference>("sync_cycle_background")
        syncCycleBackground?.setOnBindEditTextListener { editText -> applyEditTextAttributes(editText, 5) }
        syncCycleBackground?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            String.format(getString(R.string.minutes), preference.text)
        }
        syncCycleBackground?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue.toString().isNotEmpty() && Integer.parseInt(newValue.toString()) >= Constants.MIN_SYNC_CYCLE_BACKGROUND) {
                // Restart Background service with new configuration -> update JobScheduler
                val backgroundSyncFrequency = Integer.parseInt(su.getString("sync_cycle_background", Constants.DEFAULT_SYNC_CYCLE_BACKGROUND.toString())) * 1000 * 60
                val component = ComponentName(activity, SyncJobService::class.java)
                val info = JobInfo.Builder(Constants.JOB_SYNC_ID, component)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(backgroundSyncFrequency.toLong())
                    .setPersisted(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) info.setRequiresBatteryNotLow(true)
                val scheduler = activity.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                Log.i(Constants.TAG, if (scheduler.schedule(info.build()) == JobScheduler.RESULT_SUCCESS) "Job re-scheduled successfully" else "Job re-schedule failed")
                true
            } else {
                false
            }
        }

        // LimitP1
        val limitP1 = findPreference<EditTextPreference>("limit_p1")
        limitP1?.setOnBindEditTextListener { editText ->
            applyEditTextAttributes(editText, 4)
            editText.hint = getString(R.string.zero_to_disable)
        }
        limitP1?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            if(preference.text == "") getString(R.string.pref_limit_disabled) else "${preference.text} µg/m³"
        }
        limitP1?.setOnPreferenceChangeListener { _, newValue ->
            newValue.toString().isNotEmpty()
        }

        // LimitP2
        val limitP2 = findPreference<EditTextPreference>("limit_p2")
        limitP2?.setOnBindEditTextListener { editText ->
            applyEditTextAttributes(editText, 4)
            editText.hint = getString(R.string.zero_to_disable)
        }
        limitP2?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            if(preference.text == "") getString(R.string.pref_limit_disabled) else "${preference.text} µg/m³"
        }
        limitP2?.setOnPreferenceChangeListener { _, newValue ->
            newValue.toString().isNotEmpty()
        }

        // LimitTemperature
        val limitTemp = findPreference<EditTextPreference>("limit_temp")
        limitTemp?.setOnBindEditTextListener { editText ->
            applyEditTextAttributes(editText, 2)
            editText.hint = getString(R.string.zero_to_disable)
        }
        limitTemp?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            if(preference.text == "") getString(R.string.pref_limit_disabled) else "${preference.text} °C"
        }
        limitTemp?.setOnPreferenceChangeListener { _, newValue ->
            newValue.toString().isNotEmpty()
        }

        // LimitHumidity
        val limitHumidity = findPreference<EditTextPreference>("limit_humidity")
        limitHumidity?.setOnBindEditTextListener { editText ->
            applyEditTextAttributes(editText, 2)
            editText.hint = getString(R.string.zero_to_disable)
        }
        limitHumidity?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            if(preference.text == "") getString(R.string.pref_limit_disabled) else "${preference.text} %"
        }
        limitHumidity?.setOnPreferenceChangeListener { _, newValue ->
            newValue.toString().isNotEmpty()
        }

        // LimitPressure
        val limitPressure = findPreference<EditTextPreference>("limit_pressure")
        limitPressure?.setOnBindEditTextListener { editText ->
            applyEditTextAttributes(editText, 6)
            editText.hint = getString(R.string.zero_to_disable)
        }
        limitPressure?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            if(preference.text == "") getString(R.string.pref_limit_disabled) else "${preference.text} hPa"
        }
        limitPressure?.setOnPreferenceChangeListener { _, newValue ->
            newValue.toString().isNotEmpty()
        }

        // NotificationBreakdownNumber
        val notificationBreakdownNumber = findPreference<EditTextPreference>("notification_breakdown_number")
        notificationBreakdownNumber?.setOnBindEditTextListener { editText ->
            applyEditTextAttributes(editText, 3)
            editText.hint = getString(R.string.zero_to_disable)
        }
        notificationBreakdownNumber?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            //String.format(getString(if(preference.text.toString().toInt() == 1) R.string.measurement else R.string.measurements), preference.text)
            resources.getQuantityString(R.plurals.measurements, preference.text.toInt(), preference.text)
        }

        // EnableMarkerClustering
        val enableMarkerClustering = findPreference<SwitchPreferenceCompat>("enable_marker_clustering")
        enableMarkerClustering?.setOnPreferenceChangeListener { _, _ ->
            restartApp(activity)
            true
        }

        // ClearSensorData
        val clearSensorData = findPreference<Preference>("clear_sensor_data")
        clearSensorData?.setOnPreferenceClickListener {
            AlertDialog.Builder(activity)
                .setTitle(R.string.clear_sensor_data_t)
                .setMessage(R.string.clear_sensor_data_m)
                .setIcon(R.drawable.delete_red)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.yes) { _, _ ->
                    val pd = ProgressDialog(activity).setMessage(R.string.please_wait_).show()
                    CoroutineScope(Dispatchers.IO).launch {
                        su.deleteAllDataDatabases()
                        su.clearSensorDataMetadata()
                        CoroutineScope(Dispatchers.Main).launch { pd.dismiss() }
                    }
                }
                .show()
            true
        }

        // ServerInfo
        val serverInfo = findPreference<Preference>("server_info")
        serverInfo?.summaryProvider = Preference.SummaryProvider<Preference> { preference ->
            if(smu.isInternetAvailable) preference.extras.getString("summary") else getString(R.string.internet_is_not_available)
        }
        serverInfo?.setOnPreferenceClickListener {
            if (smu.isInternetAvailable) {
                // Create ProgressDialog
                val pd = ProgressDialog(requireContext())
                    .setTitle(R.string.pref_server_info_t)
                    .setMessage(R.string.pref_server_info_downloading_)
                    .show()
                CoroutineScope(Dispatchers.IO).launch {
                    val result = loadServerInfo(activity)

                    val status = when(result?.serverStatus) {
                        ServerInfo.SERVER_STATUS_ONLINE, ServerInfo.SERVER_STATUS_ONLINE_WITH_CAMPAIGN -> getString(R.string.server_status_online_short)
                        ServerInfo.SERVER_STATUS_OFFLINE -> getString(R.string.server_status_offline_short)
                        ServerInfo.SERVER_STATUS_MAINTENANCE -> getString(R.string.server_status_maintenance_short)
                        ServerInfo.SERVER_STATUS_SUPPORT_ENDED -> getString(R.string.server_status_support_ended_short)
                        else -> ""
                    }

                    val clientName = String.format(activity.getString(R.string.client_name), result?.clientName)
                    val serverStatus = String.format(activity.getString(R.string.server_status), status)
                    val minAppVersion = String.format(activity.getString(R.string.min_app_version), result?.minAppVersionName)
                    val latestAppVersion = String.format(activity.getString(R.string.latest_app_version), result?.latestAppVersionName)
                    val owner = String.format(activity.getString(R.string.server_owner), result?.serverOwner)
                    // Concatenate strings and display dialog
                    val info = SpannableString(clientName+ "\n" + serverStatus + "\n" + minAppVersion + "\n" + latestAppVersion + "\n" + owner)
                    addLinks(info, Linkify.WEB_URLS)
                    CoroutineScope(Dispatchers.Main).launch {
                        pd.dismiss()
                        val d = AlertDialog.Builder(requireContext())
                            .setTitle(getString(R.string.pref_server_info_t))
                            .setMessage(info)
                            .setPositiveButton(getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
                            .show()
                        (d.findViewById<View>(android.R.id.message) as TextView).movementMethod = LinkMovementMethod.getInstance()
                    }
                }
            } else {
                Toast.makeText(activity, getString(R.string.internet_is_not_available), Toast.LENGTH_SHORT).show()
            }
            true
        }
        if(smu.isInternetAvailable) {
            CoroutineScope(Dispatchers.IO).launch {
                val result = loadServerInfo(requireActivity())
                try {
                    CoroutineScope(Dispatchers.Main).launch {
                        val summary = when(result?.serverStatus) {
                            ServerInfo.SERVER_STATUS_ONLINE -> getString(R.string.server_status_online)
                            ServerInfo.SERVER_STATUS_OFFLINE -> getString(R.string.server_status_offline)
                            ServerInfo.SERVER_STATUS_MAINTENANCE -> getString(R.string.server_status_maintenance)
                            ServerInfo.SERVER_STATUS_SUPPORT_ENDED -> getString(R.string.server_status_support_ended)
                            else -> ""
                        }
                        serverInfo?.extras?.putString("summary", summary)
                    }
                } catch (ignored: Exception) {}
            }
        }

        val openSourceLicenses = findPreference<Preference>("open_source_licenses")
        openSourceLicenses?.setOnPreferenceClickListener {
            val s = SpannableString(getString(R.string.openSourceLicense))
            addLinks(s, Linkify.ALL)

            val d = AlertDialog.Builder(activity)
                .setTitle(openSourceLicenses.title)
                .setMessage(HtmlCompat.fromHtml(s.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY))
                .setPositiveButton(getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
                .show()
            (d.findViewById<View>(android.R.id.message) as TextView).movementMethod = LinkMovementMethod.getInstance()
            false
        }

        val openSource = findPreference<Preference>("open_source")
        openSource?.setOnPreferenceClickListener {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(getString(R.string.github_url))
            startActivity(i)
            false
        }

        val appVersion = findPreference<Preference>("app_version")
        appVersion?.summaryProvider = Preference.SummaryProvider<Preference> {
            try {
                val pInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
                pInfo.versionName
            } catch (ignored: PackageManager.NameNotFoundException) { "" }
        }
        appVersion?.setOnPreferenceClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${activity.packageName}")))
            } catch (anfe: android.content.ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}")))
            }
            false
        }

        val developers = findPreference<Preference>("developers")
        developers?.setOnPreferenceClickListener {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(getString(R.string.url_homepage))
            startActivity(i)
            false
        }

        val moreApps = findPreference<Preference>("more_apps")
        moreApps?.setOnPreferenceClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_store_developer_site_market))))
            } catch (anfe: android.content.ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_store_developer_site))))
            }
            false
        }
    }

    private fun applyEditTextAttributes(editText: EditText, length: Int) {
        editText.inputType = InputType.TYPE_CLASS_NUMBER
        editText.filters += InputFilter.LengthFilter(length)
        editText.selectAll()
    }
}

fun restartApp(context: Context) {
    AlertDialog.Builder(context)
        .setTitle(R.string.app_restart_t)
        .setMessage(R.string.app_restart_m)
        .setCancelable(false)
        .setPositiveButton(R.string.ok) { _, _ ->
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)
        }
        .show()
}