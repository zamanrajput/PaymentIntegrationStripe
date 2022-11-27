package pk.zaman.paymentintegrationwithstripe;

import static android.content.ContentValues.TAG;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import pk.zaman.paymentintegrationwithstripe.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private final String SECRET_KEY = "sk_test_51LYp1qDe8MGafGTkL1CM4lK9ifyMRJ0stPKHN8AWacz15i7UnhcOlnXRhdfmP9yGJoFFZYBlVgfDOF6CU2xUX2yH00MGiCrkp8";
    private final String PUBLISH_KEY = "pk_test_51LYp1qDe8MGafGTk3aQ6tzwNAuOu0oKAwANuSYhbuYWYnvaiaHUfDYvD2UmFaJlPDZ4LOmkotON0Eypo1LLmNEC900MAQE7bDt";
    PaymentSheet paymentSheet;
    String customerID, ephemeralKey, secret_id;
    ProgressDialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        PaymentConfiguration.init(this, PUBLISH_KEY);


        paymentSheet = new PaymentSheet(this, paymentSheetResult -> {
            if (paymentSheetResult instanceof PaymentSheetResult.Completed) {
                Toast.makeText(MainActivity.this, "Payment Successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, paymentSheetResult.describeContents(), Toast.LENGTH_SHORT).show();
            }
        });
        dialog = new ProgressDialog(MainActivity.this);
        binding.Pay.setOnClickListener(view -> Pay(listener));

    }

    Listener listener = new Listener() {


        @Override
        public void onStart() {
            dialog.setMessage("Authenticating");
            dialog.show();
        }

        @Override
        public void onComplete() {
            dialog.dismiss();
            paymentFlow();
        }
    };

    void Pay(Listener listener) {
        Customers(listener);
    }

    private void Customers(Listener listener) {
        listener.onStart();
        OkHttpClient client = new OkHttpClient();
        RequestBody body = new FormBody.Builder().add("", "").
                build();
        Request request = new Request.Builder().url("https://api.stripe.com/v1/customers").post(body).header("Authorization", "Bearer " + SECRET_KEY).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.i(TAG, "onFailure: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String RESPONSE = response.body().string();
                Log.i(TAG, "onResponse: " + RESPONSE);
                JSONObject object = null;
                try {
                    object = new JSONObject(RESPONSE);
                    customerID = object.getString("id");
                    getEphericalID(customerID, listener);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });


    }

    private void getEphericalID(String id, Listener listener) {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = new FormBody.Builder().add("customer", id).
                build();
        Request request = new Request.Builder().url("https://api.stripe.com/v1/ephemeral_keys").header("Stripe-Version", "2022-08-01").post(body).header("Authorization", "Bearer " + SECRET_KEY).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.i(TAG, "onFailure: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String RESPONSE = response.body().string();
                Log.i(TAG, "onResponse: " + RESPONSE);

                JSONObject object = null;
                try {

                    object = new JSONObject(RESPONSE);
                    ephemeralKey = object.getString("id");
                    getClientSecret(customerID, listener);
                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        });

    }

    private void getClientSecret(String key, Listener listener) {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = new FormBody.Builder().add("customer", key).
                add("amount", "10000").
                add("currency", "usd").
                add("automatic_payment_methods[enabled]", "true").
                build();
        Request request = new Request.Builder().url("https://api.stripe.com/v1/payment_intents").header("Stripe-Version", "2022-08-01").post(body).header("Authorization", "Bearer " + SECRET_KEY).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.i(TAG, "onFailure: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String RESPONSE = response.body().string();
                Log.i(TAG, "onResponse: " + RESPONSE);

                JSONObject object = null;
                try {
                    object = new JSONObject(RESPONSE);
                    secret_id = object.getString("client_secret");
                    listener.onComplete();

                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        });

    }

    void paymentFlow() {
        paymentSheet.presentWithPaymentIntent(
                secret_id,
                new PaymentSheet.Configuration("Test Company",
                        new PaymentSheet.CustomerConfiguration(customerID, ephemeralKey)
                )
        );
    }


}