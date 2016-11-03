package com.blanke.httpcachetest;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;

import static com.blanke.httpcachetest.MainActivity.TAG_CACHE_OFFLINE_AGE;
import static com.blanke.httpcachetest.MainActivity.TAG_CACHE_ONLINE_AGE;

/**
 * Created by blanke on 16-11-3.
 */

public interface ContentApi {
    //    @GET("news/latest")
    @GET("/")
    @Headers({TAG_CACHE_ONLINE_AGE + ":" + 5,//在线缓存的超时时间
            TAG_CACHE_OFFLINE_AGE + ":" + 1//在线缓存过期之后，离线缓存的超时时间
    })
    Call<String> getContent();
}
