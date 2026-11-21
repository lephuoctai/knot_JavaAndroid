package com.taile.knot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.taile.knot.data.PrototypeStore;
import com.taile.knot.databinding.ActivityMainBinding;
import com.taile.knot.ui.adapter.TimelineAdapter;
import com.taile.knot.ui.custom.RopeItemDecoration;
import com.taile.knot.ui.viewmodel.MainViewModel;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private PrototypeStore store;
    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
    private boolean reloading;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new PrototypeStore(this);
        if (!store.hasEmail() || store.journey().isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class)); finish(); return;
        }
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        TimelineAdapter adapter = new TimelineAdapter();
        binding.rvTimeline.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTimeline.addItemDecoration(new RopeItemDecoration());
        binding.rvTimeline.setAdapter(adapter);
        binding.tvJourneyCode.setText(getString(R.string.journey_code, store.journey()));
        JourneyActions.bind(this, binding.tvJourneyCode, binding.btnSettings, store, this::reload);
        binding.fabQuickAction.setOnClickListener(v -> {
            int last = adapter.getItemCount() - 1;
            if (last >= 0) binding.rvTimeline.smoothScrollToPosition(last);
        });
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        viewModel.getKnotsLiveData().observe(this, adapter::submitList);
        adapter.setOnAddKnotClickListener(() -> startActivity(new Intent(this, AddKnotActivity.class)));
        adapter.setOnPreviewClick(this::showPreview);
    }
    private void showPreview(com.taile.knot.data.model.Knot knot) {
        android.widget.LinearLayout content = new android.widget.LinearLayout(this);
        content.setOrientation(android.widget.LinearLayout.VERTICAL);
        content.setPadding(24, 12, 24, 24);
        android.widget.ImageView image = new android.widget.ImageView(this);
        image.setAdjustViewBounds(true);
        image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        com.bumptech.glide.Glide.with(this).load(knot.getImageUrl()).into(image);
        content.addView(image, new android.widget.LinearLayout.LayoutParams(-1, -2));
        android.widget.TextView text = new android.widget.TextView(this);
        text.setText(new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                .format(new java.util.Date(knot.getMemoryDate())) + "\n\n" + knot.getDescription());
        text.setTextSize(16);
        text.setPadding(0, 16, 0, 0);
        content.addView(text);
        android.widget.ScrollView scroll=new android.widget.ScrollView(this);
        scroll.addView(content);
        androidx.appcompat.app.AlertDialog dialog=new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(scroll).setPositiveButton("Đóng", null).create();
        dialog.show();
        if(dialog.getWindow()!=null)dialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                (int)(getResources().getDisplayMetrics().heightPixels*0.9f));
    }
    @Override protected void onResume() {
        super.onResume();
        if(viewModel==null||reloading)return;
        executor.execute(() -> {
            try {
                java.util.List<com.taile.knot.data.model.Knot> cached=store.cachedKnots();
                runOnUiThread(() -> {
                    if(isDestroyed()||isFinishing())return;
                    if(cached!=null)viewModel.setKnots(cached);
                    if(cached==null||store.needsRefresh())reload();
                });
            }catch(org.json.JSONException error){runOnUiThread(this::reload);}
        });
    }
    private void reload() {
        if (viewModel == null || reloading) return;
        reloading = true;
        executor.execute(() -> {
            try {
                java.util.List<com.taile.knot.data.model.Knot> knots = store.knots();
                runOnUiThread(() -> {
                    reloading = false;
                    if (!isDestroyed() && !isFinishing()) {
                        viewModel.setKnots(knots);
                        Toast.makeText(this,store.syncNotice,Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (org.json.JSONException | RuntimeException error) {
                runOnUiThread(() -> {
                    reloading = false;
                    if (isDestroyed() || isFinishing()) return;
                    Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                    if (error instanceof PrototypeStore.ApiFailure) {
                        int status = ((PrototypeStore.ApiFailure) error).status;
                        if (status == 401 || status == 404 || status == 410) {
                            viewModel.setKnots(new java.util.ArrayList<>());
                            if (status == 401) store.leaveScreen(); else store.leaveJourney();
                            startActivity(new Intent(this, LoginActivity.class)
                                    .putExtra(LoginActivity.EXTRA_CHOOSE_JOURNEY, status != 401));
                            finish();
                        }
                    }
                });
            }
        });
    }
    @Override protected void onDestroy() { executor.shutdownNow(); super.onDestroy(); }
}
