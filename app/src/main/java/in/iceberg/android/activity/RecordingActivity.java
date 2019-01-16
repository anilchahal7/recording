package in.iceberg.android.activity;

import butterknife.BindView;
import butterknife.ButterKnife;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
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
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
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
    @BindView(R.id.recording_indicator)
    public ImageView recordingIndicator;
    @BindView(R.id.adView)
    public AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        MobileAds.initialize(this, Constants.ADMOB_APP_ID);

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

        mAdView.setAdSize(AdSize.BANNER);
        mAdView.setAdUnitId(getString(R.string.banner_home_footer));
        AdRequest adRequest = new AdRequest.Builder()
                // Check the LogCat to get your test device ID
                .addTestDevice("f7c1a9d3a898f3c1")
                .build();
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
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
        setIndicatorColor(getResources().getColor(R.color.startRecording), true);
        mediaRecorder = new MediaRecorder();
        output = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.mp3";
        if (mediaRecorder != null) {
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
        setIndicatorColor(getResources().getColor(R.color.stopRecording), false);
        if (state && mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            state = false;
        } else {
            Toast.makeText(this, getResources().getString(R.string.stop_recording_toast_message),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void pauseRecording() {
        setIndicatorColor(getResources().getColor(R.color.pauseRecording), false);
        if (state && mediaRecorder != null) {
            if (!recordingStopped) {
                Toast.makeText(this,getResources().getString(R.string.pause_recording_toast_message),
                        Toast.LENGTH_SHORT).show();
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
        setIndicatorColor(getResources().getColor(R.color.startRecording), true);
        Toast.makeText(this,getResources().getString(R.string.resume_recording_toast_message),
                Toast.LENGTH_SHORT).show();
        mediaRecorder.resume();
        buttonPauseRecording.setText(getResources().getString(R.string.pause));
        recordingStopped = false;
    }

    private void setIndicatorColor(int indicatorColor, boolean animate) {
        LayerDrawable drawableFile = (LayerDrawable) recordingIndicator.getBackground().mutate();
        GradientDrawable gradientDrawable = (GradientDrawable) drawableFile.findDrawableByLayerId(R.id.circle_background);
        gradientDrawable.invalidateSelf();
        drawableFile.invalidateSelf();
        gradientDrawable.setColor(indicatorColor);

        if (animate) {
            Animation animation = new AlphaAnimation(1, 0);
            animation.setDuration(200);
            animation.setInterpolator(new LinearInterpolator());
            animation.setRepeatCount(Animation.INFINITE);
            animation.setRepeatMode(Animation.REVERSE);
            recordingIndicator.startAnimation(animation);
        } else {
            recordingIndicator.clearAnimation();
        }
    }

    private void playRecording() {
        File file = new File(output);
        if (file.exists()) {
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
        } else {
            Toast.makeText(this,"Recording Doesn't exist", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteRecording() {
        File file = new File(output);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                Toast.makeText(this,"Recording has been Deleted", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
