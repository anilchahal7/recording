package in.iceberg.android.activity;

import butterknife.BindView;
import butterknife.ButterKnife;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.iceberg.in.recording.BuildConfig;
import android.iceberg.in.recording.R;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
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
    private boolean state;
    private boolean recordingStopped;

    @BindView(R.id.button_start_recording)
    public Button buttonStartRecording;
    @BindView(R.id.button_pause_recording)
    public Button buttonPauseRecording;
    @BindView(R.id.button_stop_recording)
    public Button buttonStopRecording;
    @BindView(R.id.button_play_recording)
    public Button buttonPlayRecording;
    @BindView(R.id.button_delete_recording)
    public Button buttonDeleteRecording;
    @BindView(R.id.recording_image)
    public ImageView recordingImage;
    @BindView(R.id.recording_image_background)
    public ImageView recordingImageBackground;
    @BindView(R.id.adView)
    public AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        MobileAds.initialize(this, BuildConfig.ADMOB_APP_ID);
        setButtons(state);

        buttonStartRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(RecordingActivity.this,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(RecordingActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    String[] permissions = {android.Manifest.permission.RECORD_AUDIO,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE};
                    ActivityCompat.requestPermissions(RecordingActivity.this, permissions, 0);
                } else {
                    startRecording();
                }
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
        mediaRecorder = new MediaRecorder();
        output = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.mp3";
        if (mediaRecorder != null) {
            setButtons(true);
            setImage(R.drawable.ic_microphone_new, R.color.startRecording, R.color.startRecordingBackground);

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(output);
            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
                state = true;
                Toast.makeText(RecordingActivity.this,
                        getResources().getString(R.string.start_recording_toast_message), Toast.LENGTH_SHORT).show();
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
            setButtons(false);
            setImage(R.drawable.ic_microphone_new, R.color.stopRecording, R.color.stopRecordingBackground);

            mediaRecorder.stop();
            mediaRecorder.release();
            state = false;
            resetPauseButton();
        } else {
            Toast.makeText(this, getResources().getString(R.string.stop_recording_toast_message),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void pauseRecording() {
        if (state && mediaRecorder != null) {
            if (!recordingStopped) {
                setImage(R.drawable.ic_microphone_new, R.color.pauseRecording, R.color.pauseRecordingBackground);

                Toast.makeText(this,getResources().getString(R.string.pause_recording_toast_message), Toast.LENGTH_SHORT).show();
                mediaRecorder.pause();
                recordingStopped = true;
                buttonPauseRecording.setText(getResources().getString(R.string.resume));
            } else {
                resumeRecording();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private void resumeRecording() {
        setImage(R.drawable.ic_microphone_new, R.color.startRecording, R.color.startRecordingBackground);

        Toast.makeText(this,getResources().getString(R.string.resume_recording_toast_message), Toast.LENGTH_SHORT).show();
        mediaRecorder.resume();
        buttonPauseRecording.setText(getResources().getString(R.string.pause));
        recordingStopped = false;
    }

    private void setDrawableColor(int indicatorColor, boolean animate) {
        LayerDrawable drawableFile = (LayerDrawable) recordingImageBackground.getBackground().mutate();
        GradientDrawable gradientDrawable = (GradientDrawable) drawableFile.findDrawableByLayerId(R.id.circle_background);
        gradientDrawable.invalidateSelf();
        drawableFile.invalidateSelf();
        gradientDrawable.setColor(indicatorColor);

        if (!state) {
            Animation animation = new ScaleAnimation(1, 1.2f, 1, 1.2f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
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
            setImage(R.drawable.ic_speaker_icon, R.color.black, R.color.full_transparent);

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
        } else if (state) {
            Toast.makeText(this,"Stop recording before playing audio", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this,"Recording Doesn't exist", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteRecording() {
        File file = new File(output);
        if (file.exists() && !state) {
            setImage(R.drawable.ic_microphone_new, R.color.black, R.color.full_transparent);

            boolean deleted = file.delete();
            if (deleted) {
                Toast.makeText(this,"Recording has been Deleted", Toast.LENGTH_SHORT).show();
            }
        } else if (state) {
            Toast.makeText(this,"Stop recording before playing audio", Toast.LENGTH_SHORT).show();
        }
    }

    private void setImage(int drawable, int drawableColor, int backgroundColor) {
        recordingImage.setImageDrawable(getResources().getDrawable(drawable));
//        recordingImage.setColorFilter(ContextCompat.getColor(this, drawableColor), android.graphics.PorterDuff.Mode.SRC_IN);
        recordingImageBackground.setColorFilter(ContextCompat.getColor(this, drawableColor), android.graphics.PorterDuff.Mode.SRC_IN);
        setDrawableColor(getResources().getColor(backgroundColor), true);
    }

    private void resetPauseButton() {
        recordingStopped = false;
        buttonPauseRecording.setText(getResources().getString(R.string.pause));
    }

    private void setButtons(boolean isStarted) {
        if (isStarted) {
            buttonStartRecording.setEnabled(false);
            buttonPauseRecording.setEnabled(true);
            buttonStopRecording.setEnabled(true);
            buttonPlayRecording.setEnabled(false);
            buttonDeleteRecording.setEnabled(false);
        } else {
            buttonStartRecording.setEnabled(true);
            buttonPauseRecording.setEnabled(false);
            buttonStopRecording.setEnabled(false);
            buttonPlayRecording.setEnabled(true);
            buttonDeleteRecording.setEnabled(true);
        }
    }
}
