package com.ForVia.gg_1st_hw;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.BaseAdapter;
import com.google.firebase.analytics.FirebaseAnalytics;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static android.app.Activity.RESULT_OK;


public class Tab2Fragment extends Fragment {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageButton PhotoButton;
    private ImageView MainImage;
    private GridView ThumbGrid;
    private ImageAdapter adapter;
    private FirebaseAnalytics mFirebaseAnalytics;
    private Context FragmentContext;
    String mCurrentPhotoPath;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.tab2_fragment,container,false);
        /*Elementi*/

        MainImage = (ImageView) view.findViewById(R.id.MainImageView); //Lielākā bilde kreisajā pusē
        PhotoButton = (ImageButton) view.findViewById(R.id.PhotoButton); //Poga fotografēšanai
        FragmentContext = getActivity().getApplicationContext(); /*Ērtības labad*/
        ThumbGrid = (GridView) view.findViewById(R.id.small_image_gridview); //Mazās skrullējamās bildes

        /*adapteris funkcionalitātei*/
        adapter = new ImageAdapter(FragmentContext);
        ThumbGrid.setAdapter(adapter);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(FragmentContext);

        log_firebase("Selected Photo Tab");

        PhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Picturizator(); //Funkcija kas veic fotografēšanu/datu rakstīšanu utt.
            }
        });

        return view;
    }


    @Override
    public void onDestroy() {
       super.onDestroy();
        File storageDir = FragmentContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        file_cleanup(storageDir);
    }


    public void Picturizator()
    {
        Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Izveidojam tukšu failu, ko padot kā parametru talruņa fotografēšanas funkcijaai

        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
    }
        //Izsaucam kameru un norādām papildu ierakstīšanas failu
        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(FragmentContext,
                    "com.ForVia.gg_1st_hw.android.fileprovider",
                    photoFile);
            camera_intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            camera_intent.putExtra("data", photoURI);
        }
        /*Uzstartējam kameru*/
        startActivityForResult(camera_intent,REQUEST_IMAGE_CAPTURE);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode,resultCode,data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bitmap newPic = BitmapFactory.decodeFile(mCurrentPhotoPath);

            //Uzstādam kreiso bildi
            MainImage.setImageBitmap(newPic);

            //Ieliekam rullējamajā listē
            adapter.addThisBitmap(newPic);
            adapter.notifyDataSetChanged();
        }

    }


//GGA: izmantošu šo.
    public class ImageAdapter extends BaseAdapter {
        private Context mContext;
        // References to our images
        ArrayList<Bitmap> mThumbs = new ArrayList<Bitmap>();

        public ImageAdapter(Context c) {
            mContext = c;
        }

        public void addThisBitmap(Bitmap newmap) {
            mThumbs.add(newmap);
        }

        public int getCount() {
            return mThumbs.size();
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new ViewGroup.LayoutParams(500, 500));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(8, 8, 8, 8);
            } else {
                imageView = (ImageView) convertView;
            }

            imageView.setImageBitmap(mThumbs.get(position));
            return imageView;
        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = FragmentContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();

        return image;
    }

//*izmantoju metodi no pd parauga.
    public void file_cleanup (File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; ++i) {
                File file = files[i];
                if (file.isDirectory()) {
                    file_cleanup(file);
                } else {
                    // Delete files
                    file.delete();
                }
            }
        }
    }


    //funkcija ātrai firebase logošanai
    public void log_firebase(String s)
    {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, s);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }




}
