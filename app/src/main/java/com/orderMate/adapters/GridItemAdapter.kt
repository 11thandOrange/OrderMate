package com.orderMate.adapters


import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.orderMate.databinding.ItemGridBinding

class GridItemAdapter(
    private val data: MutableList<String>
) :
    RecyclerView.Adapter<GridItemAdapter.MyViewBinder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewBinder {
        val binding = ItemGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewBinder(binding)
    }


    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: MyViewBinder, position: Int) {
        val item = data[position]
        holder.binding.apply {
            itemName.text = item
            cancelButton.setOnClickListener {
                data.remove(item)
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class MyViewBinder(val binding: ItemGridBinding) :
        RecyclerView.ViewHolder(binding.root)


}