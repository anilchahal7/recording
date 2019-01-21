package in.iceberg.android.activity;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.dmoral.toasty.Toasty;
import in.iceberg.android.apputil.Util;
import in.iceberg.android.db.AppRecordData;
import in.iceberg.android.apputil.TextUtils;

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
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.File;
import java.io.IOException;


public class RecordingActivity extends AppCompatActivity {

    private String output;
    private MediaRecorder mediaRecorder;
    private boolean state, recordingStopped;

    @BindView(R.id.button_start_recording)
    public ImageButton buttonStartRecording;
    @BindView(R.id.button_pause_recording)
    public ImageButton buttonPauseRecording;
    @BindView(R.id.button_stop_recording)
    public ImageButton buttonStopRecording;
    @BindView(R.id.button_play_recording)
    public ImageButton buttonPlayRecording;
    @BindView(R.id.button_delete_recording)
    public ImageButton buttonDeleteRecording;
    @BindView(R.id.recording_image)
    public ImageView recordingImage;
    @BindView(R.id.recording_image_background)
    public ImageView recordingImageBackground;
    @BindView(R.id.adView)
    public AdView mAdView;

    private final int START = 0, STOP = 1;
    private static final int PERMISSIONS_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setButtons(STOP);
        MobileAds.initialize(this, BuildConfig.ADMOB_APP_ID);

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
        buttonPlayRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isNotNullOrEmpty(output)) {
                    playRecording();
                }
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

        mAdView = new AdView(this);
        mAdView.setAdSize(AdSize.SMART_BANNER);
        mAdView.setAdUnitId(BuildConfig.BANNER_HOME_FOOTER);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("61ef3aeee61b7d1b")
                .build();

        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Toast.makeText(getApplicationContext(), "Ad is loaded", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onAdClosed() {
                Toast.makeText(getApplicationContext(), "Ad is closed!", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onAdFailedToLoad(int errorCode) {
                Toast.makeText(getApplicationContext(), "Ad failed to load! error code: " + errorCode, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onAdLeftApplication() {
                Toast.makeText(getApplicationContext(), "Ad left application!", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onAdOpened() {
                super.onAdOpened();
            }
        });
        mAdView.loadAd(adRequest);
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }

    private void startRecording() {
        if (AppRecordData.isRecordPermissionDenied() || AppRecordData.isStoragePermissionDenied()) {
            return;
        }
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
                state = true;
                Toasty.custom(this, getResources().getString(R.string.start_recording_toast_message),
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
            recordingStopped = true;
            setButtons(STOP);
            setImage(R.drawable.ic_microphone, R.color.stopRecording, R.color.stopRecordingBackground);
            Toasty.custom(this, getResources().getString(R.string.stop_recording_toast_message),
                    getResources().getDrawable(R.drawable.ic_stop), getResources().getColor(R.color.stopRecording),
                    Toast.LENGTH_SHORT, true, true).show();

            mediaRecorder.stop();
            mediaRecorder.release();
            state = false;
            resetPauseButton();
        } else {
            Toasty.custom(this, getResources().getString(R.string.stop_recording_toast_message),
                    getResources().getDrawable(R.drawable.ic_stop), getResources().getColor(R.color.stopRecording),
                    Toast.LENGTH_SHORT, true, true).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void pauseRecording() {
        if (state && mediaRecorder != null) {
            if (!recordingStopped) {
                recordingStopped = true;
                setImage(R.drawable.ic_microphone, R.color.pauseRecording, R.color.pauseRecordingBackground);

                Toasty.custom(this, getResources().getString(R.string.pause_recording_toast_message),
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
        recordingStopped = false;
        setImage(R.drawable.ic_microphone, R.color.startRecording, R.color.startRecordingBackground);

        Toasty.custom(this, getResources().getString(R.string.resume_recording_toast_message),
                getResources().getDrawable(R.drawable.ic_mic), getResources().getColor(R.color.startRecording),
                Toast.LENGTH_SHORT, true, true).show();
        mediaRecorder.resume();
        buttonPauseRecording.setImageResource(R.drawable.ic_pause);
    }

    private void setDrawableColor(int indicatorColor) {
        LayerDrawable drawableFile = (LayerDrawable) recordingImageBackground.getBackground().mutate();
        GradientDrawable gradientDrawable = (GradientDrawable) drawableFile.findDrawableByLayerId(R.id.circle_background);
        gradientDrawable.invalidateSelf();
        drawableFile.invalidateSelf();
        gradientDrawable.setColor(indicatorColor);

        if (!recordingStopped) {
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

    private void playRecording() {
        File file = new File(output);
        if (file.exists() && !state) {
            setImage(R.drawable.ic_speaker_icon, R.color.black, R.color.startRecordingBackground);

            MediaPlayer mp = new MediaPlayer();
            try {
                mp.setDataSource(output);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                mp.prepare();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mp.start();
            disableButton(buttonStartRecording);
            disableButton(buttonDeleteRecording);
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    setImage(R.drawable.ic_microphone, R.color.black, R.color.full_transparent);
                    enableButton(buttonStartRecording);
                    enableButton(buttonDeleteRecording);
                }
            });
            Toasty.custom(this, getResources().getString(R.string.play_recording_toast_message),
                    getResources().getDrawable(R.drawable.ic_play), getResources().getColor(R.color.startRecording),
                    Toast.LENGTH_SHORT, true, true).show();
        } else if (state) {
            Toasty.error(this, getResources().getString(R.string.stop_recording_before_toast_message)).show();
        } else {
            Toasty.error(this, getResources().getString(R.string.no_recording_toast_message)).show();
        }
    }

    private void deleteRecording() {
        File file = new File(output);
        if (file.exists() && !state) {
            setImage(R.drawable.ic_microphone, R.color.black, R.color.full_transparent);

            boolean deleted = file.delete();
            if (deleted) {
                Toasty.custom(this, getResources().getString(R.string.deleted_recording_toast_message),
                        getResources().getDrawable(R.drawable.ic_delete), getResources().getColor(R.color.startRecording),
                        Toast.LENGTH_SHORT, true, true).show();
            }
        } else if (state) {
            Toasty.error(this, getResources().getString(R.string.stop_recording_before_toast_message)).show();
        }
    }

    private void setImage(int drawable, int drawableColor, int backgroundColor) {
        recordingImage.setImageDrawable(getResources().getDrawable(drawable));
        recordingImageBackground.setColorFilter(ContextCompat.getColor(this, drawableColor),
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
                disableButton(buttonPlayRecording);
                disableButton(buttonDeleteRecording);
                break;
            case STOP:
                buttonStartRecording.setVisibility(View.VISIBLE);
                buttonPauseRecording.setVisibility(View.GONE);
                disableButton(buttonStopRecording);
                enableButton(buttonPlayRecording);
                enableButton(buttonDeleteRecording);
                break;
        }
    }

    private void disableButton(ImageButton imageButton) {
        imageButton.setEnabled(false);
        imageButton.setColorFilter(ContextCompat.getColor(this, R.color.black_56),
                android.graphics.PorterDuff.Mode.SRC_IN);
        imageButton.setBackground(getResources().getDrawable(R.drawable.white_circle_light_grey_border));
    }

    private void enableButton(ImageButton imageButton) {
        imageButton.setEnabled(true);
        imageButton.setColorFilter(ContextCompat.getColor(this, R.color.black),
                android.graphics.PorterDuff.Mode.SRC_IN);
        imageButton.setBackground(getResources().getDrawable(R.drawable.white_circle_grey_border));
    }

    // App Permissions Allow and Deny ...
    private void handleRecordAndStoragePermissions() {
        if (ContextCompat.checkSelfPermission(RecordingActivity.this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(RecordingActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(RecordingActivity.this,
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
            if (ActivityCompat.shouldShowRequestPermissionRationale(RecordingActivity.this,
                    Manifest.permission.RECORD_AUDIO) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(RecordingActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(RecordingActivity.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE)) {
                String[] permissions = {android.Manifest.permission.RECORD_AUDIO,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(RecordingActivity.this,
                        permissions, PERMISSIONS_REQUEST_CODE);
            } else if (ContextCompat.checkSelfPermission(RecordingActivity.this,
                    Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(RecordingActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                AppRecordData.setStoragePermissionDenied(false);
                AppRecordData.setRecordPermissionDenied(false);
                startRecording();
            } else if ((!ActivityCompat.shouldShowRequestPermissionRationale(RecordingActivity.this,
                    Manifest.permission.RECORD_AUDIO) || AppRecordData.isRecordPermissionDenied()) ||
                    (!ActivityCompat.shouldShowRequestPermissionRationale(RecordingActivity.this,
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
                        "CANCEL", this);
            }
        } else {
            startRecording();
        }
    }

    private void goToSettings() {
        Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + RecordingActivity.this.getPackageName()));
        myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
        myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(myAppSettings);
    }

}
