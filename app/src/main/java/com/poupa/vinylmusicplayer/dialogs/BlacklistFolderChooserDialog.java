package com.poupa.vinylmusicplayer.dialogs;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.poupa.vinylmusicplayer.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Aidan Follestad (afollestad), modified by Karim Abou Zeid
 */
public class BlacklistFolderChooserDialog extends DialogFragment implements MaterialDialog.ListCallback {

    private File parentFolder;
    private File[] parentContents;
    private boolean canGoUp = false;

    private FolderCallback callback;

    final String initialPath = Environment.getExternalStorageDirectory().getAbsolutePath();

    private String[] getContentsArray() {
        if (parentContents == null) {
            if (canGoUp) {
                return new String[]{".."};
            }
            return new String[]{};
        }
        String[] results = new String[parentContents.length + (canGoUp ? 1 : 0)];
        if (canGoUp) {
            results[0] = "..";
        }
        for (int i = 0; i < parentContents.length; i++) {
            results[canGoUp ? i + 1 : i] = parentContents[i].getName();
        }
        return results;
    }

    private File[] listFiles() {
        File[] contents = parentFolder.listFiles();
        List<File> results = new ArrayList<>();
        if (contents != null) {
            for (File fi : contents) {
                if (fi.isDirectory()) {
                    results.add(fi);
                }
            }
            Collections.sort(results, new FolderSorter());
            return results.toArray(new File[0]);
        }
        return null;
    }

    public static BlacklistFolderChooserDialog create() {
        return new BlacklistFolderChooserDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = requireActivity();
        if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                return new MaterialDialog.Builder(activity)
                        .title(R.string.md_error_label)
                        .content(R.string.android13_storage_perm_error)
                        .positiveText(android.R.string.ok)
                        .build();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ActivityCompat.checkSelfPermission(
                activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
                return new MaterialDialog.Builder(activity)
                        .title(R.string.md_error_label)
                        .content(R.string.md_storage_perm_error)
                        .positiveText(android.R.string.ok)
                        .build();
        }
        if (savedInstanceState == null) {
            savedInstanceState = new Bundle();
        }
        if (!savedInstanceState.containsKey("current_path")) {
            savedInstanceState.putString("current_path", initialPath);
        }
        parentFolder = new File(savedInstanceState.getString("current_path", "/"));
        checkIfCanGoUp();
        parentContents = listFiles();
        MaterialDialog.Builder builder =
                new MaterialDialog.Builder(activity)
                        .title(parentFolder.getAbsolutePath())
                        .items(getContentsArray())
                        .itemsCallback(this)
                        .autoDismiss(false)
                        .onPositive((dialog, which) -> {
                            dismiss();
                            callback.onFolderSelection(BlacklistFolderChooserDialog.this, parentFolder);
                        })
                        .onNegative((materialDialog, dialogAction) -> dismiss())
                        .positiveText(R.string.add_action)
                        .negativeText(android.R.string.cancel);
        return builder.build();
    }

    @Override
    public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence s) {
        if (canGoUp && i == 0) {
            parentFolder = parentFolder.getParentFile();
            if (parentFolder.getAbsolutePath().equals("/storage/emulated")) {
                parentFolder = parentFolder.getParentFile();
            }
            canGoUp = parentFolder.getParent() != null;
        } else {
            parentFolder = parentContents[canGoUp ? i - 1 : i];
            canGoUp = true;
            if (parentFolder.getAbsolutePath().equals("/storage/emulated")) {
                parentFolder = Environment.getExternalStorageDirectory();
            }
        }
        reload();
    }

    private void checkIfCanGoUp() {
        try {
            canGoUp = parentFolder.getPath().split("/").length > 1;
        } catch (IndexOutOfBoundsException e) {
            canGoUp = false;
        }
    }

    private void reload() {
        parentContents = listFiles();
        MaterialDialog dialog = (MaterialDialog) getDialog();
        dialog.setTitle(parentFolder.getAbsolutePath());
        dialog.setItems(getContentsArray());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("current_path", parentFolder.getAbsolutePath());
    }

    public void setCallback(FolderCallback callback) {
        this.callback = callback;
    }

    public interface FolderCallback {
        void onFolderSelection(@NonNull BlacklistFolderChooserDialog dialog, @NonNull File folder);
    }

    static class FolderSorter implements Comparator<File> {

        @Override
        public int compare(File lhs, File rhs) {
            return lhs.getName().compareTo(rhs.getName());
        }
    }
}
