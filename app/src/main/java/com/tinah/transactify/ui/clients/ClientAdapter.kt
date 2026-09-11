package com.tinah.transactify.ui.clients

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tinah.transactify.R
import com.tinah.transactify.databinding.ItemClientBinding
import com.tinah.transactify.domain.model.ClientClassification
import com.tinah.transactify.domain.model.ClientItem
import com.tinah.transactify.utils.DateUtils
import com.tinah.transactify.utils.MoneyFormatter

class ClientAdapter(
    private val onClick: (ClientItem) -> Unit,
) : ListAdapter<ClientItem, ClientAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemClientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemClientBinding,
        private val onClick: (ClientItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ClientItem) {
            val context = binding.root.context

            binding.nameText.text = item.displayName
            binding.volumeText.text = MoneyFormatter.format(item.totalVolume)
            binding.transactionCountText.text = context.resources.getQuantityString(
                R.plurals.client_transaction_count,
                item.transactionCount,
                item.transactionCount,
            )
            binding.lastInteractionText.text = if (item.lastInteraction > 0) {
                context.getString(R.string.client_last_interaction, DateUtils.formatDate(item.lastInteraction))
            } else {
                ""
            }

            binding.classificationBadge.text = classificationLabel(context, item.classification)
            binding.classificationBadge.setTextColor(
                ContextCompat.getColor(context, classificationColorRes(item.classification)),
            )

            binding.root.setOnClickListener { onClick(item) }
        }

        private fun classificationLabel(context: Context, classification: ClientClassification): String =
            when (classification) {
                ClientClassification.VIP -> context.getString(R.string.client_classification_vip)
                ClientClassification.REGULAR -> context.getString(R.string.client_classification_regular)
                ClientClassification.ONE_TIME -> context.getString(R.string.client_classification_one_time)
            }

        private fun classificationColorRes(classification: ClientClassification): Int = when (classification) {
            ClientClassification.VIP -> R.color.orange_money
            ClientClassification.REGULAR -> R.color.mvola_blue
            ClientClassification.ONE_TIME -> R.color.text_secondary
        }
    }

    companion object {
        internal val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ClientItem>() {
            override fun areItemsTheSame(oldItem: ClientItem, newItem: ClientItem) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ClientItem, newItem: ClientItem) =
                oldItem == newItem
        }
    }
}
