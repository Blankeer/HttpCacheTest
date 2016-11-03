package com.blanke.httpcachetest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button buGetContent;
    private TextView tvHtml;
    public static final String TAG_CACHE_OFFLINE_AGE = "TAG_CACHE_OFFLINE_AGE";//添加到head的缓存超时时间
    public static final String TAG_CACHE_ONLINE_AGE = "TAG_CACHE_ONLINE_AGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buGetContent = (Button) findViewById(R.id.bu_get_content);
        tvHtml = (TextView) findViewById(R.id.tv_html);
        buGetContent.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        tvHtml.setText("loading");
        ContentApi api = getRetrofit().create(ContentApi.class);
        api.getContent().enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                tvHtml.setText(response.headers() + "\n" + response.code() + "\n"
                        + response.body() + "\n");
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                tvHtml.setText(t.toString());
            }
        });
    }

    private Retrofit getRetrofit() {
        String cachePath = getCacheDir().getAbsolutePath();
        cachePath += File.separator + "cache1";
        Cache cache = new Cache(new File(cachePath), 10 * 1024 * 1024);
        Retrofit.Builder build = new Retrofit.Builder().baseUrl("http://www.baidu.com");
//        Retrofit.Builder build = new Retrofit.Builder().baseUrl("http://news-at.zhihu.com/api/4/");
        OkHttpClient.Builder okhttpbuilder = new OkHttpClient.Builder()
                .connectTimeout(5000, TimeUnit.MILLISECONDS)
                .readTimeout(5000, TimeUnit.MILLISECONDS).cache(cache);
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        okhttpbuilder.addInterceptor(logging);
        okhttpbuilder.addInterceptor(cacheInterceptor);//无网时候调用，有网也会调用，只会调用一次
        okhttpbuilder.addNetworkInterceptor(cacheInterceptor);//有网时候调用，可能会调用多次
//        okhttpbuilder.addInterceptor(onceInterceptor);//test
//        okhttpbuilder.addNetworkInterceptor(netWorkInterceptor);//test
        build.addConverterFactory(ScalarsConverterFactory.create());
        build.client(okhttpbuilder.build());
        return build.build();
    }

    private boolean isEnableNetWork() {
        return NetWorkUtils.checkNet(MainActivity.this);
//            return false;//test
    }

    private Interceptor onceInterceptor = new Interceptor() {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Logger.d("OnceInterceptor");
            return chain.proceed(chain.request());
        }
    };
    private Interceptor netWorkInterceptor = new Interceptor() {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Logger.d("NetWorkInterceptor");
            return chain.proceed(chain.request());
        }
    };
    private Interceptor cacheInterceptor = new Interceptor() {

        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            //通过cache head设置相应的缓存设置,设置完后删除相应的head
            String offAgeStr = request.header(TAG_CACHE_OFFLINE_AGE);
            String onAgeStr = request.header(TAG_CACHE_ONLINE_AGE);
//            Logger.e("cacheInterceptor :intercept(),net=" + isEnableNetWork()
//                    + "\n\t 离线缓存时间=" + offAgeStr + ",在线缓存时间=" + onAgeStr);
            int offAge = 0, onAge = 0;
            if (!TextUtils.isEmpty(offAgeStr)) {
                try {
                    offAge = Integer.parseInt(offAgeStr);
                } catch (Exception e) {
                    offAge = 0;
                }
            }
            if (!TextUtils.isEmpty(onAgeStr)) {
                try {
                    onAge = Integer.parseInt(onAgeStr);
                } catch (Exception e) {
                    onAge = 0;
                }
            }
            if (offAge > 0 && !isEnableNetWork()) {//没网强制从缓存读取(必须得写，不然断网状态下，退出应用，或者等待一分钟后，就获取不到缓存）
                request = request.newBuilder()
                        .cacheControl(new CacheControl.Builder().onlyIfCached()
                                .maxStale(offAge, TimeUnit.SECONDS).build())
                        .build();
            }
            okhttp3.Response response = chain.proceed(request);
            if (onAge > 0 && isEnableNetWork()) {
                response = response.newBuilder()
                        .removeHeader("Pragma")
                        .removeHeader("Etag")
                        .removeHeader("Cache-Control")
                        .header("Cache-Control", "public, max-age=" + onAge)
                        .build();
            }
            return response;
        }
    };
}
