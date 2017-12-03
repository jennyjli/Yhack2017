package com.example.androidface;

/**
 * Created by SXMao on 12/3/17.
 */

        import android.app.ProgressDialog;
        import android.content.Context;
        import android.content.Intent;
        import android.graphics.Bitmap;
        import android.graphics.BitmapFactory;
        import android.net.Uri;
        import android.os.AsyncTask;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.util.Log;
        import android.view.View;
        import android.widget.ImageView;
        import android.widget.RelativeLayout;
        import android.widget.TextView;
        import android.widget.Toast;



        import org.apache.http.HttpResponse;
        import org.apache.http.client.methods.HttpGet;
        import org.apache.http.impl.client.DefaultHttpClient;
        import org.json.JSONArray;
        import org.json.JSONException;
        import org.json.JSONObject;

        import java.io.BufferedReader;

        import java.io.IOException;
        import java.io.InputStream;
        import java.io.InputStreamReader;
        import java.net.HttpURLConnection;
        import java.net.MalformedURLException;
        import java.net.URL;


        import org.apache.http.client.HttpClient;

        import static java.security.AccessController.getContext;


public class SecondActivity extends AppCompatActivity {
    private TextView textLabel;
    private String link;
    private ProgressDialog dialog;
    private String ingredient;
    private String imageUrl;
    private RelativeLayout rl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        textLabel = findViewById(R.id.labelText);
        rl = (RelativeLayout)findViewById(R.id.layoutButton);


        dialog = new ProgressDialog(this);
        dialog.setMessage("Loading...");
        dialog.show();
        Bundle extras = getIntent().getExtras();
        String value ="";
        if(extras!=null){
         value = extras.getString("key");
        }
        ingredient = value; //modify
        new HttpAsyncTask().execute(String.format("http://food2fork.com/api/search?key=e0b95b1f6f7de48563f28a9e3b36ab1f&q=%s", Uri.encode(ingredient)));
        rl.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                startActivity(browserIntent);
            }
        });

    }
    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpResponse httpResponse = httpclient.execute(new HttpGet(urls[0]));
                InputStream inputStream = httpResponse.getEntity().getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                return result.toString();
            } catch (Exception e) {
                System.out.print(e.fillInStackTrace());
            }
            return null;

        }
        @Override
        protected void onPostExecute(String result) {
            dialog.hide();

            try {
                if (result != null) {
                    JSONObject json = new JSONObject(result);
                    int count = json.optInt("count");
                    if (count == 0) {
                        throw new Exception("No weather information found for " + ingredient);
                    }
                    JSONArray recipesJson = json.getJSONArray("recipes");
                    textLabel.setText(recipesJson.getJSONObject(0).optString("title"));
                    link = recipesJson.getJSONObject(0).optString("source_url");
                    imageUrl = recipesJson.getJSONObject(0).optString("image_url");
                }

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch(Exception e){
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                rl.setEnabled(false);
                rl.setVisibility(View.GONE);
            }
        }

    }

    public void onReturn(View v)
    {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

}
