package com.taile.knot.ui.viewmodel;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.taile.knot.data.model.Knot;

public class AddKnotViewModel extends ViewModel {

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsSuccess() { return isSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void createKnot(Uri imageUri, String description, long memoryDate) {
        if (imageUri == null) {
            errorMessage.setValue("Bạn chưa chọn bức ảnh nào nè!");
            return;
        }

        if (description == null || description.trim().isEmpty()) {
            errorMessage.setValue("Đừng quên viết vài dòng ký sự nhen!");
            return;
        }

        isLoading.setValue(true);

        // Mô phỏng quá trình xử lý nén và lưu
        new Thread(() -> {
            try {
                Thread.sleep(800); // Giả lập độ trễ lưu dữ liệu

                // Đóng gói Model Knot mới
                Knot newKnot = new Knot(
                        String.valueOf(System.currentTimeMillis()),
                        description.trim(),
                        imageUri.toString(),
                        memoryDate
                );

                // Sau này sẽ gọi KnotRepository.uploadAndSave(newKnot) tại đây
                isLoading.postValue(false);
                isSuccess.postValue(true);
            } catch (InterruptedException e) {
                isLoading.postValue(false);
                errorMessage.postValue("Có lỗi xảy ra: " + e.getMessage());
            }
        }).start();
    }
}