package com.taile.knot;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
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
    private final ActivityResultLauncher<String[]> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(uri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException error) {
                        Toast.makeText(this, R.string.save_failed, Toast.LENGTH_LONG).show();
                        return;
                    }
                    selectedImageUri = uri;
                    binding.polaroidPreview.setImageURI(uri);
                    binding.tvAddPhotoHint.setText(R.string.photo_remove);
                    binding.tvAddPhotoHint.setTextColor(getColor(R.color.pink600));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityAddKnotBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        com.taile.knot.data.PrototypeStore store = new com.taile.knot.data.PrototypeStore(this);
        binding.btnBack.setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(v.getPaddingLeft(), safe.top, v.getPaddingRight(), safe.bottom);
            if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                // Insets own the keyboard space; adjustResize would apply it twice.
                binding.getRoot().post(() -> binding.getRoot().scrollTo(0, binding.layoutMessage.getTop()));
            }
            return insets;
        });
        binding.etDescription.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) {}
            public void afterTextChanged(android.text.Editable text) {
                binding.etDescription.post(() -> {
                    int line=binding.etDescription.getLayout()==null?0:binding.etDescription.getLayout()
                            .getLineForOffset(Math.max(0,binding.etDescription.getSelectionStart()));
                    int top=binding.etDescription.getLayout()==null?0:binding.etDescription.getLayout().getLineTop(line);
                    binding.etDescription.requestRectangleOnScreen(new android.graphics.Rect(0,top,
                            binding.etDescription.getWidth(),top+binding.etDescription.getLineHeight()+24),false);
                });
            }
        });

        viewModel = new ViewModelProvider(this).get(AddKnotViewModel.class);

        setupInitialState();
        if (savedInstanceState != null) {
            selectedTimestamp = savedInstanceState.getLong("date", selectedTimestamp);
            if(savedInstanceState.getBoolean("dateSelected",false))
                binding.tvSelectedDate.setText(dateFormat.format(new Date(selectedTimestamp)));
            String image = savedInstanceState.getString("image");
            if (image != null) {
                selectedImageUri = Uri.parse(image);
                binding.polaroidPreview.setImageURI(selectedImageUri);
                binding.tvAddPhotoHint.setText(R.string.photo_remove);
                binding.tvAddPhotoHint.setTextColor(getColor(R.color.pink600));
            }
        }
        setupListeners();
        observeViewModel();
    }

    private void setupInitialState() {
        // Mặc định chọn ngày hiện tại
        binding.tvSelectedDate.setText("");
    }

    private void setupListeners() {
        // 1. Chạm vào Polaroid để chọn ảnh
        binding.layoutImagePicker.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                new AlertDialog.Builder(this).setMessage("Gỡ ảnh đã chọn?")
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Gỡ ảnh", (dialog, which) -> {
                            selectedImageUri = null;
                            binding.polaroidPreview.setImageResource(R.drawable.sample11);
                            binding.tvAddPhotoHint.setText(R.string.photo_add);
                            binding.tvAddPhotoHint.setTextColor(getColor(R.color.white));
                        }).show();
            } else {
                imagePickerLauncher.launch(new String[]{"image/*"});
            }
        });

        // 2. Mở MaterialDatePicker
        binding.btnDatePicker.setOnClickListener(v -> showDatePicker());

        // 3. Nhấn nút buộc chặt
        binding.btnSubmit.setOnClickListener(v -> {
            String note = binding.etDescription.getText().toString();
            viewModel.createKnot(new com.taile.knot.data.PrototypeStore(this),
                    selectedImageUri, note, selectedTimestamp);
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
            binding.btnBack.setEnabled(!isLoading);
            binding.layoutImagePicker.setEnabled(!isLoading);
            binding.btnDatePicker.setEnabled(!isLoading);
            binding.etDescription.setEnabled(!isLoading);
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

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putLong("date", selectedTimestamp);
        out.putBoolean("dateSelected",!binding.tvSelectedDate.getText().toString().isEmpty());
        if (selectedImageUri != null) out.putString("image", selectedImageUri.toString());
        super.onSaveInstanceState(out);
    }
}
