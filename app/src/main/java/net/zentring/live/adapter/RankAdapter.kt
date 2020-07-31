package net.zentring.live.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.zentring.live.R
import net.zentring.live.RankReponse

class RankAdapter(
    context: Context,
    list: MutableList<RankReponse.MyData.RankInfo>
) : RecyclerView.Adapter<RankAdapter.ViewHolder>() {

    var mList = list
    var mContext = context

    init {
        for (data in list) {
            mList.add(data)
        }
    }

    @SuppressLint("InflateParams")
    override fun onCreateViewHolder(p: ViewGroup, p1: Int): ViewHolder {
        val inflate = LayoutInflater.from(p.context).inflate(R.layout.adapter_rank_item, p, false)
        return ViewHolder(inflate)
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.rankTextView.text = mList.get(position).su_rank.toString()
        holder.playerNameTextView.text = mList.get(position).pl_cn_name
        holder.scoreTextView.text = mList.get(position).su_to_par.toString()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var rankTextView: TextView = itemView.findViewById(R.id.rank_tv)
        var playerNameTextView: TextView = itemView.findViewById(R.id.player_name_tv)
        var scoreTextView: TextView = itemView.findViewById(R.id.score_tv)
    }
}