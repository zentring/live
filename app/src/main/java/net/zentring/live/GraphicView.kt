package net.zentring.live

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.volley.library.flowtag.FlowTagLayout
import com.volley.library.flowtag.FlowTagLayout.OnTagClickListener
import com.volley.library.flowtag.adapter.BaseFlowAdapter
import com.volley.library.flowtag.adapter.BaseTagHolder
import kotlinx.android.synthetic.main.activity_live.*
import kotlinx.android.synthetic.main.graphic_view.view.*

class GraphicView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    var mContext: Context = context
    var playerList: MutableList<Player> = ArrayList()
    var selectedPlayerIndex: Int = 0
    var showData: GraphicViewData? = null

    init {
        View.inflate(context, R.layout.graphic_view, this)

        close_graphic_view_btn.setOnClickListener {
            visibility = View.INVISIBLE
            (LiveActivity.getInstance() as LiveActivity).settingBtn.visibility = View.VISIBLE
        }

        player_flowlayout.setTagCheckedMode(FlowTagLayout.FLOW_TAG_CHECKED_SINGLE)
        player_flowlayout.setTagCheckedMode(FlowTagLayout.FLOW_TAG_CHECKED_SINGLE)
        player_flowlayout.setTagShowMode(FlowTagLayout.FLOW_TAG_SHOW_SPAN)
        player_flowlayout.setSpanCount(2)
        player_flowlayout.adapter = object : BaseFlowAdapter<Player, BaseTagHolder>(
            R.layout.adapter_player_flow_item,
            playerList
        ) {
            override fun convert(tagHelper: BaseTagHolder, item: Player) {
                tagHelper.setText(R.id.player_item, item.name)
                tagHelper.getView<View>(R.id.player_item).isSelected = item.isChecked
            }
        }
        player_flowlayout.setOnTagClickListener(OnTagClickListener { parent, view, position ->
            mPlayerItemClickListener?.onItemClick(position)
            getShowData(playerList.get(position).id.toString())
        })

    }

    private var mPlayerItemClickListener: MyPlayerItemClickListener? = null

    interface MyPlayerItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setPlayerItemClickListener(myItemClickListener: MyPlayerItemClickListener?) {
        mPlayerItemClickListener = myItemClickListener
    }

    private fun getShowData(playerID: String) {
        var queue = Volley.newRequestQueue(mContext)
        val url =
            data.API_BASE_URL + "getinitinfo.php?mt_id=${data.match}&gp_id=${data.gp_id}&pl_id=${playerID}"
        var stringRequest = StringRequest(
            Request.Method.GET, url,
            Response.Listener<String> { response ->
                Log.e("zhaofei",response)
                showData = Gson().fromJson(response, GraphicViewData::class.java)
                if (showData == null) {
                    Toast.makeText(mContext, "字幕数据获取失败", Toast.LENGTH_SHORT).show()
                    visibility = View.INVISIBLE
                } else {
                    if (showData!!.status == 200) {
                        setPlayerFlowLayoutData()
                    } else {
                        Toast.makeText(mContext, showData!!.msg, Toast.LENGTH_SHORT).show()
                        visibility = View.INVISIBLE
                    }
                }
            },
            Response.ErrorListener {
                Toast.makeText(mContext, "字幕数据获取失败", Toast.LENGTH_SHORT).show()
                visibility = View.INVISIBLE
                it.printStackTrace()
            }
        )
        queue.add(stringRequest)
    }

    fun show() {
        visibility = View.VISIBLE
        getShowData("")
    }

    fun setPlayerFlowLayoutData() {
        playerList.clear()
        var player: Player
        for ((index, item) in showData!!.data!!.list.withIndex()) {
            player = Player()
            player.name = item.pl_cn_name
            player.id = item.pl_id.toString()
            player.isCheck = item.pl_id == showData!!.data!!.pl_id
            playerList.add(player)
        }

        player_flowlayout.adapter.replaceData(playerList)
    }
}