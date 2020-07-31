package net.zentring.live

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.RequestQueue
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
import net.zentring.live.CustomLoadingView.ShapeLoadingDialog
import net.zentring.live.adapter.RankAdapter

open class GraphicView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    val queue: RequestQueue
    var mContext: Context = context
    var playerList: MutableList<Player> = ArrayList()
    var selectedPlayerIndex: Int = -1
    var hitGraphicData: GraphicViewData? = null
    var placeList: MutableList<Place> = ArrayList()
    var clubList: MutableList<Club> = ArrayList()
    var nowPreviewGraphicType = 1 // 1击球字幕 2排名字幕
    var rankList: MutableList<RankReponse.MyData.RankInfo> = ArrayList()
    var selectedClubID = -1
    var mLoadingDialog: ShapeLoadingDialog

    init {
        View.inflate(context, R.layout.graphic_view, this)

        queue = Volley.newRequestQueue(mContext)

        mLoadingDialog = ShapeLoadingDialog.Builder(context)
            .build()
        mLoadingDialog.setCancelable(false)
        mLoadingDialog.setCanceledOnTouchOutside(false)

        close_graphic_view_btn.setOnClickListener {
            visibility = View.INVISIBLE
            (LiveActivity.getInstance() as LiveActivity).settingBtn.visibility = View.VISIBLE
        }

        //球员按钮
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
            getHitGraphicData()
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
        hit_btn_flowlayout.setTagShowMode(FlowTagLayout.FLOW_TAG_SHOW_FREE)
//        hit_btn_flowlayout.setSpanCount(4)
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
            placeList[position].id?.let { savePlace(it) }
        })

        //球杆按钮
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
            if (clubList.size > 0) {
                clubList[position].id?.let { saveClub(it) }
            }
        })

        graphic_type_btn.setOnClickListener {
            if (nowPreviewGraphicType == 1) {
                getRank()
            } else {
                setPreviewHitGraphic()
            }
        }

        //排名字幕预览
        rank_recyclerview.layoutManager = LinearLayoutManager(mContext)
        rank_recyclerview.adapter = RankAdapter(mContext, rankList)

        //撤销
        undo_score_btn.setOnClickListener {
            undo()
        }

        //上字幕
        up_graphic_btn.setOnClickListener {
            upGraphic()
        }

        getHitGraphicData()
    }

    fun show() {
        visibility = View.VISIBLE
    }

    private fun setPreviewHitGraphic() {
        nowPreviewGraphicType = 1
        graphic_type_btn.text = mContext.getString(R.string.this_group_rank_graphic)
        hit_graphic_cl.visibility = View.VISIBLE
        rank_graphic_cl.visibility = View.GONE
    }

    private fun getHitGraphicData() {
        mLoadingDialog.show()
        var playerID = ""
        if (selectedPlayerIndex != -1) {
            playerID = playerList[selectedPlayerIndex].id.toString()
        }
        val url =
            data.API_BASE_URL + "getinitinfo.php?mt_id=${data.match}&gp_id=${data.gp_id}&pl_id=${playerID}"
        Log.e("zhaofei", url)
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            Response.Listener<String> { response ->
                Log.e("zhaofei", response)
                hitGraphicData = Gson().fromJson(response, GraphicViewData::class.java)
                if (hitGraphicData == null) {
                    Toast.makeText(mContext, "击球字幕数据获取失败", Toast.LENGTH_SHORT).show()
                    visibility = View.INVISIBLE
                } else {
                    if (hitGraphicData?.status == 200) {
                        setPlayerFlowLayoutData()
                        showHitGraphicConstraintLayout()
                        selectedClubID = -1
                        setClubFlowLayoutData()
                    } else {
                        Toast.makeText(mContext, hitGraphicData?.msg, Toast.LENGTH_SHORT).show()
                        visibility = View.INVISIBLE
                    }
                }
                mLoadingDialog.dismiss()
            },
            Response.ErrorListener {
                mLoadingDialog.dismiss()
                Toast.makeText(mContext, "击球字幕数据获取失败", Toast.LENGTH_SHORT).show()
                visibility = View.INVISIBLE
                it.printStackTrace()
            }
        )
        queue.add(stringRequest)
    }

    private fun setPlayerFlowLayoutData() {
        playerList.clear()
        var player: Player
        if (hitGraphicData?.data?.list?.size ?: 0 <= 0) {
            visibility = View.INVISIBLE
            Toast.makeText(mContext, "没有球员", Toast.LENGTH_SHORT).show()
            return
        }
        for ((index, item) in hitGraphicData?.data?.list?.withIndex()!!) {
            player = Player()
            player.name = item.pl_cn_name
            player.id = item.pl_id.toString()
            if (item.pl_id == hitGraphicData?.data?.pl_id) {
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
        val golfClubList = hitGraphicData?.data?.golf_club_list
        if (golfClubList?.size ?: 0 <= 0) {
            return
        }
        for ((index, value) in golfClubList?.withIndex()!!) {
            println("the element at $index is $value")
            club = Club()
            club.id = value
            club.name = data.getClubTypeString(value)
            club.isCheck = value == selectedClubID
            clubList.add(club)
        }

        club_btn_flowlayout.adapter.replaceData(clubList)
    }

    private fun showHitGraphicConstraintLayout() {
        val myData = hitGraphicData?.data ?: return
        setPreviewHitGraphic()
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

    private fun savePlace(placeID: Int) {
        if (selectedPlayerIndex == -1) {
            Toast.makeText(mContext, "请先选择球员", Toast.LENGTH_SHORT).show()
        } else {
            mLoadingDialog.show()
            val playerID = playerList[selectedPlayerIndex].id
            val holeID = hitGraphicData?.data?.mh_id
            val hitNumber = hitGraphicData?.data?.sc_score
            val lon: Double = 100.0
            val lat: Double = 100.0
            val url =
                data.API_BASE_URL + "setplace.php?mt_id=${data.match}&pl_id=${playerID}&sc_place=${placeID}" +
                        "&rd_id=${data.round}&ho_id=${holeID}&sc_score=${hitNumber}&lon=${lon}&lat=${lat}&user=${data.loginUser}"
            val stringRequest = StringRequest(
                Request.Method.GET, url,
                Response.Listener<String> { response ->
                    Log.e("zhaofei", response)
                    hitGraphicData = Gson().fromJson(response, GraphicViewData::class.java)
                    if (hitGraphicData == null) {
                        Toast.makeText(mContext, "击球字幕数据获取失败", Toast.LENGTH_SHORT).show()
                    } else {
                        if (hitGraphicData?.status == 200) {
                            setPlayerFlowLayoutData()
                            showHitGraphicConstraintLayout()
                            setClubFlowLayoutData()
                        } else {
                            Toast.makeText(mContext, hitGraphicData?.msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                    mLoadingDialog.dismiss()
                },
                Response.ErrorListener {
                    mLoadingDialog.dismiss()
                    Toast.makeText(mContext, "击球字幕数据获取失败", Toast.LENGTH_SHORT).show()
                    it.printStackTrace()
                }
            )
            queue.add(stringRequest)
        }
    }

    private fun saveClub(clubID: Int) {
        if (selectedPlayerIndex == -1) {
            Toast.makeText(mContext, "请先选择球员", Toast.LENGTH_SHORT).show()
        } else {
            mLoadingDialog.show()
            val playerID = playerList[selectedPlayerIndex].id
            val holeID = hitGraphicData?.data?.mh_id
            val hitNumber = hitGraphicData?.data?.sc_score
            val url =
                data.API_BASE_URL + "setgolfclub.php?mt_id=${data.match}&pl_id=${playerID}&sc_golf_club=${clubID}&rd_id=${data.round}" +
                        "&ho_id=${holeID}&sc_score=${hitNumber}&user=${data.loginUser}"
            val stringRequest = StringRequest(
                Request.Method.GET, url,
                Response.Listener<String> { response ->
                    Log.e("zhaofei", response)
                    hitGraphicData = Gson().fromJson(response, GraphicViewData::class.java)
                    if (hitGraphicData == null) {
                        Toast.makeText(mContext, "击球字幕数据获取失败", Toast.LENGTH_SHORT).show()
                    } else {
                        if (hitGraphicData?.status == 200) {
                            setPlayerFlowLayoutData()
                            showHitGraphicConstraintLayout()
                            selectedClubID = clubID
                            setClubFlowLayoutData()
                        } else {
                            Toast.makeText(mContext, hitGraphicData?.msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                    mLoadingDialog.dismiss()
                },
                Response.ErrorListener {
                    mLoadingDialog.dismiss()
                    Toast.makeText(mContext, "击球字幕数据获取失败", Toast.LENGTH_SHORT).show()
                    it.printStackTrace()
                }
            )
            queue.add(stringRequest)
        }
    }

    private fun getRank() {
        mLoadingDialog.show()
        val url =
            data.API_BASE_URL + "getrank.php?mt_id=${data.match}&gp_id=${data.gp_id}"
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            Response.Listener<String> { response ->
                Log.e("zhaofei", response)
                val rankReponse = Gson().fromJson(response, RankReponse::class.java)
                if (rankReponse == null) {
                    Toast.makeText(mContext, "排名字幕数据获取失败", Toast.LENGTH_SHORT).show()
                } else {
                    if (rankReponse.status == 200) {
                        if (rankReponse.data != null && rankReponse.data?.list?.size ?: 0 <= 0) {
                            Toast.makeText(mContext, "当前没有排名", Toast.LENGTH_SHORT).show()
                        } else {
                            rankReponse.data?.list?.let { showRankGraphicConstraintLayout(it) }
                        }
                    } else {
                        Toast.makeText(mContext, rankReponse.msg, Toast.LENGTH_SHORT).show()
                    }
                }
                mLoadingDialog.dismiss()
            },
            Response.ErrorListener {
                mLoadingDialog.dismiss()
                Toast.makeText(mContext, "排名字幕数据获取失败", Toast.LENGTH_SHORT).show()
                visibility = View.INVISIBLE
                it.printStackTrace()
            }
        )
        queue.add(stringRequest)
    }

    private fun showRankGraphicConstraintLayout(list: MutableList<RankReponse.MyData.RankInfo>) {
        nowPreviewGraphicType = 2
        graphic_type_btn.text = mContext.getString(R.string.to_player_graphic)

        hit_graphic_cl.visibility = View.GONE
        rank_graphic_cl.visibility = View.VISIBLE
        rankList.clear()
        rankList.addAll(list)
        rank_recyclerview.adapter?.notifyDataSetChanged()
    }

    private fun undo() {
        if (selectedPlayerIndex == -1) {
            Toast.makeText(mContext, "请先选择球员", Toast.LENGTH_SHORT).show()
        } else {
            mLoadingDialog.show()
            val playerID = playerList[selectedPlayerIndex].id
            val holeID = hitGraphicData?.data?.mh_id
            val hitNumber = hitGraphicData?.data?.sc_score
            val url =
                data.API_BASE_URL + "undo.php?mt_id=${data.match}&pl_id=${playerID}" + "&rd_id=${data.round}&ho_id=${holeID}&sc_score=${hitNumber}&user=${data.loginUser}"
            Log.e("zhaofei", url)
            val stringRequest = StringRequest(
                Request.Method.GET, url,
                Response.Listener<String> { response ->
                    Log.e("zhaofei", response)
                    hitGraphicData = Gson().fromJson(response, GraphicViewData::class.java)
                    if (hitGraphicData == null) {
                        Toast.makeText(mContext, "击球字幕数据获取失败", Toast.LENGTH_SHORT).show()
                    } else {
                        if (hitGraphicData?.status == 200) {
                            setPlayerFlowLayoutData()
                            showHitGraphicConstraintLayout()
                            setClubFlowLayoutData()
                        } else {
                            Toast.makeText(mContext, hitGraphicData?.msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                    mLoadingDialog.dismiss()
                },
                Response.ErrorListener {
                    mLoadingDialog.dismiss()
                    Toast.makeText(mContext, "击球字幕数据获取失败", Toast.LENGTH_SHORT).show()
                    it.printStackTrace()
                }
            )
            queue.add(stringRequest)
        }
    }

    private fun upGraphic() {
        mLoadingDialog.show()
        var url: String = ""
        var code = ""
        if (nowPreviewGraphicType == 1) {
            val holeID = hitGraphicData?.data?.mh_id
            val hitNumber = hitGraphicData?.data?.sc_score
            val playerName = hitGraphicData?.data?.pl_cn_name
            val score = hitGraphicData?.data?.su_to_par
            val rank = hitGraphicData?.data?.su_rank
            val holePar = hitGraphicData?.data?.mh_par
            val totalYard = hitGraphicData?.data?.mh_tee
            val prevTee = hitGraphicData?.data?.prev_tee
            val prevLeftTee = hitGraphicData?.data?.prev_left_tee
            val prevScPlace = hitGraphicData?.data?.prev_sc_place
            val prevScGolfClub = hitGraphicData?.data?.prev_sc_golf_club
            code = "info"
            url = data.API_BASE_URL + "live_png.php?code=${code}&pl_cn_name=${playerName}&sc_to_par=${score}&sc_rank=${rank}" +
                    "&sc_score=${hitNumber}&ho_id=${holeID}&mh_par=${holePar}&mh_tee=${totalYard}&prev_tee=${prevTee}&prev_left_tee=${prevLeftTee}" +
                    "&prev_sc_place=${prevScPlace}&prev_sc_golf_club=${prevScGolfClub}"
        } else {
            code = "rank"
            val rankJsonString = Gson().toJson(rankList)
            url = data.API_BASE_URL + "live_png.php?code=${code}&list=${rankJsonString}"
        }
        Log.e("zhaofei", url)
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            Response.Listener<String> { response ->
                Log.e("zhaofei", response)
                val graphicVo = Gson().fromJson(response, GraphicVo::class.java)
                if (graphicVo == null) {
                    Toast.makeText(mContext, "生成击球字幕失败", Toast.LENGTH_SHORT).show()
                } else {
                    if (!TextUtils.isEmpty(graphicVo.url)) {
                        if (::graphicListener.isInitialized) {
                            if (nowPreviewGraphicType == 1) {
                                graphicListener(GraphicType.HIT, graphicVo.url, graphicVo.time)
                            } else {
                                graphicListener(GraphicType.RANK, graphicVo.url, graphicVo.time)
                            }
                        }
                    } else {
                        Toast.makeText(mContext, "生成击球字幕失败", Toast.LENGTH_SHORT).show()
                    }
                }
                mLoadingDialog.dismiss()
            },
            Response.ErrorListener {
                mLoadingDialog.dismiss()
                Toast.makeText(mContext, "生成击球字幕失败", Toast.LENGTH_SHORT).show()
                it.printStackTrace()
            }
        )
        queue.add(stringRequest)
    }

    private lateinit var graphicListener: (GraphicType, String, Long) -> Unit
    fun setGraphicListener(listener: (GraphicType, String, Long) -> Unit) {
        this@GraphicView.graphicListener = listener
    }
}