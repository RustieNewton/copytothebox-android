// AboutFragment.java
package com.deadmole.copytothebox.ui;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.deadmole.copytothebox.BuildConfig;
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        TextView mobileInfoSection = view.findViewById(R.id.about_phone);

        //FIXME add some conditional logic to show phone related info if phone version
        if (BuildConfig.FLAVOR.equals("phone")) {
            mobileInfoSection.setVisibility(View.VISIBLE);
        } else {
            mobileInfoSection.setVisibility(View.GONE);
        }
    }
}

