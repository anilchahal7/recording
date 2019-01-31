package in.iceberg.android.activity;

import android.content.Context;
import android.content.Intent;
import android.iceberg.in.recording.R;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import es.dmoral.toasty.Toasty;
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
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_playlist_row, parent, false);
        }

        TextView fileName = convertView.findViewById(R.id.audio_name);
        fileName.setText(getItem(position));
        fileName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playBackRowClickListener.playbackRowClick(filePathList.get(position));
            }
        });

        ImageButton overflowButton = convertView.findViewById(R.id.overflow);
        overflowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu(v, position);
            }
        });

        return convertView;
    }

    public void showMenu(View v, final int position) {
        PopupMenu popup = new PopupMenu(getContext(), v);

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.share:
                        String filePath = filePathList.get(position);
                        if (TextUtils.isNotNullOrEmpty(filePath)) {
                            shareRecording(filePath);
                        }
                        return true;
                    case R.id.delete:
                        deleteRecording(filePathList.get(position), position);
                        notifyDataSetChanged();
                        return true;
                    default:
                        return false;
                }
            }
        });
        popup.inflate(R.menu.playback_item_menu);
        popup.show();
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

    private void deleteRecording(String pathName, int position) {
        File file = new File(pathName);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                fileNameList.remove(position);
                filePathList.remove(position);
                Toasty.custom(getContext(), getContext().getString(R.string.deleted_recording_toast_message),
                        getContext().getResources().getDrawable(R.drawable.ic_delete), getContext().getResources().getColor(R.color.startRecording),
                        Toast.LENGTH_SHORT, true, true).show();
            }
        }
    }

    @Override
    public String getItem(int position) {
        return fileNameList.get(position);
    }

    @Override
    public int getCount() {
        return fileNameList.size();
    }

    public void setFileNameList(List<String> fileNameList) {
        this.fileNameList = fileNameList;
    }

    public void setFilePathList(List<String> filePathList) {
        this.filePathList = filePathList;
    }
}