package com.taile.knot.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.taile.knot.data.model.Knot;

import java.util.List;

public class MainViewModel extends ViewModel {

    private final MutableLiveData<List<Knot>> knotsLiveData = new MutableLiveData<>();

    public LiveData<List<Knot>> getKnotsLiveData() {
        return knotsLiveData;
    }

    public void setKnots(List<Knot> knots) {
        knotsLiveData.setValue(knots);
    }
}
