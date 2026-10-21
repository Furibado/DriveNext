package ru.mtuci.drivenext.presentation.noconnection

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import ru.mtuci.drivenext.MainActivity
import ru.mtuci.drivenext.R
import ru.mtuci.drivenext.databinding.FragmentNoConnectionBinding

class NoConnectionFragment : Fragment(R.layout.fragment_no_connection) {

    private val viewModel: NoConnectionViewModel by viewModels { NoConnectionViewModel.Factory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentNoConnectionBinding.bind(view)

        binding.retryButton.setOnClickListener { viewModel.retry() }

        viewModel.destination.observe(viewLifecycleOwner) { destination ->
            (requireActivity() as MainActivity).navigateTo(destination)
        }
    }
}