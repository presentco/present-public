package present.server.notification.fcm;

import com.google.gson.JsonObject;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Copied from https://github.com/PopTudor/Java-fcm so we can adjust the OkHttp client, etc.
 *
 * Created by Tudor on 15-Nov-16.
 */
public class FcmSender {
  private final Fcm fcm;

  public FcmSender(String serverKey) {
    Retrofit retrofit = createRetrofit(serverKey).build();
    this.fcm = retrofit.create(Fcm.class);
  }

  private Retrofit.Builder createRetrofit(String serverKey) {
    new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.NONE);
    OkHttpClient httpClient = new OkHttpClient.Builder()
        .addInterceptor(createInterceptor(serverKey))
        .build();
    // We can send up to 100 notifications at a time.
    httpClient.dispatcher().setMaxRequestsPerHost(100);
    return new Retrofit.Builder()
        .baseUrl("https://fcm.googleapis.com/fcm/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create());
  }

  private Interceptor createInterceptor(String serverKey) {
    return chain -> {
      Request request = chain.request().newBuilder()
          .header("Authorization", String.format("key=%s", serverKey))
          .header("content-type", "application/json")
          .build();
      return chain.proceed(request);
    };
  }

  public void send(FcmMessage message) {
    send(message, new DefaultCallback());
  }

  public void send(FcmMessage message, Callback callback) {
    fcm.send(message).enqueue(callback);
  }

  /** Adapter for Firebase Cloud Messenger API. */
  private interface Fcm {
    @POST("send")
    Call<JsonObject> send(@Body FcmMessage to);
  }

  private class DefaultCallback implements Callback {
    @Override
    public void onResponse(Call call, Response response) {
      System.out.println(response.body());
    }

    @Override
    public void onFailure(Call call, Throwable t) {
      t.printStackTrace();
    }
  }
}