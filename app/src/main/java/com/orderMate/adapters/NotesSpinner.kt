package com.orderMate.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.material.textview.MaterialTextView
import com.orderMate.R
import com.orderMate.utils.isHeading


class MyCustomAdapter(context: Context, private val items: List<String> ) :
    ArrayAdapter<String>(context, R.layout.item_spinner, items) {

    private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = items[position]
        val isBold = isHeading(item)
        val view = convertView ?: inflater.inflate(
            if (isBold) R.layout.item_spinner_heading else R.layout.item_spinner,
            parent,
            false
        )

        val textView = view.findViewById<MaterialTextView>(R.id.spinnerItem)
        textView.text = item

        return view
    }



    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent)
    }


}