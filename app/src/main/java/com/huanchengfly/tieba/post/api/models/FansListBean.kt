package com.huanchengfly.tieba.post.api.models

import com.google.gson.annotations.SerializedName
import com.huanchengfly.tieba.post.models.BaseBean

/**
 * Response of the client endpoint `/c/u/fans/page` (fans list of a user).
 */
data class FansListBean(
    @SerializedName("error_code")
    val errorCode: Int = 0,
    @SerializedName("error_msg")
    val errorMsg: String? = null,
    @SerializedName("user_list")
    var userList: List<FanUserBean> = emptyList(),
    val page: PageBean? = null,
) : BaseBean() {

    data class FanUserBean(
        val id: Long = 0,
        val name: String? = "",
        @SerializedName("name_show")
        val nameShow: String? = "",
        val portrait: String? = null,
        val intro: String? = null,
        /** 1 = 我已回关该粉丝 */
        @SerializedName("has_concerned")
        val hasConcerned: Int = 0,
    )

    data class PageBean(
        @SerializedName("page_size")
        val pageSize: Int = 0,
        @SerializedName("current_page")
        val currentPage: Int = 0,
        @SerializedName("total_page")
        val totalPage: Int = 0,
        @SerializedName("total_count")
        val totalCount: Int = 0,
        @SerializedName("has_more")
        val hasMore: Int = 0,
    )
}
