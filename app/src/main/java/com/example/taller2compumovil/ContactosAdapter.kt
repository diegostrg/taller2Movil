package com.example.taller2compumovil

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
 
class ContactosAdapter(private val context: Context, private val contactos: List<Contacto>) :
    android.widget.BaseAdapter() {

    override fun getCount(): Int = contactos.size

    override fun getItem(position: Int): Any = contactos[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val vista: View
        val holder: ViewHolder

        if (convertView == null) {
            vista = LayoutInflater.from(context).inflate(R.layout.item_contactos, parent, false)
            holder = ViewHolder(vista)
            vista.tag = holder
        } else {
            vista = convertView
            holder = vista.tag as ViewHolder
        }

        val contacto = contactos[position]
        holder.tvNumero.text = contacto.numero
        holder.tvNombre.text = contacto.nombre

        return vista
    }

    private class ViewHolder(vista: View) {
        val tvNumero: TextView = vista.findViewById(R.id.tvNumero)
        val tvNombre: TextView = vista.findViewById(R.id.tvNombre)
    }
}