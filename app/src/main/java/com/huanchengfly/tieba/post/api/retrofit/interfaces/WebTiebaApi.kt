package com.huanchengfly.tieba.post.api.retrofit.interfaces

import com.huanchengfly.tieba.post.api.Header
import com.huanchengfly.tieba.post.api.models.*
import com.huanchengfly.tieba.post.api.models.web.*
import com.huanchengfly.tieba.post.api.retrofit.ApiResult
import com.huanchengfly.tieba.post.models.MyInfoBean
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import retrofit2.Call
import retrofit2.http.*


interface WebTiebaApi {
    @Headers(
        "${Header.FORCE_LOGIN}: ${Header.FORCE_LOGIN_TRUE}"
    )
    @GET
    fun follow(
        @Url url: String
    ): Call<CommonResponse>

    @Headers(
        "${Header.FORCE_LOGIN}: ${Header.FORCE_LOGIN_TRUE}",
        "${Header.REFERER}: https://tieba.baidu.com/index/tbwise/forum?source=index",
        "sec-ch-ua: \".Not/A)Brand\";v=\"8\", \"Microsoft Edge\";v=\"135\", \"Chromium\";v=\"135\"",
        "sec-ch-ua-mobile: ?1",
        "sec-ch-ua-platform: Android"
    )
    @GET("/mg/o/getForumHome")
    fun getForumHomeAsync(
        @Query("st") sortType: Int,
        @Query("pn") page: Int,
        @Query("rn") pageSize: Int,
        @Query("eqid") eqid: String,
        @Query("refer") refer: String
    ): Deferred<ApiResult<ForumHome>>

    @Headers(
        "${Header.FORCE_LOGIN}: ${Header.FORCE_LOGIN_TRUE}",
        "${Header.REFERER}: https://tieba.baidu.com/index/tbwise/mine?source=index",
        "sec-ch-ua: \".Not/A)Brand\";v=\"8\", \"Microsoft Edge\";v=\"135\", \"Chromium\";v=\"135\"",
        "sec-ch-ua-mobile: ?1",
        "sec-ch-ua-platform: Android"
    )
    @GET("/mg/o/profile")
    fun myProfileAsync(
        @Query("format") format: String,
        @Query("eqid") eqid: String,
        @Query("refer") refer: String
    ): Deferred<ApiResult<com.huanchengfly.tieba.post.api.models.web.Profile>>

    @GET("/mo/q/hotMessage/list?fr=newwise")
    fun hotMessageList(): Call<HotMessageListBean>

    @Headers(
        "${Header.ADD_WEB_COOKIE}: ${Header.ADD_WEB_COOKIE_FALSE}"
    )
    @GET("/mo/q/newmoindex?need_user=1")
    fun myInfo(
        @retrofit2.http.Header("cookie") cookie: String
    ): Call<MyInfoBean>

    @Headers(
        "${Header.ADD_WEB_COOKIE}: ${Header.ADD_WEB_COOKIE_FALSE}"
    )
    @GET("/mo/q/newmoindex?need_user=1")
    fun myInfoAsync(
        @retrofit2.http.Header("cookie") cookie: String
    ): Deferred<ApiResult<MyInfoBean>>

    @Headers(
        "${Header.ADD_WEB_COOKIE}: ${Header.ADD_WEB_COOKIE_FALSE}"
    )
    @GET("/mo/q/newmoindex?need_user=1")
    fun myInfoFlow(
        @retrofit2.http.Header("cookie") cookie: String
    ): Flow<MyInfoBean>

    @GET("/mo/q/search/forum")
    fun searchForum(
        @Query("word") keyword: String
    ): Call<SearchForumBean>

    @Headers(
        "${Header.FORCE_LOGIN}: ${Header.FORCE_LOGIN_TRUE}"
    )
    @POST("/mo/q/cooluploadpic")
    @FormUrlEncoded
    fun webUploadPic(
        @Field("pic") base64: String?,
        @Query("type") type: String = "ajax",
        @Query("r") r: String = Math.random().toString()
    ): Call<WebUploadPicBean>

    @Headers(
        "${Header.HOST}: tieba.baidu.com",
        "${Header.ORIGIN}: https://tieba.baidu.com",
        "X-Requested-With: XMLHttpRequest"
    )
    @POST("/mo/q/apubpost")
    @FormUrlEncoded
    fun webReply(
        @Query("_t") _t_url: Long = System.currentTimeMillis(),
        @Field("co") content: String,
        @Field("_t") _t_form: Long = System.currentTimeMillis(),
        @Field("tag") tag: String = "11",
        @Field("upload_img_info") imgInfo: String,
        @Field("fid") forumId: String,
        @Field("src") src: String = "1",
        @Field("word") forumName: String,
        @Field("tbs") tbs: String,
        @Field("z") threadId: String,
        @Field("lp") lp: String = "6026",
        @Field("nick_name") nickName: String,
        @Field("pid") postId: String? = null,
        @Field("lzl_id") replyPostId: String? = null,
        @Field("floor") floor: String? = null,
        @Field("_BSK") bsk: String,
        @retrofit2.http.Header(Header.REFERER) referer: String
    ): Call<WebReplyResultBean>

    @Headers(
        "${Header.HOST}: tieba.baidu.com",
        "${Header.ORIGIN}: https://tieba.baidu.com",
        "X-Requested-With: XMLHttpRequest"
    )
    @POST("/mo/q/apubpost")
    @FormUrlEncoded
    fun webReplyAsync(
        @Query("_t") _t_url: Long = System.currentTimeMillis(),
        @Field("co") content: String,
        @Field("_t") _t_form: Long = System.currentTimeMillis(),
        @Field("tag") tag: String = "11",
        @Field("upload_img_info") imgInfo: String,
        @Field("fid") forumId: String,
        @Field("src") src: String = "1",
        @Field("word") forumName: String,
        @Field("tbs") tbs: String,
        @Field("z") threadId: String,
        @Field("lp") lp: String = "6026",
        @Field("nick_name") nickName: String,
        @Field("pid") postId: String? = null,
        @Field("lzl_id") replyPostId: String? = null,
        @Field("floor") floor: String? = null,
        @Field("_BSK") bsk: String,
        @retrofit2.http.Header(Header.REFERER) referer: String
    ): Deferred<ApiResult<WebReplyResultBean>>
}