package com.tinah.transactify.ui.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tinah.transactify.R
import com.tinah.transactify.databinding.ItemTransactionBinding
import com.tinah.transactify.domain.model.OperatorType
import com.tinah.transactify.domain.model.TransactionItem
import com.tinah.transactify.domain.model.TransactionType
import com.tinah.transactify.utils.DateUtils
import com.tinah.transactify.utils.MoneyFormatter

/** Liste des transactions, triée par le repository — l'adapter ne trie pas. */
class TransactionAdapter(
    private val onClick: (TransactionItem) -> Unit,
) : ListAdapter<TransactionItem, TransactionAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemTransactionBinding,
        private val onClick: (TransactionItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TransactionItem) {
            val context = binding.root.context

            binding.operatorText.text = item.operator.storageValue
            binding.operatorDot.backgroundTintList = ContextCompat.getColorStateList(context, operatorColorRes(item.operator))

            binding.typeText.text = item.type.displayName
            binding.typeText.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (item.type == TransactionType.RECU) R.color.profit_green else R.color.loss_red,
                ),
            )

            binding.amountText.text = MoneyFormatter.format(item.amount)
            binding.phoneText.text = item.phoneNumber
            binding.dateText.text = DateUtils.formatDateTime(item.timestamp)

            binding.bonusBadge.visibility = if (item.bonusLinked) View.VISIBLE else View.GONE

            binding.root.setOnClickListener { onClick(item) }
        }

        private fun operatorColorRes(operator: OperatorType): Int = when (operator) {
            OperatorType.ORANGE_MONEY -> R.color.orange_money
            OperatorType.AIRTEL_MONEY -> R.color.airtel_red
            OperatorType.MVOLA -> R.color.mvola_blue
        }
    }

    companion object {
        /** `internal` pour être exercé directement par [TransactionAdapterDiffTest]. */
        internal val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TransactionItem>() {
            override fun areItemsTheSame(oldItem: TransactionItem, newItem: TransactionItem) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: TransactionItem, newItem: TransactionItem) =
                oldItem == newItem
        }
    }
}
