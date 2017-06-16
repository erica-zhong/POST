package org.snowcorp.imageupload;

import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.SimpleMultiPartRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.lang.System.in;

public class MainActivity extends AppCompatActivity {
    private ImageView imageView;
    private Button btnChoose, btnUpload;
    private ProgressBar progressBar;

    private static final String TAG = "MainActivity";
    public static String BASE_URL = "http://172.19.144.219:12345/images";
    static final int PICK_IMAGE_REQUEST = 1;
    String filePath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ImageView) findViewById(R.id.imageView);
        btnChoose = (Button) findViewById(R.id.button_choose);
        btnUpload = (Button) findViewById(R.id.button_upload);

        btnChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageBrowse();
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (filePath != null) {
                    imageUpload(filePath);
                } else {
                    Toast.makeText(getApplicationContext(), "Image not selected!", Toast.LENGTH_LONG).show();
                }

            }
        });
    }

    private void imageBrowse() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Start the Intent
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if(requestCode == PICK_IMAGE_REQUEST){
                Uri picUri = data.getData();

                filePath = getPath(picUri);

                Log.d("picUri", picUri.toString());
                Log.d("filePath", filePath);

                imageView.setImageURI(picUri);

            }

        }

    }

    private void imageUpload(final String imagePath) {

        SimpleMultiPartRequest smr = new SimpleMultiPartRequest(Request.Method.POST, BASE_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Response", response);
                        HashMap allLetters = new HashMap();
                        try {
//                            JSONObject jObj = new JSONObject(response);
//                            String message = jObj.getString("message");
                            // ADDED JSON PARSING
                            HashMap dimensions = new HashMap();
                            JSONArray jsonArray = new JSONArray(response);
                            for(int idx=0; idx<jsonArray.length(); idx++){

                                JSONObject jsonObject = jsonArray.getJSONObject(idx);
                                int x_start = jsonObject.getInt("x_start");
                                int y_start = jsonObject.getInt("y_start");
                                int x_dim = jsonObject.getInt("x_dim");
                                int y_dim = jsonObject.getInt("y_dim");

                                //convert this JSONArray to Array type
                                JSONArray img = jsonObject.getJSONArray("img");


                                Toast.makeText(getApplicationContext(), x_dim, Toast.LENGTH_LONG).show();

                                // COMPARISON FUNCTION FOR FEATURE ANALYSIS
                                // assuming we have a "database" with the template letters
                                // map each letter to the distance between it and every letter in the template
                                Map<String, String> mapping = new HashMap<String, String>();

                                for(int i = 0; i<database.names().length(); i++){
                                    int[] temp = database.get(database.names().getString(i));
                                    //"key = " + database.names().getString(i)
                                    // "value = " + database.get(database.names().getString(i)));
                                    int num = 0;
                                    for(int l=0; l<img.length(); l++){
                                        //error here because img is parsed as a json array (ex. [{1:[1,2,3,4,5...]}]
                                        //supposed to be [1, 2, 3, 4,5...]
                                        num += Math.pow(img[i], 2) + Math.pow(temp[l], 2);
                                    }
                                    mapping.put(database.names().getString(i), num);
                                }
                                int min = 0 ;
                                String let = null;
                                //first entry in map should be a string of the letter 'a' or 'b'
                                for (Map.Entry<String, Integer> entry : mapping.entrySet()) {
                                    //key = entry.getKey()
                                    //Value = " + entry.getValue());
                                    int val = entry.getValue();
                                    if (val < min){
                                        min = val;
                                        let = entry.getKey();
                                    }
                                }
                                //allLetters contains each letter that is recognized mapped to its dimensions
                                //next step is to put this through location algorithm to return words

                                allLetters.put(let, dimensions);
                                dimensions.put("x_start", x_start);
                                dimensions.put("y_start", y_start);
                                dimensions.put("x_dim", x_dim);
                                dimensions.put("y_dim", y_dim);
                                //sort based on y_start to get the top left character
                            }


                            } catch (JSONException e) {
                            // JSON error
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), "Json error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        smr.addFile("image", imagePath);
        MyApplication.getInstance().addToRequestQueue(smr);


    }

    private String getPath(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(getApplicationContext(), contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

}
