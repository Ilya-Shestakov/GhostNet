package com.example.mapmemories.Lenta;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.mapmemories.R;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.cachemanager.CacheManager;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class Map extends AppCompatActivity {

    private MapView mapView;
    private ExtendedFloatingActionButton fabDownloadMap;
    private ImageButton btnClose;

    private ConstraintLayout mainContentLayout;

    private int minZoom = 10;
    private int maxZoom = 16;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        setContentView(R.layout.activity_map);

        mapView = findViewById(R.id.mapView);
        fabDownloadMap = findViewById(R.id.fabDownloadMap);
        btnClose = findViewById(R.id.btnClose);
        mainContentLayout = findViewById(R.id.mainContentLayout);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        setupMap();
        setupClickListeners();


        if (savedInstanceState == null && getIntent().hasExtra("revealX")) {
            mainContentLayout.setVisibility(View.INVISIBLE);
            ViewTreeObserver viewTreeObserver = mainContentLayout.getViewTreeObserver();
            if (viewTreeObserver.isAlive()) {
                viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mainContentLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        revealActivity(getIntent().getIntExtra("revealX", 0),
                                getIntent().getIntExtra("revealY", 0));
                    }
                });
            }
        }


    }



    private void setupMap() {
        mapView.setTileSource(new XYTileSource("Mapnik", 0, 19, 256, ".png",
                new String[]{"https://a.tile.openstreetmap.org/"}, "© OSM"));

        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);

        IMapController mapController = mapView.getController();
        mapController.setZoom(10.0);
        mapController.setCenter(new GeoPoint(55.7512, 37.6184));
    }

    private void setupClickListeners() {

        btnClose.setOnClickListener(v -> {
            Close();
        });

        fabDownloadMap.setOnClickListener(v -> {
            showDownloadDialog();
        });
    }

    private void showDownloadDialog() {
        BoundingBox currentBox = mapView.getBoundingBox();

        CacheManager cacheManager = new CacheManager(mapView);
        int possibleTiles = cacheManager.possibleTilesInArea(currentBox, minZoom, maxZoom);

        String message = String.format(
                "Будет скачана область, видимая сейчас на экране.\n\n" +
                        "Уровни зума: %d - %d\n" +
                        "Количество тайлов: ~%d\n" +
                        "Примерный размер: ~%.1f МБ",
                minZoom, maxZoom, possibleTiles, (possibleTiles * 20.0) / 1024.0
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Скачать эту область?");
        builder.setMessage(message);

        if (possibleTiles > 10000) {
            builder.setMessage(message + "\n\n⚠️ ВНИМАНИЕ: Область слишком большая! Загрузка займет много времени и памяти. Лучше приблизьте карту.");
            builder.setPositiveButton("Всё равно скачать", (dialog, which) -> startDownload(currentBox));
        } else {
            builder.setPositiveButton("Скачать", (dialog, which) -> startDownload(currentBox));
        }

        builder.setNegativeButton("Отмена", null);

        builder.setNeutralButton("Очистить кэш тут", (dialog, which) -> clearCache(currentBox));

        builder.show();
    }

    private void startDownload(BoundingBox bbox) {
        CacheManager cacheManager = new CacheManager(mapView);

        Toast.makeText(this, "Загрузка началась...", Toast.LENGTH_SHORT).show();
        fabDownloadMap.setEnabled(false);
        fabDownloadMap.setText("Качаем...");

        cacheManager.downloadAreaAsync(this, bbox, minZoom, maxZoom, new CacheManager.CacheManagerCallback() {
            @Override
            public void onTaskComplete() {
                runOnUiThread(() -> {
                    Toast.makeText(Map.this, "Успешно скачано!", Toast.LENGTH_LONG).show();
                    fabDownloadMap.setEnabled(true);
                    fabDownloadMap.setText("Загрузить");
                });
            }

            @Override
            public void onTaskFailed(int errors) {
                runOnUiThread(() -> {
                    Toast.makeText(Map.this, "Ошибка загрузки. Ошибок: " + errors, Toast.LENGTH_SHORT).show();
                    fabDownloadMap.setEnabled(true);
                    fabDownloadMap.setText("Загрузить");
                });
            }

            @Override public void updateProgress(int progress, int currentZoomLevel, int zoomMin, int zoomMax) {}
            @Override public void downloadStarted() {}
            @Override public void setPossibleTilesInArea(int total) {}
        });
    }

    private void clearCache(BoundingBox bbox) {
        CacheManager cacheManager = new CacheManager(mapView);
        cacheManager.cleanAreaAsync(this, bbox, minZoom, maxZoom);
        Toast.makeText(this, "Очистка кэша запущена...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {

        Close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    private void revealActivity(int x, int y) {
        float finalRadius = (float) (Math.max(mainContentLayout.getWidth(), mainContentLayout.getHeight()) * 1.1);
        Animator circularReveal = ViewAnimationUtils.createCircularReveal(mainContentLayout, x, y, 0, finalRadius);
        circularReveal.setDuration(400);
        circularReveal.setInterpolator(new AccelerateInterpolator());
        mainContentLayout.setVisibility(View.VISIBLE);
        circularReveal.start();
    }

    private void unRevealActivity(int x, int y) {
        float finalRadius = (float) (Math.max(mainContentLayout.getWidth(), mainContentLayout.getHeight()) * 1.1);
        Animator circularReveal = ViewAnimationUtils.createCircularReveal(mainContentLayout, x, y, finalRadius, 0);
        circularReveal.setDuration(500);
        circularReveal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mainContentLayout.setVisibility(View.INVISIBLE);
                finish();
                overridePendingTransition(0, 0);
            }
        });
        circularReveal.start();
    }

    public void Close() {
        int revealX = getIntent().getIntExtra("revealX", 0);
        int revealY = getIntent().getIntExtra("revealY", 0);
        unRevealActivity(revealX, revealY);
    }

}