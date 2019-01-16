package in.iceberg.android.activity;

import butterknife.BindView;
import butterknife.ButterKnife;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

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
    @BindView(R.id.recording_image)
    public ImageView recordingImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

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
                playRecording();
            }
        });
        buttonDeleteRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteRecording();
            }
        });
    }

    private void startRecording() {
        mediaRecorder = new MediaRecorder();
        output = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.mp3";
        if (mediaRecorder != null) {
            recordingImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_microphone_new));
            setDrawableColor(recordingImage, getResources().getColor(R.color.startRecordingBackground));
            recordingImage.setColorFilter(ContextCompat.getColor(this, R.color.startRecording), android.graphics.PorterDuff.Mode.SRC_IN);
//        setDrawableColor(recordingIndicator, getResources().getColor(R.color.startRecording));

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
            }
        }
    }

    private void stopRecording() {
        if (state && mediaRecorder != null) {
            recordingImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_microphone_new));
            setDrawableColor(recordingImage, getResources().getColor(R.color.stopRecordingBackground));
            recordingImage.setColorFilter(ContextCompat.getColor(this, R.color.stopRecording), android.graphics.PorterDuff.Mode.SRC_IN);
//        setDrawableColor(recordingIndicator, getResources().getColor(R.color.stopRecording));

            mediaRecorder.stop();
            mediaRecorder.release();
            state = false;
        } else {
            Toast.makeText(this, getResources().getString(R.string.stop_recording_toast_message), Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void pauseRecording() {
        if (state && mediaRecorder != null) {
            if (!recordingStopped) {
                recordingImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_microphone_new));
                setDrawableColor(recordingImage, getResources().getColor(R.color.pauseRecordingBackground));
                recordingImage.setColorFilter(ContextCompat.getColor(this, R.color.pauseRecording), android.graphics.PorterDuff.Mode.SRC_IN);
//        setDrawableColor(recordingIndicator, getResources().getColor(R.color.pauseRecording));

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
        recordingImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_microphone_new));
        setDrawableColor(recordingImage, getResources().getColor(R.color.startRecordingBackground));
        recordingImage.setColorFilter(ContextCompat.getColor(this, R.color.startRecording), android.graphics.PorterDuff.Mode.SRC_IN);
//        setDrawableColor(recordingIndicator, getResources().getColor(R.color.startRecording));
        Toast.makeText(this,getResources().getString(R.string.resume_recording_toast_message), Toast.LENGTH_SHORT).show();
        mediaRecorder.resume();
        buttonPauseRecording.setText(getResources().getString(R.string.pause));
        recordingStopped = false;
    }

    private void setDrawableColor(ImageView imageView, int indicatorColor) {
        LayerDrawable drawableFile = (LayerDrawable) imageView.getBackground().mutate();
        GradientDrawable gradientDrawable = (GradientDrawable) drawableFile.findDrawableByLayerId(R.id.circle_background);
        gradientDrawable.invalidateSelf();
        drawableFile.invalidateSelf();
        gradientDrawable.setColor(indicatorColor);
    }

    private void playRecording() {
        File file = new File(output);
        if (file.exists()) {
            recordingImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_speaker_icon));
            setDrawableColor(recordingImage, getResources().getColor(R.color.full_transparent));
            recordingImage.setColorFilter(ContextCompat.getColor(this, R.color.black), android.graphics.PorterDuff.Mode.SRC_IN);
            setDrawableColor(recordingIndicator, getResources().getColor(R.color.full_transparent));

            MediaPlayer mp = new MediaPlayer();
            try {
                mp.setDataSource(output);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                mp.prepare();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mp.start();
        } else {
            Toast.makeText(this,"Recording Doesn't exist", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteRecording() {
        recordingImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_microphone));
        setDrawableColor(recordingIndicator, getResources().getColor(R.color.stopRecording));
        File file = new File(output);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                Toast.makeText(this,"Recording has been Deleted", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
