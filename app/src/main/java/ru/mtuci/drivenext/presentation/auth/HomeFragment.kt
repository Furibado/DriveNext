package ru.mtuci.drivenext.presentation.auth

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ru.mtuci.drivenext.presentation.profile.bindMainNavigation
import ru.mtuci.drivenext.R
import ru.mtuci.drivenext.databinding.FragmentHomeBinding
import ru.mtuci.drivenext.presentation.catalog.CarAdapter
import ru.mtuci.drivenext.presentation.catalog.CarDestinationFragment
import ru.mtuci.drivenext.presentation.catalog.CatalogViewModel

class HomeFragment : Fragment(R.layout.fragment_home) {
    private val model: CatalogViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentHomeBinding.bind(view)
        val adapter = CarAdapter(viewLifecycleOwner.lifecycleScope) { car, rental ->
            binding.search.clearFocus()
            (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(view.windowToken, 0)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CarDestinationFragment.create(car, rental))
                .addToBackStack(null)
                .commit()
        }
        binding.cars.layoutManager = LinearLayoutManager(requireContext())
        binding.cars.adapter = adapter
        binding.cars.itemAnimator = null
        bindMainNavigation(binding.navigation.bottomNavigation, R.id.nav_home)
        // Восстановление текста не должно повторно отправлять запрос или открывать клавиатуру.
        binding.search.setQuery(model.state.value!!.query, false)
        binding.search.clearFocus()
        val backCallback = object : OnBackPressedCallback(model.state.value!!.results) {
            override fun handleOnBackPressed() { model.home() }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                model.search(newText)
                return true
            }
            override fun onQueryTextSubmit(query: String): Boolean {
                model.search(query, immediately = true)
                binding.search.clearFocus()
                return true
            }
        })
        binding.back.setOnClickListener { model.home() }
        binding.retry.setOnClickListener { model.load() }
        model.state.observe(viewLifecycleOwner) { state ->
            backCallback.isEnabled = state.results
            binding.resultsBar.isVisible = state.results
            binding.heading.isVisible = !state.results
            binding.header.setBackgroundColor((if (state.results) "#FFFFFF" else "#F9F5FF").toColorInt())
            if (binding.search.query.toString() != state.query) binding.search.setQuery(state.query, false)
            binding.loading.isVisible = state.loading
            binding.cars.isVisible = !state.loading && !state.failed && state.cars.isNotEmpty()
            binding.messagePanel.isVisible = !state.loading && (state.failed || state.cars.isEmpty())
            binding.retry.isVisible = state.failed
            binding.message.setText(when {
                !state.failed -> R.string.catalog_empty
                state.results -> R.string.catalog_search_error
                else -> R.string.catalog_load_error
            })
            adapter.submitList(state.cars)
        }
    }
}
