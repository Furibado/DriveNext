package ru.mtuci.drivenext.presentation.catalog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import ru.mtuci.drivenext.R
import ru.mtuci.drivenext.databinding.ItemCarBinding
import ru.mtuci.drivenext.domain.Car
import java.text.NumberFormat
import java.util.Locale

internal class CarAdapter(scope: CoroutineScope, private val open: (Car, Boolean) -> Unit) :
    ListAdapter<Car, CarAdapter.Holder>(object : DiffUtil.ItemCallback<Car>() {
        override fun areItemsTheSame(old: Car, new: Car) = old.id == new.id
        override fun areContentsTheSame(old: Car, new: Car) = old == new
    }) {
    private val images = CarImageLoader(scope)
    private val number = NumberFormat.getNumberInstance(Locale.forLanguageTag("ru-RU")).apply {
        maximumFractionDigits = 2
    }

    inner class Holder(val binding: ItemCarBinding) : RecyclerView.ViewHolder(binding.root) {
        var imageJob: Job? = null
        fun bind(car: Car) = with(binding) {
            imageJob?.cancel()
            model.text = car.model
            brand.text = car.brand
            price.text = root.context.getString(R.string.catalog_price, number.format(car.pricePerDay))
            transmission.text = car.transmission
            val transmissionIcon = androidx.core.content.ContextCompat.getDrawable(root.context, R.drawable.catalog_transmission)
            val iconSize = (18 * root.resources.displayMetrics.density).toInt()
            transmissionIcon?.setBounds(0, 0, iconSize, iconSize)
            transmission.setCompoundDrawables(transmissionIcon, null, null, null)
            transmission.compoundDrawablePadding = (4 * root.resources.displayMetrics.density).toInt()
            fuel.text = car.fuelType
            photo.contentDescription = root.context.getString(R.string.catalog_photo, car.brand, car.model)
            imageJob = images.load(car.imageUrl, photo)
            photo.setOnClickListener {
                imageJob?.cancel()
                imageJob = images.load(car.imageUrl, photo)
            }
            book.setOnClickListener { open(car, true) }
            details.setOnClickListener { open(car, false) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemCarBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) { holder.bind(getItem(position)) }
    override fun onViewRecycled(holder: Holder) {
        holder.imageJob?.cancel()
        holder.binding.photo.setImageDrawable(null)
        super.onViewRecycled(holder)
    }
}
