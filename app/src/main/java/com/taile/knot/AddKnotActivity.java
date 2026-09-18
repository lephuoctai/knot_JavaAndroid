package com.taile.knot;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.taile.knot.databinding.ActivityAddKnotBinding;
import com.taile.knot.ui.viewmodel.AddKnotViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddKnotActivity extends AppCompatActivity {

    private ActivityAddKnotBinding binding;
    private AddKnotViewModel viewModel;

    private Uri selectedImageUri = null;
    private long selectedTimestamp = System.currentTimeMillis();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // Bộ chọn ảnh từ bộ sưu tập máy
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    binding.polaroidPreview.setImageURI(uri);
                    binding.tvAddPhotoHint.setVisibility(View.GONE); // Ẩn chữ "+ Ảnh"
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityAddKnotBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutContainer, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(AddKnotViewModel.class);

        setupInitialState();
        setupListeners();
        observeViewModel();
    }

    private void setupInitialState() {
        // Mặc định chọn ngày hiện tại
        binding.tvSelectedDate.setText(dateFormat.format(new Date(selectedTimestamp)));
    }

    private void setupListeners() {
        // 1. Chạm vào Polaroid để chọn ảnh
        binding.layoutImagePicker.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // 2. Mở MaterialDatePicker
        binding.btnDatePicker.setOnClickListener(v -> showDatePicker());

        // 3. Nhấn nút buộc chặt
        binding.btnSubmit.setOnClickListener(v -> {
            String note = binding.etDescription.getText().toString();
            viewModel.createKnot(selectedImageUri, note, selectedTimestamp);
        });
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Chọn thời điểm kỷ niệm")
                .setSelection(selectedTimestamp)
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                selectedTimestamp = selection;

                // Tự động dùng múi giờ và Locale mặc định của OS
                SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                binding.tvSelectedDate.setText(displayFormat.format(new Date(selection)));
            }
        });

        datePicker.show(getSupportFragmentManager(), "KNOT_DATE_PICKER");
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnSubmit.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
            binding.btnSubmit.setEnabled(!isLoading);
        });

        viewModel.getIsSuccess().observe(this, isSuccess -> {
            if (Boolean.TRUE.equals(isSuccess)) {
                Toast.makeText(this, "Đã buộc chặt một nút thắt mới!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}