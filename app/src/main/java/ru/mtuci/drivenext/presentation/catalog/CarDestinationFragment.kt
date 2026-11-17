package ru.mtuci.drivenext.presentation.catalog

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import ru.mtuci.drivenext.R
import ru.mtuci.drivenext.databinding.FragmentCarDestinationBinding
import ru.mtuci.drivenext.domain.Car

/** В лабораторной 3 реализуется переход; содержимое экранов относится к лабораторной 4. */
class CarDestinationFragment : Fragment(R.layout.fragment_car_destination) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentCarDestinationBinding.bind(view)
        binding.title.setText(if (requireArguments().getBoolean("rental")) R.string.catalog_rental else R.string.catalog_details)
        binding.carName.text = requireArguments().getString("car_name")
        binding.back.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    companion object {
        fun create(car: Car, rental: Boolean) = CarDestinationFragment().apply {
            arguments = bundleOf("car_id" to car.id, "car_name" to "${car.brand} ${car.model}", "rental" to rental)
        }
    }
}
