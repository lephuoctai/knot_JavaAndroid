package com.taile.knot.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.taile.knot.R;
import com.taile.knot.data.model.Knot;

import java.util.ArrayList;
import java.util.List;

public class MainViewModel extends ViewModel {

    private final MutableLiveData<List<Knot>> knotsLiveData = new MutableLiveData<>();

    public LiveData<List<Knot>> getKnotsLiveData() {
        return knotsLiveData;
    }

    public void loadMockKnots() {
        List<Knot> mockList = new ArrayList<>();
        long now = System.currentTimeMillis();
        long oneDay = 86400000L;

        mockList.add(new Knot("1", "Kỷ niệm ngày đầu tiên gặp gỡ nhau tại quán cà phê góc phố.", R.drawable.sample2, now - (oneDay * 20)));
        mockList.add(new Knot("2", "Chuyến đi biển cùng nhau, ngắm hoàng hôn buông xuống rất đẹp.", R.drawable.sample3, now - (oneDay * 10)));
        mockList.add(new Knot("3", "Bức ảnh chụp vội nhưng ghi lại trọn vẹn nụ cười rạng rỡ ngày hôm đó.", R.drawable.sample1, now));

        knotsLiveData.setValue(mockList);
    }
}