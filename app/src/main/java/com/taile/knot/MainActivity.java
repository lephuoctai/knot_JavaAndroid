package com.taile.knot;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.taile.knot.databinding.ActivityMainBinding;
import com.taile.knot.ui.adapter.TimelineAdapter;
import com.taile.knot.ui.custom.RopeItemDecoration;
import com.taile.knot.ui.viewmodel.MainViewModel;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private TimelineAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Cấu hình RecyclerView
        adapter = new TimelineAdapter();
        binding.rvTimeline.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTimeline.addItemDecoration(new RopeItemDecoration());
        binding.rvTimeline.setAdapter(adapter);

        // 2. Khởi tạo ViewModel & Observe LiveData
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        viewModel.getKnotsLiveData().observe(this, knots -> {
            if (knots != null) {
                adapter.submitList(knots);
            }
        });

        // 3. Nạp mock data để kiểm tra UI
        viewModel.loadMockKnots();
    }
}