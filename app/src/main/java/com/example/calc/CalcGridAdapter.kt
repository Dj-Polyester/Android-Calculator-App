package com.example.calc

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class CalcGridAdapter(
    private val ctx: Context,
    private val labels:List<String>,
): BaseAdapter() {
    private var inflater:LayoutInflater = ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var fields:Array<View?> = arrayOfNulls(labels.size)


    override fun getCount(): Int {
        return labels.size
    }

    override fun getItem(p0: Int): Any {
        return 0
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        if (fields[p0] == null){
            fields[p0] = inflater.inflate(R.layout.calc_button, null)
        }
        val textView:TextView = fields[p0]!!.findViewById<TextView>(R.id.calcButton)
        textView.text = labels[p0]
        return fields[p0]!!
    }
}