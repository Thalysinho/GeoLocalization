package com.example.kotlinandroiduserlocationaddress

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SalaDeleteAdapter(
    private val salas: MutableList<Sala>,
    private val onDeleteClick: (Sala) -> Unit
) : RecyclerView.Adapter<SalaDeleteAdapter.SalaVH>() {

    class SalaVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtNome: TextView = itemView.findViewById(R.id.txtNomeSalaItem)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteSala)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SalaVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sala_delete, parent, false)
        return SalaVH(view)
    }

    override fun onBindViewHolder(holder: SalaVH, position: Int) {
        val sala = salas[position]
        holder.txtNome.text = sala.nome

        holder.btnDelete.setOnClickListener {
            onDeleteClick(sala)
        }
    }

    override fun getItemCount(): Int = salas.size

    fun update(newList: List<Sala>) {
        salas.clear()
        salas.addAll(newList)
        notifyDataSetChanged()
    }
}
