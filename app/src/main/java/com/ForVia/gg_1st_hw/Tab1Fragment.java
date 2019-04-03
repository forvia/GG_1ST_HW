package com.ForVia.gg_1st_hw;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import com.google.firebase.analytics.FirebaseAnalytics;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public class Tab1Fragment extends Fragment {

    /*Poga ierakstīšanai, pēdejā ierakstītā faila nosaukums un failu saraksts*/
    private Button RecordButton;
    private TextView RecentFileName;
    private ListView Recorded_file_list;
    /*pazīme vai dotajā brīdē tiek ierakstīts*/
    private boolean is_recording = false;

    /*Faila nosaukums un adrese*/
    String PathSave = "" ;
    String FileName = "";

    private Context FragmentContext;
    /* Nepieciešamo tiesību saraksts un mainīgais tiesību pieprasīšanai*/
    final int REQUEST_PERMISSION_CODE = 1000;

            String[] Permission_list = {
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.RECORD_AUDIO
            };

    /*Ierakstītāja objekts*/
    private MediaRecorder recorder = null;
    /* failu saraksta attēlošanai*/
    final List<String> file_list = new ArrayList<String>();
    private ArrayAdapter<String> arrayAdapter;
    /*Statistikai*/
    private FirebaseAnalytics mFirebaseAnalytics;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.tab1_fragment,container,false);

        FragmentContext = getActivity().getApplicationContext();
        arrayAdapter = new ArrayAdapter<String>(FragmentContext, android.R.layout.simple_list_item_1, file_list);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(FragmentContext);

        log_firebase("Selected Audio Tab");

        //Ja nav piešķirtas nepieciešamās tiesības, tad paprasam.
        if(!has_permissions())    {
            ask_permissions();
        }

        //Ielasam layout elementus - pēdejā rakstītā faila nosaukums, failu liste un poga.
        RecentFileName = (TextView) view.findViewById(R.id.recent_filename_view);
        Recorded_file_list = (ListView) view.findViewById(R.id.list_view_filelist);
        Recorded_file_list.setAdapter(arrayAdapter);

        //Ielasa skaņas failus no krātuves (tādiem vajadzētu būt tikai ja aplikācija neaizveras korekti).
                 populate_filelist();

        RecordButton = (Button) view.findViewById(R.id.Audio_record_button);


        RecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(is_recording) //Ja spiešanas brīdī ieraksta
                {
                    log_firebase("Apturēja audio ierakstu");
                    RecordButton.setText( "Ierakstīt");
                    recorder.stop();
                    recorder.release();
                    is_recording = false;
                    RecentFileName.setText(FileName);
                    file_list.add(FileName);
                    arrayAdapter.notifyDataSetChanged();
                }
                else
                {
                    log_firebase("Sāka audio ierakstu");
                    if(!has_permissions())
                    {
                       ask_permissions();
                       return;
                    }
                    is_recording = true;

                    FileName = UUID.randomUUID().toString()+".3gp";
                    PathSave =  FragmentContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC).toString()+ "/" + FileName;

                    recorder = new MediaRecorder();
                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    recorder.setOutputFile(PathSave);

                    try {
                        recorder.prepare();
                    } catch (IOException e) {
                        Toast.makeText(FragmentContext, "prepare() failed", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    RecordButton.setText("STOP");
                    recorder.start();
                }
            }
        });

        return view;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        File storageDir = FragmentContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        file_cleanup(storageDir);
    }


    public void ask_permissions()
    {
        log_firebase("Asked permissions");
        ActivityCompat.requestPermissions(getActivity(), Permission_list,   REQUEST_PERMISSION_CODE);
    }

    public boolean has_permissions()
    {

        for(int x = 0; x<Permission_list.length;x++)
        {
            if (ContextCompat.checkSelfPermission(FragmentContext,Permission_list[x])!=PackageManager.PERMISSION_GRANTED)
            {
                return false;
            }
        }

        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        switch (requestCode)
        {
            case REQUEST_PERMISSION_CODE:
            {
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Toast.makeText(FragmentContext, "Permission Granted", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(FragmentContext, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
            break;


        }

    }

    //funkcija, kas nolasa ierakstītos failus no direktorijas un ieliek sarakstā.
    public void populate_filelist()
    {
        File dir = FragmentContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; ++i)
                    file_list.add( files[i].getName());
                }
        arrayAdapter.notifyDataSetChanged();
    }


    //Failu iztīrīšnas funkcija. Rekursija šajā gadījumā lieka, bet atstāju.
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

    public void log_firebase(String s)
    {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, s);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

}
