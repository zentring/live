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
    var selectedPlayerIndex: Int = -1
    var showData: GraphicViewData? = null
    var placeList: MutableList<Place> = ArrayList()
    var clubList: MutableList<Club> = ArrayList()

    init {
        View.inflate(context, R.layout.graphic_view, this)

        close_graphic_view_btn.setOnClickListener {
            visibility = View.INVISIBLE
            (LiveActivity.getInstance() as LiveActivity).settingBtn.visibility = View.VISIBLE
        }

        //球员按钮
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
            selectedPlayerIndex = position
            getShowData()
        })

        //落点按钮
        placeList.clear()
        var place: Place
        enumValues<PlaceType>().forEach {
            Log.e("zhaofei", it.name)
            Log.e("zhaofei", it.getPlaceString())
            Log.e("zhaofei", it.ordinal.toString())
            Log.e("zhaofei", it.placeTypeID.toString())
            place = Place()
            place.name = it.getPlaceString()
            place.id = it.placeTypeID
            placeList.add(place)
        }

        hit_btn_flowlayout.setTagCheckedMode(FlowTagLayout.FLOW_TAG_CHECKED_SINGLE)
        hit_btn_flowlayout.setTagCheckedMode(FlowTagLayout.FLOW_TAG_CHECKED_SINGLE)
        hit_btn_flowlayout.setTagShowMode(FlowTagLayout.FLOW_TAG_SHOW_SPAN)
        hit_btn_flowlayout.setSpanCount(4)
        hit_btn_flowlayout.adapter = object : BaseFlowAdapter<Place, BaseTagHolder>(
            R.layout.adapter_place_flow_item,
            placeList
        ) {
            override fun convert(tagHelper: BaseTagHolder, item: Place) {
                tagHelper.setText(R.id.place_item, item.name)
                tagHelper.getView<View>(R.id.place_item).isSelected = item.isChecked
            }
        }
        hit_btn_flowlayout.setOnTagClickListener(OnTagClickListener { parent, view, position ->
            savePlace()
        })

        //球杆按钮
        club_btn_flowlayout.setTagCheckedMode(FlowTagLayout.FLOW_TAG_CHECKED_SINGLE)
        club_btn_flowlayout.setTagCheckedMode(FlowTagLayout.FLOW_TAG_CHECKED_SINGLE)
        club_btn_flowlayout.setTagShowMode(FlowTagLayout.FLOW_TAG_SHOW_SPAN)
        club_btn_flowlayout.setSpanCount(4)
        club_btn_flowlayout.adapter = object : BaseFlowAdapter<Club, BaseTagHolder>(
            R.layout.adapter_club_flow_item,
            clubList
        ) {
            override fun convert(tagHelper: BaseTagHolder, item: Club) {
                tagHelper.setText(R.id.club_item, item.name)
                tagHelper.getView<View>(R.id.club_item).isSelected = item.isChecked
            }
        }
        club_btn_flowlayout.setOnTagClickListener(OnTagClickListener { parent, view, position ->
            saveClub()
        })
    }

    private fun getShowData() {
        var playerID = ""
        if (selectedPlayerIndex != -1) {
            playerID = playerList.get(selectedPlayerIndex).id.toString()
        }
        val queue = Volley.newRequestQueue(mContext)
        val url =
            data.API_BASE_URL + "getinitinfo.php?mt_id=${data.match}&gp_id=${data.gp_id}&pl_id=${playerID}"
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            Response.Listener<String> { response ->
                Log.e("zhaofei", response)
                showData = Gson().fromJson(response, GraphicViewData::class.java)
                if (showData == null) {
                    Toast.makeText(mContext, "字幕数据获取失败", Toast.LENGTH_SHORT).show()
                    visibility = View.INVISIBLE
                } else {
                    if (showData?.status == 200) {
                        setPlayerFlowLayoutData()
                        showHitGraphicConstraintLayout()
                        setClubFlowLayoutData()
                    } else {
                        Toast.makeText(mContext, showData?.msg, Toast.LENGTH_SHORT).show()
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
        getShowData()
    }

    private fun setPlayerFlowLayoutData() {
        playerList.clear()
        var player: Player
        if (showData?.data?.list?.size ?: 0 <= 0) {
            visibility = View.INVISIBLE
            Toast.makeText(mContext, "没有球员", Toast.LENGTH_SHORT).show()
            return
        }
        for ((index, item) in showData?.data?.list?.withIndex()!!) {
            player = Player()
            player.name = item.pl_cn_name
            player.id = item.pl_id.toString()
            if (item.pl_id == showData?.data?.pl_id) {
                player.isChecked = true
                selectedPlayerIndex = index
            }
            playerList.add(player)
        }

        player_flowlayout.adapter.replaceData(playerList)
    }

    private fun setClubFlowLayoutData() {
        clubList.clear()
        var club: Club
        val golfClubList = showData?.data?.golf_club_list
        if (golfClubList?.size ?: 0 <= 0) {
            return
        }
        for ((index, value) in golfClubList?.withIndex()!!) {
            println("the element at $index is $value")
            club = Club()
            club.id = value
            club.name = data.getClubTypeString(value)
            clubList.add(club)
        }

        club_btn_flowlayout.adapter.replaceData(clubList)
    }

    private fun showHitGraphicConstraintLayout() {
        val myData = showData?.data ?: return
        hit_graphic_cl.visibility = View.VISIBLE
        rank_graphic_cl.visibility = View.INVISIBLE
        player_name_tv.text = myData.pl_cn_name
        hole_number_tv.text = mContext.getString(R.string.current_hole, myData.mh_id)
        hole_par_tv.text = myData.mh_par_str
        hole_total_yard_tv.text = myData.mh_tee_str
        current_hit_number_tv.text = mContext.getString(R.string.current_hit_sort, myData.sc_score)
        current_hit_club_tv.text = myData.sc_golf_club?.let { data.getClubTypeString(it) }
        current_hit_to_pin_tv.text =
            mContext.getString(
                R.string.to_pin,
                Utils.deleteUselessZero(myData.prev_left_tee.toString())
            )

        score_tv.text = myData.su_to_par.toString()
        rank_tv.text = myData.su_rank.toString()
        if (myData.sc_score != 1) {
            prev_hit_number_tv.text = mContext.getString(
                R.string.current_hit_sort,
                myData.sc_score?.minus(1)
            )
            prev_hit_club_tv.text = myData.prev_sc_golf_club?.let { data.getClubTypeString(it) }
            prev_hit_yard_tv.text =
                mContext.getString(
                    R.string.hit_distance,
                    Utils.deleteUselessZero(myData.prev_tee.toString())
                )
            prev_hit_place_tv.text = mContext.getString(R.string.place,
                myData.prev_sc_place?.let { data.getPlaceTypeString(it) })
        } else {
            prev_hit_number_tv.text = ""
            prev_hit_club_tv.text = ""
            prev_hit_yard_tv.text = ""
            prev_hit_place_tv.text = ""
        }
    }

    fun setHitBtnFlowLayoutData() {
    }

    fun savePlace() {

    }

    fun saveClub() {

    }
}