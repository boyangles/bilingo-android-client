/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.bilingo.bilingoclientapp;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.translate.Translate;
import com.google.api.services.translate.TranslateRequest;
import com.google.api.services.translate.TranslateRequestInitializer;
import com.google.api.services.translate.model.TranslationsListResponse;
import com.google.api.services.translate.model.TranslationsResource;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.LocalizedObjectAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.NormalizedVertex;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    public static final String CLOUD_VISION_API_KEY = BuildConfig.API_KEY;
    public static final String FILE_NAME = "temp.jpg";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    private static final int MAX_LABEL_RESULTS = 10;
    private static final int MAX_DIMENSION = 1200;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int GALLERY_PERMISSIONS_REQUEST = 0;
    private static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;

    private RecyclerView mRecyclerView;
    private ImageLabelAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    public ImageView mMainImage;
    private ProgressBar mProgressBar;

    public Map<String, Bitmap> mBitmaps;
    public Map<String, String> mTranslations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Spinner srcSpinner = findViewById(R.id.source_lang_spinner);
        ArrayAdapter<CharSequence> srcAdapter = ArrayAdapter.createFromResource(this,
                R.array.languages_array, R.layout.spinner_item);
        srcAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        srcSpinner.setAdapter(srcAdapter);

        Spinner dstSpinner = findViewById(R.id.target_lang_spinner);
        ArrayAdapter<CharSequence> dstAdapter = ArrayAdapter.createFromResource(this,
                R.array.languages_array, R.layout.spinner_item);
        dstAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dstSpinner.setAdapter(dstAdapter);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> startCamera());

        FloatingActionButton gal = findViewById(R.id.gal);
        gal.setOnClickListener(view -> startGalleryChooser());

        mBitmaps = new HashMap<>();
        mTranslations = new HashMap<>();

        mLayoutManager = new LinearLayoutManager(this);
        mAdapter = new ImageLabelAdapter(this);
        mRecyclerView = findViewById(R.id.rvLabel);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mProgressBar = findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.GONE);

        mMainImage = findViewById(R.id.main_image);
        mMainImage.setOnClickListener((View view) -> {
            Bitmap mainBitmap = mBitmaps.get("MAIN_BITMAP");
            if (mainBitmap != null) {
                mMainImage.setImageBitmap(mainBitmap);
            }
        });
    }

    public void startGalleryChooser() {
        if (PermissionUtils.requestPermission(this, GALLERY_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select a photo"),
                    GALLERY_IMAGE_REQUEST);
        }
    }

    public void startCamera() {
        if (PermissionUtils.requestPermission(
                this,
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
        }
    }

    public File getCameraFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Bitmap bitmap = null;

        if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            bitmap = transformBitmap(getBitmapFromUri(uri), uri);
        } else if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            bitmap = transformBitmap(getBitmapFromUri(uri), uri);
        }

        if (bitmap == null) {
            Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            return;
        }

        uploadImage(bitmap);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, CAMERA_PERMISSIONS_REQUEST, grantResults)) {
                    startCamera();
                }
                break;
            case GALLERY_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, GALLERY_PERMISSIONS_REQUEST, grantResults)) {
                    startGalleryChooser();
                }
                break;
        }
    }

    public Bitmap getBitmapFromUri(Uri uri) {
        if (uri == null) {
            Log.d(TAG, "Image picker gave us a null image.");
            return null;
        }

        try {
            return MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        } catch (IOException e) {
            Log.d(TAG, "Image picking failed because " + e.getMessage());
            return null;
        }
    }

    public Bitmap transformBitmap(Bitmap bitmap, Uri uri) {
        if (bitmap == null) {
            Log.d(TAG, "Input bitmap is null");
            return null;
        }

        if (uri == null) {
            Log.d(TAG, "URI is null. Not transforming");
            return bitmap;
        }

        Bitmap result = scaleBitmapDown(bitmap, MAX_DIMENSION);

        InputStream in = null;
        try {
            in = getContentResolver().openInputStream(uri);

            ExifInterface exif = new ExifInterface(in);
            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                                                ExifInterface.ORIENTATION_NORMAL);

            int rotationInDegrees = 0;
            if (rotation == ExifInterface.ORIENTATION_ROTATE_90) {
                rotationInDegrees = 90;
            } else if (rotation == ExifInterface.ORIENTATION_ROTATE_180) {
                rotationInDegrees = 180;
            } else if (rotation == ExifInterface.ORIENTATION_ROTATE_270) {
                rotationInDegrees = 270;
            }

            Matrix matrix = new Matrix();
            if (rotation != 0) {
                matrix.preRotate(rotationInDegrees);
            }

            result = Bitmap.createBitmap(result, 0, 0, result.getWidth(), result.getHeight(), matrix, true);
        } catch (IOException e) {
            Log.d(TAG, "Bitmap transformation error: " + e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {}
            }
        }

        return result;
    }

    public void uploadImage(Bitmap bitmap) {
        // Clear previous translations
        mTranslations.clear();

        // Clear previous references to bitmaps
        mBitmaps.clear();
        mBitmaps.put("MAIN_BITMAP", bitmap);

        // scale the image to save on bandwidth
        callCloudVision(bitmap);

        mMainImage.setImageBitmap(bitmap);
    }

    public Translate.Translations.List prepareTranslateRequest(List<String> values, String sourceLang, String targetLang) throws IOException {
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        TranslateRequestInitializer requestInitializer = new TranslateRequestInitializer(CLOUD_VISION_API_KEY) {
            @Override
            protected  void initializeTranslateRequest(TranslateRequest<?> translateRequest)
                    throws IOException {
                super.initializeTranslateRequest(translateRequest);

                String packageName = getPackageName();
                translateRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                translateRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
            }
        };

        Translate.Builder builder = new Translate.Builder(httpTransport, jsonFactory, null);
        builder.setTranslateRequestInitializer(requestInitializer);

        Translate translate = builder.build();
        Translate.Translations.List list = translate.new Translations().list(values, targetLang);
        list.setKey(CLOUD_VISION_API_KEY);
        list.setSource(sourceLang);

        return list;
    }

    private Vision.Images.Annotate prepareAnnotationRequest(Bitmap bitmap) throws IOException {
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        VisionRequestInitializer requestInitializer =
                new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                    /**
                     * We override this so we can inject important identifying fields into the HTTP
                     * headers. This enables use of a restricted cloud platform API key.
                     */
                    @Override
                    protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                            throws IOException {
                        super.initializeVisionRequest(visionRequest);

                        String packageName = getPackageName();
                        visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                        String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                        visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                    }
                };

        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(requestInitializer);

        Vision vision = builder.build();

        BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                new BatchAnnotateImagesRequest();
        batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
            AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

            // Add the image
            Image base64EncodedImage = new Image();
            // Convert the bitmap to a JPEG
            // Just in case it's a format that Android understands but Cloud Vision
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            // Base64 encode the JPEG
            base64EncodedImage.encodeContent(imageBytes);
            annotateImageRequest.setImage(base64EncodedImage);

            // add the features we want
            annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                Feature objectLocalization = new Feature();
                objectLocalization.setType("OBJECT_LOCALIZATION");
                objectLocalization.setMaxResults(MAX_LABEL_RESULTS);
                add(objectLocalization);
            }});

            // Add the list of one thing to the request
            add(annotateImageRequest);
        }});

        Vision.Images.Annotate annotateRequest =
                vision.images().annotate(batchAnnotateImagesRequest);
        // Due to a bug: requests to Vision API containing large images fail when GZipped.
        annotateRequest.setDisableGZipContent(true);
        Log.d(TAG, "created Cloud Vision request object, sending request");

        return annotateRequest;
    }

    private static class TranslationTask extends AsyncTask<Object, Void, TranslationsListResponse> {
        private Translate.Translations.List mRequest;

        TranslationTask(Translate.Translations.List list) {
            mRequest = list;
        }

        @Override
        protected TranslationsListResponse doInBackground(Object... params) {
            try {
                Log.d(TAG, "created Cloud Vision request object, sending request");
                TranslationsListResponse response = mRequest.execute();

                return response;
            } catch (GoogleJsonResponseException e) {
                Log.d(TAG, "failed to make API request because " + e.getContent());
            } catch (IOException e) {
                Log.d(TAG, "failed to make API request because of other IOException " +
                        e.getMessage());
            }
            return null;
        }
    }

    private static class LableDetectionTask extends AsyncTask<Object, Void, BatchAnnotateImagesResponse> {
        private final WeakReference<MainActivity> mActivityWeakReference;
        private Vision.Images.Annotate mRequest;

        LableDetectionTask(MainActivity activity, Vision.Images.Annotate annotate) {
            mActivityWeakReference = new WeakReference<>(activity);
            mRequest = annotate;
        }

        @Override
        protected BatchAnnotateImagesResponse doInBackground(Object... params) {
            try {
                Log.d(TAG, "created Cloud Vision request object, sending request");
                BatchAnnotateImagesResponse response = mRequest.execute();

                return response;
            } catch (GoogleJsonResponseException e) {
                Log.d(TAG, "failed to make API request because " + e.getContent());
            } catch (IOException e) {
                Log.d(TAG, "failed to make API request because of other IOException " +
                        e.getMessage());
            }
            return null;
        }

        protected void onPostExecute(BatchAnnotateImagesResponse result) {
            MainActivity activity = mActivityWeakReference.get();
            if (activity != null && !activity.isFinishing()) {
                ProgressBar progressBar = activity.findViewById(R.id.progress_bar);

                progressBar.setVisibility(View.GONE);

                if (result == null) {
                    return;
                }

                Bitmap mainBitmap = activity.mBitmaps.get("MAIN_BITMAP");

                AnnotateImageResponse annotateImageResponse = result.getResponses().get(0);
                List<LocalizedObjectAnnotation> labels = annotateImageResponse.getLocalizedObjectAnnotations();

                if (labels != null && labels.size() > 0 && mainBitmap != null) {
                    Bitmap blurredBitmap = BlurUtils.fastblur(mainBitmap, 0.10f, 7);
                    blurredBitmap = Bitmap.createScaledBitmap(blurredBitmap, mainBitmap.getWidth(), mainBitmap.getHeight(), false);
                    activity.mBitmaps.put("BLURRED_BITMAP", blurredBitmap);

                    for (int i = 0; i < labels.size(); i++) {
                        LocalizedObjectAnnotation label = labels.get(i);
                        List<NormalizedVertex> vertices = label.getBoundingPoly().getNormalizedVertices();

                        if (vertices.size() == 4) {
                            NormalizedVertex v0 = vertices.get(0); // Top Left
                            NormalizedVertex v2 = vertices.get(2); // Bottom Right

                            float v0x_n = (v0.getX() == null) ? 0.0f : v0.getX();
                            float v0y_n = (v0.getY() == null) ? 0.0f : v0.getY();
                            float v2x_n = (v2.getX() == null) ? 0.0f : v2.getX();
                            float v2y_n = (v2.getY() == null) ? 0.0f : v2.getY();

                            float bitmapWidth = mainBitmap.getWidth();
                            float bitmapHeight = mainBitmap.getHeight();

                            double v0x = Math.floor(v0x_n * bitmapWidth);
                            double v0y = Math.floor(v0y_n * bitmapHeight);
                            double v2x = Math.floor(v2x_n * bitmapWidth);
                            double v2y = Math.floor(v2y_n * bitmapHeight);

                            int startX = (int)Math.abs(v0x);
                            int startY = (int)Math.abs(v0y);
                            int w = (int)Math.abs(v2x - v0x);
                            int h = (int)Math.abs(v2y - v0y);

                            Bitmap resizedBitmap = Bitmap.createBitmap(mainBitmap, startX, startY, w, h);

                            Bitmap bmOverlay = Bitmap.createBitmap(blurredBitmap.getWidth(), blurredBitmap.getHeight(), blurredBitmap.getConfig());
                            Canvas canvas = new Canvas(bmOverlay);
                            canvas.drawBitmap(blurredBitmap, new Matrix(), null);
                            canvas.drawBitmap(resizedBitmap, startX, startY, null);

                            activity.mBitmaps.put("BM_OVERLAY:::" + label.getMid() + "___" + i, bmOverlay);
                        }
                    }
                }

                populateItemInfo(result, activity);
            }
        }
    }

    private void callCloudVision(final Bitmap bitmap) {
        // Switch text to loading
        mProgressBar.setVisibility(View.VISIBLE);
        mAdapter.setList(new ArrayList<>());

        // Do the real work in an async task, because we need to use the network anyway
        try {
            AsyncTask<Object, Void, BatchAnnotateImagesResponse> labelDetectionTask =
                    new LableDetectionTask(this, prepareAnnotationRequest(bitmap));
            labelDetectionTask.execute();
        } catch (IOException e) {
            Log.d(TAG, "failed to make API request because of other IOException " +
                    e.getMessage());
        }
    }

    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private static void populateItemInfo(BatchAnnotateImagesResponse batchResult,
                                                 MainActivity activity) {
        AnnotateImageResponse annotateImageResponse = batchResult.getResponses().get(0);
        List<LocalizedObjectAnnotation> labels = annotateImageResponse.getLocalizedObjectAnnotations();

        String sourceLang = "en";
        String targetLang = "zh-CN";
        List<String> items = new ArrayList<>();

        for (int i = 0; i < labels.size(); i++) {
            items.add(labels.get(i).getName());
        }

        try {
            AsyncTask<Object, Void, TranslationsListResponse> translationTask =
                    new TranslationTask(activity.prepareTranslateRequest(items, sourceLang, targetLang));
            TranslationsListResponse response = translationTask.execute().get();

            List<TranslationsResource> translationsResources = null;
            if (response != null) {
                translationsResources = response.getTranslations();
            }


            if (translationsResources != null && activity != null) {
                for (int i = 0; i < translationsResources.size(); i++) {
                    if (i >= items.size()) {
                        break;
                    }

                    Log.d(TAG, items.get(i) + " " + translationsResources.get(i).getTranslatedText());

                    TranslationsResource tr = translationsResources.get(i);
                    String translatedText = LanguageUtils.getDecoratedPhrase(tr.getTranslatedText(), targetLang);
                    activity.mTranslations.put(sourceLang + ":::" + targetLang + ":::" + items.get(i),
                            translatedText);

                }
            }
        } catch (IOException e) {
            Log.d(TAG, "failed to make API request because of other IOException " +
                    e.getMessage());
        } catch (InterruptedException e) {
            Log.d(TAG, "failed to make API request because of InterruptedException " +
                    e.getMessage());
        } catch (ExecutionException e) {
            Log.d(TAG, "failed to make API request because of other ExecutionException " +
                    e.getMessage());
        }

        populateIdentifiedItemsView(batchResult, activity);
    }

    private static void populateIdentifiedItemsView(BatchAnnotateImagesResponse response,
                                                    MainActivity activity) {
        AnnotateImageResponse annotateImageResponse = response.getResponses().get(0);
        List<LocalizedObjectAnnotation> labels = annotateImageResponse.getLocalizedObjectAnnotations();

        if (labels != null) {
            activity.mAdapter.setList(labels);
        }
    }
}
