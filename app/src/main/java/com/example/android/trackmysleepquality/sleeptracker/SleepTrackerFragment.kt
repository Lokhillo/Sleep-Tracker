package com.example.android.trackmysleepquality.sleeptracker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.android.trackmysleepquality.R
import com.example.android.trackmysleepquality.database.SleepDatabase
import com.example.android.trackmysleepquality.databinding.FragmentSleepTrackerBinding
import com.google.android.material.snackbar.Snackbar

/**
 * A fragment with buttons to record start and end times for sleep, which are saved in
 * a database.
 */
class SleepTrackerFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding: FragmentSleepTrackerBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_sleep_tracker, container, false
        )
        val application = requireNotNull(this.activity).application
        val dataSource = SleepDatabase.getInstance(application).sleepDatabaseDao
        val viewModelFactory = SleepTrackerViewModelFactory(dataSource, application)
        val viewModel =
            ViewModelProvider(this, viewModelFactory).get(SleepTrackerViewModel::class.java)
        binding.sleepTrackerViewModel = viewModel
        binding.lifecycleOwner = this
        viewModel.navigateToSleepQuality.observe(viewLifecycleOwner, Observer { sleepNight ->
            sleepNight?.let {
                this.findNavController()
                    .navigate(
                        SleepTrackerFragmentDirections.actionSleepTrackerFragmentToSleepQualityFragment(
                            sleepNight.nightId
                        )
                    )
                viewModel.navigateToSleepQualityComplete()
            }
        })
        viewModel.showSnackBarEvent.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    getString(R.string.cleared_message),
                    Snackbar.LENGTH_SHORT
                ).show()
                viewModel.showSnackBarEventComplete()
            }
        })

        val manager = GridLayoutManager(activity, 3)
        manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) =  when (position) {
                0 -> 3
                else -> 1
            }
        }
        binding.sleepList.layoutManager = manager

        val adapter = SleepNightAdapter(SleepNightListener { nightId ->
            viewModel.onSleepNightClicked(nightId)
        })
        binding.sleepList.adapter = adapter

        viewModel.nights.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.addHeaderAndSubmitList(it)
            }
        })
        viewModel.navigateToSleepDataQuality.observe(viewLifecycleOwner, Observer { nightId ->
            nightId?.let {
                this.findNavController().navigate(
                    SleepTrackerFragmentDirections
                        .actionSleepTrackerFragmentToSleepDetailFragment(nightId)
                )
                Log.e("Click",nightId.toString())
                viewModel.onSleepDataQualityNavigated()
            }
        })
        return binding.root
    }
}
