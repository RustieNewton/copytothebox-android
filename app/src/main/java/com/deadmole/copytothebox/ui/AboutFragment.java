// AboutFragment.java
package com.deadmole.copytothebox.ui;

import android.os.Bundle;
//import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
//import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.deadmole.copytothebox.R;

public class AboutFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Inflate the layout normally
        return inflater.inflate(R.layout.fragment_about, container, false);
    }
}

