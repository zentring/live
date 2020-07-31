package net.zentring.live

class RankReponse {
    var status: Int? = null
    var msg: String? = null
    var data: MyData? = null

    inner class MyData {
        var list: MutableList<RankInfo> = ArrayList()

        inner class RankInfo {
            var pl_id: Int? = null
            var pl_cn_name: String? = null
            var su_to_par: Int? = null
            var su_rank: Int? = null
        }
    }
}