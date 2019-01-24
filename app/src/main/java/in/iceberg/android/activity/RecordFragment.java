package in.iceberg.android.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.iceberg.in.recording.BuildConfig;
import android.iceberg.in.recording.R;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.dmoral.toasty.Toasty;
import in.iceberg.android.apputil.TextUtils;
import in.iceberg.android.apputil.Util;
import in.iceberg.android.db.AppRecordData;

public class RecordFragment extends Fragment {

    private String title;
    private int page;

    private String output;
    private MediaRecorder mediaRecorder;
    private boolean state, recordingStopped, playbackStopped;

    @BindView(R.id.button_start_recording)
    public ImageButton buttonStartRecording;
    @BindView(R.id.button_pause_recording)
    public ImageButton buttonPauseRecording;
    @BindView(R.id.button_stop_recording)
    public ImageButton buttonStopRecording;
    @BindView(R.id.button_delete_recording)
    public ImageButton buttonDeleteRecording;
    @BindView(R.id.recording_image)
    public ImageView recordingImage;
    @BindView(R.id.recording_image_background)
    public ImageView recordingImageBackground;
    @BindView(R.id.adView)
    public AdView mAdView;
    private MediaPlayer mediaPlayer;
    @BindView(R.id.chronometer)
    public Chronometer chronometer;

    private long elapsedTime;
    private String TAG = "TAG";
    private long timeWhenStopped = 0;

    private final int START = 0, STOP = 1;
    private static final int PERMISSIONS_REQUEST_CODE = 1001;

    public static RecordFragment newInstance(int page, String title) {
        RecordFragment fragmentFirst = new RecordFragment();
        Bundle args = new Bundle();
        args.putInt("someInt", page);
        args.putString("someTitle", title);
        fragmentFirst.setArguments(args);
        return fragmentFirst;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        page = getArguments().getInt("someInt", 0);
        title = getArguments().getString("someTitle");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_first, container, false);
        ButterKnife.bind(this, view.getRootView());

        setButtons(STOP);
        mediaPlayer = new MediaPlayer();
        MobileAds.initialize(getContext(), BuildConfig.ADMOB_APP_ID);

        buttonStartRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleRecordAndStoragePermissions();
            }
        });
        buttonStopRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
            }
        });
        buttonPauseRecording.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View v) {
                pauseRecording();
            }
        });
        buttonDeleteRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isNotNullOrEmpty(output)) {
                    deleteRecording();
                }
            }
        });
        chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                if (!state) {
                    long minutes = ((SystemClock.elapsedRealtime() - chronometer.getBase())/1000) / 60;
                    long seconds = ((SystemClock.elapsedRealtime() - chronometer.getBase())/1000) % 60;
                    elapsedTime = SystemClock.elapsedRealtime();
                    Log.d(TAG, "onChronometerTick: " + minutes + " : " + seconds);
                } else {
                    long minutes = ((elapsedTime - chronometer.getBase())/1000) / 60;
                    long seconds = ((elapsedTime - chronometer.getBase())/1000) % 60;
                    elapsedTime = elapsedTime + 1000;
                    Log.d(TAG, "onChronometerTick: " + minutes + " : " + seconds);
                }
            }
        });

        mAdView = new AdView(getContext());
        mAdView.setAdSize(AdSize.SMART_BANNER);
        mAdView.setAdUnitId(BuildConfig.BANNER_HOME_FOOTER);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("61ef3aeee61b7d1b")
                .build();

        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                //Toast.makeText(getContext(), "Ad is loaded", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onAdClosed() {
                //Toast.makeText(getContext(), "Ad is closed!", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onAdFailedToLoad(int errorCode) {
                //Toast.makeText(getContext(), "Ad failed to load! error code: " + errorCode, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onAdLeftApplication() {
                //Toast.makeText(getContext(), "Ad left application!", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onAdOpened() {
                super.onAdOpened();
            }
        });
        mAdView.loadAd(adRequest);
        return view;
    }

    private void startRecording() {
        if (AppRecordData.isRecordPermissionDenied() || AppRecordData.isStoragePermissionDenied()) {
            return;
        }
        playbackStopped = true;
        recordingStopped = false;
        mediaRecorder = new MediaRecorder();
        output = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.mp3";
        if (mediaRecorder != null) {
            setButtons(START);
            setImage(R.drawable.ic_microphone, R.color.startRecording, R.color.startRecordingBackground);

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(output);
            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
                if (!state) {
                    chronometer.setBase(SystemClock.elapsedRealtime());
                    chronometer.start();
                    timeWhenStopped = 0;
                } else {
                    chronometer.start();
                }
                state = true;
                Toasty.custom(getContext(), getResources().getString(R.string.start_recording_toast_message),
                        getResources().getDrawable(R.drawable.ic_mic), getResources().getColor(R.color.startRecording),
                        Toast.LENGTH_SHORT, true, true).show();
                resetPauseButton();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException io) {
                io.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording() {
        if (state && mediaRecorder != null) {
            playbackStopped = true;
            recordingStopped = true;
            setButtons(STOP);
            setImage(R.drawable.ic_microphone, R.color.stopRecording, R.color.stopRecordingBackground);
            Toasty.custom(getContext(), getResources().getString(R.string.stop_recording_toast_message),
                    getResources().getDrawable(R.drawable.ic_stop), getResources().getColor(R.color.stopRecording),
                    Toast.LENGTH_SHORT, true, true).show();

            mediaRecorder.stop();
            mediaRecorder.release();
            chronometer.stop();
            chronometer.setText("00:00");
            state = false;
            resetPauseButton();
        } else {
            Toasty.custom(getContext(), getResources().getString(R.string.stop_recording_toast_message),
                    getResources().getDrawable(R.drawable.ic_stop), getResources().getColor(R.color.stopRecording),
                    Toast.LENGTH_SHORT, true, true).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void pauseRecording() {
        if (state && mediaRecorder != null) {
            if (!recordingStopped) {
                timeWhenStopped = chronometer.getBase() - SystemClock.elapsedRealtime();
                chronometer.stop();
                playbackStopped = true;
                recordingStopped = true;
                setImage(R.drawable.ic_microphone, R.color.pauseRecording, R.color.pauseRecordingBackground);

                Toasty.custom(getContext(), getResources().getString(R.string.pause_recording_toast_message),
                        getResources().getDrawable(R.drawable.ic_pause), getResources().getColor(R.color.pauseRecording),
                        Toast.LENGTH_SHORT, true, true).show();
                mediaRecorder.pause();
                buttonPauseRecording.setImageResource(R.drawable.ic_mic);
            } else {
                resumeRecording();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private void resumeRecording() {
        playbackStopped = true;
        recordingStopped = false;
        setImage(R.drawable.ic_microphone, R.color.startRecording, R.color.startRecordingBackground);

        Toasty.custom(getContext(), getResources().getString(R.string.resume_recording_toast_message),
                getResources().getDrawable(R.drawable.ic_mic), getResources().getColor(R.color.startRecording),
                Toast.LENGTH_SHORT, true, true).show();
        mediaRecorder.resume();
        chronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
        chronometer.start();
        buttonPauseRecording.setImageResource(R.drawable.ic_pause);
    }

    private void setDrawableColor(int indicatorColor) {
        LayerDrawable drawableFile = (LayerDrawable) recordingImageBackground.getBackground().mutate();
        GradientDrawable gradientDrawable = (GradientDrawable) drawableFile.findDrawableByLayerId(R.id.circle_background);
        gradientDrawable.invalidateSelf();
        drawableFile.invalidateSelf();
        gradientDrawable.setColor(indicatorColor);

        if (!recordingStopped || !playbackStopped) {
            Animation animation = new ScaleAnimation(1, 1.2f, 1, 1.2f,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            animation.setDuration(200);
            animation.setInterpolator(new LinearInterpolator());
            animation.setRepeatCount(Animation.INFINITE);
            animation.setRepeatMode(Animation.REVERSE);
            recordingImageBackground.startAnimation(animation);
        } else {
            recordingImageBackground.clearAnimation();
        }
    }

    private void deleteRecording() {
        stopRecording();
        File file = new File(output);
        if (file.exists() && !state) {
            setImage(R.drawable.ic_microphone, R.color.black, R.color.full_transparent);

            boolean deleted = file.delete();
            if (deleted) {
                Toasty.custom(getContext(), getResources().getString(R.string.deleted_recording_toast_message),
                        getResources().getDrawable(R.drawable.ic_delete), getResources().getColor(R.color.startRecording),
                        Toast.LENGTH_SHORT, true, true).show();
            }
        } else if (state) {
            Toasty.error(getContext(), getResources().getString(R.string.stop_recording_before_toast_message)).show();
        }
    }

    private void setImage(int drawable, int drawableColor, int backgroundColor) {
        recordingImage.setImageDrawable(getResources().getDrawable(drawable));
        recordingImageBackground.setColorFilter(ContextCompat.getColor(getContext(), drawableColor),
                android.graphics.PorterDuff.Mode.SRC_IN);
        setDrawableColor(getResources().getColor(backgroundColor));
    }

    private void resetPauseButton() {
        recordingStopped = false;
        buttonPauseRecording.setImageResource(R.drawable.ic_pause);
    }

    private void setButtons(int state) {
        switch (state) {
            case START:
                buttonStartRecording.setVisibility(View.GONE);
                buttonPauseRecording.setVisibility(View.VISIBLE);
                enableButton(buttonStopRecording);
                break;
            case STOP:
                buttonStartRecording.setVisibility(View.VISIBLE);
                buttonPauseRecording.setVisibility(View.GONE);
                disableButton(buttonStopRecording);
                break;
        }
    }

    private void disableButton(ImageButton imageButton) {
        imageButton.setEnabled(false);
        imageButton.setColorFilter(ContextCompat.getColor(getContext(), R.color.black_56),
                android.graphics.PorterDuff.Mode.SRC_IN);
        imageButton.setBackground(getResources().getDrawable(R.drawable.white_circle_light_grey_border));
    }

    private void enableButton(ImageButton imageButton) {
        imageButton.setEnabled(true);
        imageButton.setColorFilter(ContextCompat.getColor(getContext(), R.color.black),
                android.graphics.PorterDuff.Mode.SRC_IN);
        imageButton.setBackground(getResources().getDrawable(R.drawable.white_circle_grey_border));
    }

    // App Permissions Allow and Deny ...
    private void handleRecordAndStoragePermissions() {
        if (ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(getActivity(),
                    permissions, PERMISSIONS_REQUEST_CODE);
        } else if (!AppRecordData.isStoragePermissionDenied() &&
                !AppRecordData.isRecordPermissionDenied() ) {
            startRecording();
        } else {
            checkRecordPermission();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean grantStatus = false;
        if (requestCode == PERMISSIONS_REQUEST_CODE &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            grantStatus = true;
            for (int i : grantResults) {
                if (i != PackageManager.PERMISSION_GRANTED) {
                    grantStatus = false;
                }
            }
        }
        if (grantStatus) {
            AppRecordData.setRecordPermissionDenied(false);
            AppRecordData.setStoragePermissionDenied(false);
            startRecording();
        } else {
            AppRecordData.setRecordPermissionDenied(true);
            AppRecordData.setStoragePermissionDenied(true);
            checkRecordPermission();
        }
    }

    private void checkRecordPermission() {
        if (AppRecordData.isRecordPermissionDenied()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.RECORD_AUDIO) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            Manifest.permission.READ_EXTERNAL_STORAGE)) {
                String[] permissions = {android.Manifest.permission.RECORD_AUDIO,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(getActivity(),
                        permissions, PERMISSIONS_REQUEST_CODE);
            } else if (ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(getContext(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                AppRecordData.setStoragePermissionDenied(false);
                AppRecordData.setRecordPermissionDenied(false);
                startRecording();
            } else if ((!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.RECORD_AUDIO) || AppRecordData.isRecordPermissionDenied()) ||
                    (!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) || AppRecordData.isStoragePermissionDenied())) {
                Util.showAlertDialog(getString(R.string.permission_needed),
                        getString(R.string.permission_denied_read),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                goToSettings();
                            }
                        },
                        "OPEN SETTINGS",
                        null,
                        "CANCEL", getContext());
            }
        } else {
            startRecording();
        }
    }

    private void goToSettings() {
        Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getContext().getPackageName()));
        myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
        myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(myAppSettings);
    }

}
