package com.doughnut.net.apirequest;

import android.preference.PreferenceManager;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.doughnut.R;
import com.doughnut.config.AppConfig;
import com.doughnut.net.NetManager;
import com.doughnut.net.volleyext.BaseJsonRequest;
import com.doughnut.utils.TLog;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Function;


public abstract class ApiRequest<T> implements IApiRequest {
    private final static String TAG = ApiRequest.class.getSimpleName();
    private static int reqId = 0;//自动递加请求的ID，全局唯一
    private int curReq;
    private Observable<String> apiResponseObservable;

    public ApiRequest() {
        reqId++;
        curReq = reqId;
    }

    public int getReqId() {
        return curReq;
    }

    @Override
    public Map<String, String> initHeader() {
        // TODO Auto-generated method stub
        Map<String, String> header = new HashMap<String, String>();

        String SetCookie = PreferenceManager.getDefaultSharedPreferences(AppConfig.getContext()).getString("Cookie", "");
        TLog.d(TAG, "Set-Cookie = " + SetCookie);
        String cookie = initCookie();
        if (cookie != null) {
            header.put("Cookie", cookie);
        }

        String contentType = initContentType();
        if (contentType != null) {
            header.put("Content-Type", contentType);
        }
        if (iniEncoding() != null) {
            header.put("Accept-Encoding", iniEncoding());
        }
        return header;
    }

    /**
     * 执行http请求
     */
    public void execute() {
        NetManager.getInstance().setRequestTask(this);
    }

    /**
     * 提取通用接口，用于rxjava模式访问 RiverApi 类型接口
     *
     * @param shouldCache 是否缓存
     * @return
     */
    public Observable<String> getObservableObj(final boolean shouldCache) {
        if (apiResponseObservable == null) {
            Observable<String> strObservable = getStrObservable(shouldCache);
            apiResponseObservable = strObservable.map(new Function<String, String>() {
                @Override
                public String apply(String s) throws Exception {
                    return s;
                }
            });
        }
        return apiResponseObservable;
    }

    public Observable<String> getStrObservable(final boolean shouldCache) {
        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                BaseJsonRequest<String> request = new BaseJsonRequest<String>(getMethod(), initUrl(), initHeader(), initRequest(),
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String s) {
                                TLog.d(TAG, "RxJava request response = " + s);
                                emitter.onNext(s);
                                emitter.onComplete();
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError volleyError) {
                                TLog.d(TAG, "RxJava request Exception = " + volleyError);
                                String errMsg = AppConfig.getContext().getString(R.string.content_network_err);
                                int errCode = AppConfig.ERR_CODE.NETWORK_ERR;
                                if (volleyError != null && volleyError.networkResponse != null) {
                                    errCode = volleyError.networkResponse.statusCode;
                                    errMsg = volleyError.toString();
                                }
                                emitter.onError(new Throwable(errMsg + AppConfig.getContext().getString(R.string.content_error_code) + errCode));
                            }
                        });
                request.setShouldCache(shouldCache);
                NetManager.getInstance().addToRequestQueue(request);
                TLog.d(TAG, "RxJava request start, url =  " + initUrl());
            }
        });
    }

}
