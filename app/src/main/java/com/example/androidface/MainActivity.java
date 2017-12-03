package com.example.androidface;



import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.FaceAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.w3c.dom.Text;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Vision vision;

    public static final String FILE_NAME = "temp.jpg";

    private static final int GALLERY_PERMISSIONS_REQUEST = 0;
    private static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;

    private ImageView imageInput;
    private TextView textInput;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        FloatingActionButton button = (FloatingActionButton) findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder
                        .setMessage("Please choose your image")
                        .setPositiveButton("Upload from Gallery", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                pickImage();
                            }
                        })
                        .setNegativeButton("Take a Photo", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                takePicture();
                            }
                        });
                builder.create().show();
            }
        });


        imageInput = (ImageView) findViewById(R.id.main_image);
        textInput = (TextView) findViewById(R.id.text_description);

    }

    public File getCameraFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }


    private void takePicture() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePicture.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePicture, CAMERA_IMAGE_REQUEST);
        }
    }

    public void pickImage() {

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), GALLERY_IMAGE_REQUEST);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imageInput.setImageBitmap(imageBitmap);
            cloudVision(imageBitmap);

        }
        else if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == RESULT_OK && data != null){
            //Picasso.with(MainActivity.this).load(data.getData()).noPlaceholder().centerCrop().fit()
              //      .into(imageInput);
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                imageInput.setImageBitmap(selectedImage);
                cloudVision(selectedImage);
            }catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }
    }



    //take in image input and outputs Toasts for the result
    public void cloudVision(final Bitmap bitmap){


        Vision.Builder visionBuilder = new Vision.Builder(
                new NetHttpTransport(),
                new AndroidJsonFactory(),
                null);

        visionBuilder.setVisionRequestInitializer(
                new VisionRequestInitializer("AIzaSyBXhbKk7I9GFJQJBTgEmxYCJoZ4CgkKjd4"));

        vision = visionBuilder.build();




        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {

                try {

                    Image inputImage = new Image();

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] photoData = stream.toByteArray();


                    inputImage.encodeContent(photoData);


                    Feature desiredFeature = new Feature();
                    desiredFeature.setType("LABEL_DETECTION");
                    //desiredFeature.setMaxResults(10);


                    AnnotateImageRequest request = new AnnotateImageRequest();
                    request.setImage(inputImage);
                    request.setFeatures(Arrays.asList(desiredFeature));


                    BatchAnnotateImagesRequest batchRequest =
                            new BatchAnnotateImagesRequest();

                    batchRequest.setRequests(Arrays.asList(request));


                    BatchAnnotateImagesResponse batchResponse =
                            vision.images().annotate(batchRequest).execute();


                    List<EntityAnnotation> labels = batchResponse.getResponses()
                            .get(0).getLabelAnnotations();



                    // Count faces
                    int numberOfLabels = labels.size();



                    // Get joy likelihood for each face
                    String foodOutput = "";
                    String myString ="";
                    for(int i=0; i<numberOfLabels; i++) {
                        if(i==0)
                            myString=labels.get(i).getDescription();
                        if ((labels.get(i).getScore() < 0.7) || (labels.get(i).getDescription().indexOf("dish") > -1) || (labels.get(i).getDescription().indexOf("food") > -1)){
                            labels.remove(i);
                            i --;
                            numberOfLabels --;
                        }
                        else if (labels.get(i).getDescription().indexOf("food") > -1){
                            labels.remove(i);
                            i --;
                            numberOfLabels --;
                        }
                        else {

                            foodOutput += "\n There is " + labels.get(i).getDescription() +  " in the picture.";
                        }

                    }

                    // Concatenate everything
                    final String message =
                            "This photo contains" + numberOfLabels + " types of food" + foodOutput;

                    // Display toast on UI thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textInput.setText(message);
                            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);

                        }
                    });

                    Intent intent = new Intent(getApplicationContext(),SecondActivity.class);
                    intent.putExtra("key",myString);
                    startActivity(intent);

                } catch(IOException e){

                    e.printStackTrace();
                }
            }



        });
    }

}







