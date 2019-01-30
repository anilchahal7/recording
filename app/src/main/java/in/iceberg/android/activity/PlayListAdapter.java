package in.iceberg.android.activity;

import android.content.Context;
import android.content.Intent;
import android.iceberg.in.recording.R;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import in.iceberg.android.apputil.TextUtils;
import in.iceberg.android.interfaces.PlayBackRowClickListener;

public class PlayListAdapter extends ArrayAdapter<String> {

    private List<String> fileNameList;
    private List<String> filePathList;
    private PlayBackRowClickListener playBackRowClickListener;

    public PlayListAdapter(Context context, List<String> fileNameList, List<String> filePathList, PlayBackRowClickListener playBackRowClickListener) {
        super(context, 0, fileNameList);
        this.fileNameList = fileNameList;
        this.filePathList = filePathList;
        this.playBackRowClickListener = playBackRowClickListener;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        String user = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_playlist_row, parent, false);
        }
        // Lookup view for data population
        TextView fileName = (TextView) convertView.findViewById(R.id.audio_name);
        ImageButton shareButton = (ImageButton) convertView.findViewById(R.id.share_recording);
        fileName.setText(fileNameList.get(position));
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String filePath = filePathList.get(position);
                if (TextUtils.isNotNullOrEmpty(filePath)) {
                    shareRecording(filePath);
                }
            }
        });

        fileName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playBackRowClickListener.playbackRowClick(filePathList.get(position));
            }
        });

        // Populate the data into the template view using the data object
        // Return the completed view to render on screen
        return convertView;
    }

    private void shareRecording(String filePath) {
        if (TextUtils.isNotNullOrEmpty(filePath)) {
            File file = new File(filePath);
            if (file.exists()) {
                Uri uri = Uri.parse(filePath);
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("audio/*");
                share.putExtra(Intent.EXTRA_STREAM, uri);
                getContext().startActivity(Intent.createChooser(share, "Share Sound File"));
            }
        }
    }
}