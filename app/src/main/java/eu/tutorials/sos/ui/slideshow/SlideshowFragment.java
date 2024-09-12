package eu.tutorials.sos.ui.slideshow;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import com.google.firebase.firestore.QuerySnapshot;


import java.util.ArrayList;
import java.util.Map;

import eu.tutorials.sos.R;
import eu.tutorials.sos.databinding.FragmentSlideshowBinding;

public class SlideshowFragment extends Fragment {

    private FragmentSlideshowBinding binding;
    RecyclerView recyclerView;
    ArrayList<friends> contactsList;
    MyAdapter MyAdapter;
    FirebaseFirestore db;
    ProgressDialog pd;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SlideshowViewModel slideshowViewModel =
                new ViewModelProvider(this).get(SlideshowViewModel.class);

        binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        pd=new ProgressDialog(getContext());
        pd.setCancelable(false);
        pd.setMessage("Fetching Data...");
        recyclerView=root.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        db=FirebaseFirestore.getInstance();
        contactsList=new ArrayList<friends>();
        MyAdapter=new MyAdapter(getContext(),contactsList);
        recyclerView.setAdapter(MyAdapter);
        MyAdapter.notifyDataSetChanged();
        EventChangeListener();
        return root;
    }
    private void EventChangeListener() {
        Log.d("Firestore Data", "Fetching data...");
        String currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("friends")
                .whereEqualTo("UID", currentUserUid)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        Log.d("Firestore Data", "Fetching data...");
                        if (error != null) {
                            if (pd.isShowing()) {
                                pd.dismiss();
                            }
                            Log.i("Firestore Exception", error.getMessage());
                            return;
                        }

                        if (value == null|| value.isEmpty()) {
                            Log.i("Firestore Data", "No data available");
                            return;
                        }
                        contactsList.clear();
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                String id = dc.getDocument().getId();
                                Log.i("Firestore Data", "Document ID: " + id);
                                Map<String, Object> friendMap = dc.getDocument().getData();
                                Log.i("Firestore Data", "Document Data: " + friendMap.toString());
                                String phone = (String) friendMap.get("Phone");
                                String name = (String) friendMap.get("name");
                                boolean exists = false;
                                for (friends contact : contactsList) {
                                    if (contact.getPhone().equals(phone)) {
                                        exists = true;
                                        Log.i("Firestore Data", "Contact with phone " + phone + " already exists.");
                                        Toast.makeText(getContext(),"Contact already registered",Toast.LENGTH_SHORT).show();
                                        break;
                                    }
                                }
                                if (!exists) {
                                    contactsList.add(new friends(phone, name));
                                    Log.d("Firestore Data", "Name: " + name + ", Phone: " + phone);
                                }
                            }
                        }
                        MyAdapter.notifyDataSetChanged();
                        if (pd.isShowing()) {
                            pd.dismiss();
                        }
                    }
                });
    }





    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}