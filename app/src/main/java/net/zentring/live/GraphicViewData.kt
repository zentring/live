package net.zentring.live

class GraphicViewData {
    var status: Int? = null
    var msg: String? = null
    var data: MyData? = null

    inner class MyData {
        var pl_id: Int? = null//选中的球员ID
        var pl_cn_name: String? = null//选中的球员名字
        var mh_id: Int? = null//洞号
        var mh_par: Int? = null//标准杆
        var mh_par_str: String? = null
        var mh_tee: Double? = null//当前洞的总码数
        var mh_tee_str: String? = null
        var su_to_par: Int? = null//当前球员成绩
        var su_rank: Int? = null//当前球员排名
        var sc_score: Int? = null//当前杆数
        var prev_tee: Double? = null//前一杆击球码数
        var prev_tee_str: String? = null
        var prev_left_tee: Double? = null//当前杆to pin值
        var prev_left_tee_str: String? = null
        var prev_sc_place: Int? = null
        var prev_sc_golf_club: Int? = null
        var sc_place: Int? = null//当前杆击球落点
        var sc_golf_club: Int? = null//当前杆球杆
        var list: MutableList<Player> = ArrayList()
        var golf_club_list: MutableList<Int> = ArrayList()

        inner class Player {
            var pl_id: Int? = null
            var pl_cn_name: String? = null
        }
    }
}