package in.iceberg.android.activity;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.iceberg.in.recording.R;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.dmoral.toasty.Toasty;
import in.iceberg.android.apputil.TextUtils;
import in.iceberg.android.interfaces.PlayBackRowClickListener;

public class PlayFragment extends Fragment implements PlayBackRowClickListener {

    private String title;
    private int page;

    private String output;
    private MediaRecorder mediaRecorder;
    private boolean state, recordingStopped, playbackStopped;

    @BindView(R.id.recording_image)
    public ImageView recordingImage;
    @BindView(R.id.recording_image_background)
    public ImageView recordingImageBackground;
    private MediaPlayer mediaPlayer;
    @BindView(R.id.playlist)
    public ListView playlist;

    private final int START = 0, STOP = 1;
    private static final int PERMISSIONS_REQUEST_CODE = 1001;
    private PlayListAdapter adapter;
    private final String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/RecordingApp";
    private ArrayList<HashMap<String, String>> songList;

    public static PlayFragment newInstance(int page, String title) {
        PlayFragment fragmentFirst = new PlayFragment();
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

        ((HomepageActivity)getActivity()).setFragmentRefreshListener(new HomepageActivity.FragmentRefreshListener() {
            @Override
            public void onRefresh() {
                ArrayList<HashMap<String, String>> songListNew = getPlayList(path);
                if (songListNew != null && !songListNew.equals(songList)) {
                    songList.clear();
                    songList.addAll(songListNew);
                    List<String> fileNameList;
                    List<String> filePathList;
                    if (songList != null) {
                        fileNameList = new ArrayList<>();
                        filePathList = new ArrayList<>();
                        for (int i = 0; i < songList.size(); i++) {
                            fileNameList.add(songList.get(i).get("file_name"));
                            filePathList.add(songList.get(i).get("file_path"));
                        }
                        if (fileNameList.size() > 0) {
                            adapter.setFileNameList(fileNameList);
                            adapter.setFilePathList(filePathList);
                            adapter.notifyDataSetChanged();
                        }
                    }
                }
            }
        });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_second, container, false);
        ButterKnife.bind(this, view.getRootView());

        mediaPlayer = new MediaPlayer();
        setPlayList();

        return view;
    }

    private void playRecording(String filePath) {
        if (mediaPlayer.isPlaying()) {
            recordingStopped = true;
            playbackStopped = true;
            mediaPlayer.stop();
//            buttonPlayRecording.setImageResource(R.drawable.ic_play);
            setImage(R.drawable.ic_speaker_icon, R.color.black, R.color.full_transparent);
            Toasty.custom(getContext(), getResources().getString(R.string.stop_playback_toast_message),
                    getResources().getDrawable(R.drawable.ic_stop), getResources().getColor(R.color.stopRecording),
                    Toast.LENGTH_SHORT, true, true).show();
        } else {
            recordingStopped = true;
            playbackStopped = false;
            File file = new File(filePath);
            if (file.exists() && !state) {
                setImage(R.drawable.ic_speaker_icon, R.color.black, R.color.startRecordingBackground);
                try {
                    mediaPlayer.setDataSource(filePath);
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
                    mediaPlayer.prepare();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mediaPlayer.start();
//                buttonPlayRecording.setImageResource(R.drawable.ic_stop);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        setImage(R.drawable.ic_microphone, R.color.black, R.color.full_transparent);
//                        buttonPlayRecording.setImageResource(R.drawable.ic_play);
                    }
                });
                Toasty.custom(getContext(), getResources().getString(R.string.play_recording_toast_message),
                        getResources().getDrawable(R.drawable.ic_play), getResources().getColor(R.color.startRecording),
                        Toast.LENGTH_SHORT, true, true).show();
            } else if (state) {
                Toasty.error(getContext(), getResources().getString(R.string.stop_recording_before_toast_message)).show();
            } else {
                Toasty.error(getContext(), getResources().getString(R.string.no_recording_toast_message)).show();
            }
        }

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

    private void setImage(int drawable, int drawableColor, int backgroundColor) {
        recordingImage.setImageDrawable(getResources().getDrawable(drawable));
        recordingImageBackground.setColorFilter(ContextCompat.getColor(getContext(), drawableColor),
                android.graphics.PorterDuff.Mode.SRC_IN);
        setDrawableColor(getResources().getColor(backgroundColor));
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

    private void goToSettings() {
        Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getContext().getPackageName()));
        myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
        myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(myAppSettings);
    }

    public void setPlayList() {
        songList = getPlayList(path);
        List<String> fileNameList;
        List<String> filePathList;
        if (songList != null) {
            fileNameList = new ArrayList<>();
            filePathList = new ArrayList<>();
            for (int i = 0; i < songList.size(); i++) {
                fileNameList.add(songList.get(i).get("file_name"));
                filePathList.add(songList.get(i).get("file_path"));
            }
            if (fileNameList.size() > 0) {
                adapter = new PlayListAdapter(getContext(), fileNameList, filePathList, this);
                playlist.setAdapter(adapter);
            }
        }
    }

    ArrayList<HashMap<String, String>> getPlayList(String rootPath) {
        ArrayList<HashMap<String, String>> fileList = new ArrayList<>();
        try {
            File rootFolder = new File(rootPath);
            File[] files = rootFolder.listFiles(); //here you will get NPE if directory doesn't contains  any file,handle it like this.
            for (File file : files) {
                if (file.getName().endsWith(".mp3")) {
                    HashMap<String, String> song = new HashMap<>();
                    song.put("file_path", file.getAbsolutePath());
                    song.put("file_name", file.getName());
                    fileList.add(song);
                }
            }
            return fileList;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void playbackRowClick(String pathName) {
        playRecording(pathName);
    }
}
