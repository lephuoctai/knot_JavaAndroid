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

    public void createKnot(com.taile.knot.data.PrototypeStore store,
                           Uri imageUri, String description, long memoryDate) {
        if (Boolean.TRUE.equals(isLoading.getValue())) return;
        if (imageUri == null) {
            errorMessage.setValue("Bạn chưa chọn bức ảnh nào nè!");
            return;
        }

        if (description == null || description.trim().isEmpty()) {
            errorMessage.setValue("Đừng quên viết vài dòng ký sự nhen!");
            return;
        }

        isLoading.setValue(true);

        new Thread(() -> {
            try {
                Knot newKnot = new Knot(
                        java.util.UUID.randomUUID().toString(),
                        description.trim(),
                        imageUri.toString(),
                        memoryDate
                );

                if (!store.addKnot(newKnot)) {
                    isLoading.postValue(false);
                    errorMessage.postValue("Chưa lưu được. Bạn thử lại nha.");
                    return;
                }
                isLoading.postValue(false);
                isSuccess.postValue(true);
            } catch (org.json.JSONException | RuntimeException e) {
                isLoading.postValue(false);
                errorMessage.postValue(e instanceof IllegalStateException ? e.getMessage() : "Chưa lưu được. Bạn thử lại nha.");
            }
        }).start();
    }
}
