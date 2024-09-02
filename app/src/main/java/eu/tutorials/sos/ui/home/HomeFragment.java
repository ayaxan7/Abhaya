package eu.tutorials.sos.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import eu.tutorials.sos.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Set up the SOS button
        Button sosButton = binding.sosButton;
        sosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle the SOS button click
                sendSos();
            }
        });

        return root;
    }

    private void sendSos() {
        // Implement the SOS sending logic here
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
