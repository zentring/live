package net.zentring.live

class data {
    companion object {
        var pushurl //推流地址
                : String? = null
        var match //比赛ID
                : String? = null
        var round //比赛当前轮次
                : String? = null
        var graphic //是否开启字幕功能（0:否 1：是）
                : String? = null
        var player //当前登录账号的组别球员
                : List<Player>? = null
        var minrate //推流最小码率
                : String? = null
        var targetrate //推流最大码率
                : String? = null
        var initrate //推流初始化码率
                : String? = null

    }
}