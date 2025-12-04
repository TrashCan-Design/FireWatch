package com.example.firewatch;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static Retrofit retrofit;
    private static Context context;


    public static void init(Context ctx) {
        context = ctx.getApplicationContext();
    }

    public static void reset() {
        retrofit = null;
    }

    public static SupabaseApi api(Context ctx) {
        if (context == null) {
            init(ctx);
        }

        if (retrofit == null) {
            SharedPreferences sp = ctx.getSharedPreferences("firewatch_prefs", Context.MODE_PRIVATE);

            String url = sp.getString("supabase_url", "<SupaBase Url>");
            String key = sp.getString("supabase_key",
                    "<SupaBase Key>");

            if (!url.endsWith("/")) url += "/";

            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(log)
                    .addInterceptor(new Interceptor() {
                        @NonNull
                        @Override
                        public Response intercept(@NonNull Chain chain) throws IOException {
                            Request request = chain.request().newBuilder()
                                    .header("apikey", key)
                                    .header("Authorization", "Bearer " + key)
                                    .header("Content-Type", "application/json")
                                    .build();
                            return chain.proceed(request);
                        }
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(url)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit.create(SupabaseApi.class);
    }
}
