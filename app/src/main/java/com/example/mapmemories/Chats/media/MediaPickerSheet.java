package com.example.mapmemories.Chats.media;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mapmemories.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MediaPickerSheet extends BottomSheetDialogFragment {

    private static final int PERMISSION_REQUEST_CODE = 101;
    private RecyclerView recyclerView;
    private MediaAdapter adapter;
    private List<Uri> mediaUris = new ArrayList<>();
    private Set<Uri> selectedUris = new HashSet<>();
    private MediaPickerListener listener;
    private TextView btnAttach;
    private ProgressBar progressBar;
    private ImageView btnCamera;

    private Uri currentCameraUri;

    public interface MediaPickerListener {
        void onMediaSelected(List<Uri> uris);
    }

    public void setMediaPickerListener(MediaPickerListener listener) {
        this.listener = listener;
    }

    // Лаунчер для быстрой камеры
    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
                if (result && currentCameraUri != null) {
                    // Добавляем новое фото в выбранные
                    selectedUris.add(currentCameraUri);
                    // Вставляем в начало списка медиа (или можно просто добавить в адаптер)
                    mediaUris.add(0, currentCameraUri);
                    adapter.notifyItemInserted(0);
                    recyclerView.scrollToPosition(0);
                    updateAttachButton();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_media_picker, container, false);
        recyclerView = view.findViewById(R.id.recyclerMedia);
        btnAttach = view.findViewById(R.id.btnAttach);
        progressBar = view.findViewById(R.id.progressBar);
        btnCamera = view.findViewById(R.id.btnCamera);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        adapter = new MediaAdapter();
        recyclerView.setAdapter(adapter);

        btnAttach.setOnClickListener(v -> {
            if (selectedUris.isEmpty()) {
                Toast.makeText(getContext(), "Выберите медиа", Toast.LENGTH_SHORT).show();
                return;
            }
            if (listener != null) {
                listener.onMediaSelected(new ArrayList<>(selectedUris));
            }
            dismiss();
        });

        btnCamera.setOnClickListener(v -> openCamera());

        // Изначально кнопка скрыта
        btnAttach.setVisibility(View.GONE);
        updateAttachButton();

        if (hasMediaPermission()) {
            loadMedia();
        } else {
            requestMediaPermission();
        }

        return view;
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireContext().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(getContext(), "Ошибка создания файла", Toast.LENGTH_SHORT).show();
                return;
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(requireContext(),
                        "com.example.mapmemories.fileprovider",
                        photoFile);
                currentCameraUri = photoURI;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                takePictureLauncher.launch(photoURI);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void updateAttachButton() {
        int count = selectedUris.size();
        if (count > 0) {
            btnAttach.setText("Прикрепить (" + count + ")");
            btnAttach.setBackgroundResource(R.drawable.bg_attach_btn_active);
            btnAttach.setTextColor(android.graphics.Color.WHITE);
            btnAttach.setEnabled(true);
            btnAttach.setAlpha(1f);
            if (btnAttach.getVisibility() != View.VISIBLE) {
                btnAttach.setVisibility(View.VISIBLE);
                btnAttach.setTranslationY(100f);
                btnAttach.animate().translationY(0f).setDuration(250).start();
            }
        } else {
            if (btnAttach.getVisibility() == View.VISIBLE) {
                btnAttach.animate().translationY(100f).setDuration(200).withEndAction(() -> {
                    btnAttach.setVisibility(View.GONE);
                    btnAttach.setTranslationY(0f);
                }).start();
            }
            btnAttach.setText("Прикрепить");
            btnAttach.setBackgroundResource(R.drawable.bg_attach_btn_inactive);
            btnAttach.setTextColor(android.graphics.Color.parseColor("#999999"));
            btnAttach.setEnabled(false);
        }
    }

    private boolean hasMediaPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestMediaPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO}, PERMISSION_REQUEST_CODE);
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMedia();
            } else {
                Toast.makeText(getContext(), "Нет доступа к медиа", Toast.LENGTH_SHORT).show();
                dismiss();
            }
        }
    }

    private void loadMedia() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        new Thread(() -> {
            List<MediaItem> items = new ArrayList<>();
            String[] projection = {MediaStore.MediaColumns._ID, MediaStore.MediaColumns.MIME_TYPE};
            Uri collection = MediaStore.Files.getContentUri("external");
            String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + " IN ("
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + ","
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO + ")";
            String sortOrder = MediaStore.MediaColumns.DATE_ADDED + " DESC";

            try (Cursor cursor = requireContext().getContentResolver().query(collection, projection, selection, null, sortOrder)) {
                if (cursor != null) {
                    int idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
                    int mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE);
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idColumn);
                        String mimeType = cursor.getString(mimeColumn);
                        Uri contentUri = Uri.withAppendedPath(MediaStore.Files.getContentUri("external"), String.valueOf(id));
                        boolean isVideo = mimeType != null && mimeType.startsWith("video/");
                        items.add(new MediaItem(contentUri, isVideo));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            requireActivity().runOnUiThread(() -> {
                mediaUris.clear();
                for (MediaItem item : items) {
                    mediaUris.add(item.uri);
                }
                adapter.setMediaItems(items);
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                updateAttachButton();
            });
        }).start();
    }

    private static class MediaItem {
        Uri uri;
        boolean isVideo;

        MediaItem(Uri uri, boolean isVideo) {
            this.uri = uri;
            this.isVideo = isVideo;
        }
    }

    private class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.ViewHolder> {

        private List<MediaItem> mediaItems = new ArrayList<>();

        void setMediaItems(List<MediaItem> items) {
            this.mediaItems = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media_picker, parent, false);
            return new ViewHolder(item);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MediaItem mediaItem = mediaItems.get(position);
            Uri uri = mediaItem.uri;

            Glide.with(holder.imageView.getContext())
                    .load(uri)
                    .centerCrop()
                    .into(holder.imageView);

            holder.videoIcon.setVisibility(mediaItem.isVideo ? View.VISIBLE : View.GONE);

            boolean isSelected = selectedUris.contains(uri);
            holder.checkBox.setChecked(isSelected);
            holder.overlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            holder.itemView.setOnClickListener(v -> {
                if (isSelected) {
                    selectedUris.remove(uri);
                } else {
                    selectedUris.add(uri);
                }
                notifyItemChanged(position);
                updateAttachButton();
            });
        }

        @Override
        public int getItemCount() {
            return mediaItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            CheckBox checkBox;
            View overlay;
            ImageView videoIcon;

            ViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.mediaThumb);
                checkBox = itemView.findViewById(R.id.checkbox);
                overlay = itemView.findViewById(R.id.overlay);
                videoIcon = itemView.findViewById(R.id.videoIcon);
            }
        }
    }
}