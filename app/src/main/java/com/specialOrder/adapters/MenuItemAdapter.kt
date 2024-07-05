package com.specialOrder.adapters

import android.annotation.SuppressLint
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.specialOrder.R
import com.specialOrder.databinding.ItemCustomMenuItemsBinding
import com.specialOrder.fragment.customFields.CustomFieldsFragment
import com.specialOrder.modals.ModalData
import com.specialOrder.utils.Constants
import com.specialOrder.utils.debugSnackBar
import com.specialOrder.utils.hideView
import com.specialOrder.utils.showSnackBar
import com.specialOrder.utils.showView


class MenuItemAdapter(
    private val data: MutableList<ModalData>,
) :
    RecyclerView.Adapter<MenuItemAdapter.MyViewBinder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MenuItemAdapter.MyViewBinder {
        val binding =
            ItemCustomMenuItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewBinder(binding)
    }


    override fun onBindViewHolder(holder: MenuItemAdapter.MyViewBinder, position: Int) {
        val value = data[position]
        holder.binding.apply {
            menuItemType.text = value.name
            val gridLayoutManager = GridItemAdapter(value.list)
            val manager = StaggeredGridLayoutManager(1, GridLayoutManager.HORIZONTAL)
            menuItemValueRecycler.layoutManager = manager
            menuItemValueRecycler.adapter = gridLayoutManager
            setUpClickListener(this, position, manager)
            manageViews(value, this)
            if(value.name.equals(Constants.isCustomModalShown , true)||
                value.name.equals(Constants.isCustomModalBasket , true)
                ){
                container.setBackgroundColor(ContextCompat.getColor(root.context, R.color.darkBlue))
            }
            else{
                container.setBackgroundColor(ContextCompat.getColor(root.context, R.color.white))
            }
        }
    }

    private fun manageViews(
        value: ModalData,
        itemCustomMenuItemsBinding: ItemCustomMenuItemsBinding
    ) {
        itemCustomMenuItemsBinding.apply {
            arrowDown.isVisible = value.hasDropDown
            menuEnableButton.isChecked = value.isActive
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateSpanCount(
        itemCustomMenuItemsBinding: ItemCustomMenuItemsBinding,
        manager: StaggeredGridLayoutManager,
    ) {

        if (!isScrollingHorizontal(itemCustomMenuItemsBinding)) {
            if (manager.spanCount > 1) {
                manager.spanCount = manager.spanCount - 1
            }
            return
        }
        val count = manager.spanCount
        manager.spanCount = count + 1
        itemCustomMenuItemsBinding.menuItemValueRecycler.adapter?.notifyDataSetChanged()

    }

    private fun isScrollingHorizontal(itemCustomMenuItemsBinding: ItemCustomMenuItemsBinding): Boolean {
        return itemCustomMenuItemsBinding.menuItemValueRecycler.canScrollHorizontally(RecyclerView.LAYOUT_DIRECTION_LTR) && itemCustomMenuItemsBinding.menuItemValueRecycler.isScrollContainer

    }

    private fun setUpClickListener(
        itemCustomMenuItemsBinding: ItemCustomMenuItemsBinding,
        position: Int,
        manager: StaggeredGridLayoutManager
    ) {
        itemCustomMenuItemsBinding.apply {
            root.setOnClickListener {

                if (!data[position].hasDropDown) {
                    return@setOnClickListener
                }

                if (menuItemValueRecycler.isVisible) {

                    arrowDown.setImageDrawable(
                        ContextCompat.getDrawable(
                            root.context,
                            R.drawable.ic_up_arrow
                        )
                    )
                    menuItemValueRecycler.hideView()
                    addItemLayout.hideView()
                } else {
                    arrowDown.setImageDrawable(
                        ContextCompat.getDrawable(
                            root.context,
                            R.drawable.ic_arrow_down
                        )
                    )
                    menuItemValueRecycler.showView()
                    addItemLayout.showView()

                }

            }
            addMoreItemsButton.setOnClickListener {

                addItem(this, position, manager)
                addItems.text?.clear()
            }

            menuEnableButton.setOnClickListener {
                CustomFieldsFragment.isDataSaved = false
                data[position].isActive = !(data[position].isActive)
            }
            addItems.doAfterTextChanged {
                if(addItems.length() >= Constants.item_max_length){
                    addItems.debugSnackBar("Maximum 20 characters are allowed")
                }
            }

            addItems.setOnEditorActionListener { _, _, keyEvent ->
                if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                    addItem(this, position, manager)
                    addItems.text?.clear()
                }
                false
            }
        }
    }

    private fun addItem(
        itemCustomMenuItemsBinding: ItemCustomMenuItemsBinding,
        position: Int,
        manager: StaggeredGridLayoutManager
    ) {
        itemCustomMenuItemsBinding.apply {
            val message: String = addItems.text?.trim().toString()
            if (message.isEmpty()) {
                root.showSnackBar("Please add the valid Data")
                return
            }
            if (isContained(message, position)) {
                CustomFieldsFragment.isDataSaved = false
                data[position].list.add(message)
                menuItemValueRecycler.adapter?.notifyItemInserted(data[position].list.size - 1)
                updateSpanCount(this, manager)
            } else {
                root.showSnackBar("This value already exists")
            }
        }
    }

    private fun isContained(message: String, position: Int): Boolean {
        return !data[position].list.contains(message)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class MyViewBinder(val binding: ItemCustomMenuItemsBinding) :
        RecyclerView.ViewHolder(binding.root)
}