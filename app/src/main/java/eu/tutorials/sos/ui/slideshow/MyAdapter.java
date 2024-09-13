package eu.tutorials.sos.ui.slideshow;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import eu.tutorials.sos.R;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    Context context;
    ArrayList<friends> contactsList;

    // Constructor
    public MyAdapter(Context context, ArrayList<friends> contactsList) {
        this.context = context;
        this.contactsList = contactsList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the contact item layout
        View v = LayoutInflater.from(context).inflate(R.layout.contacts, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        friends friend = contactsList.get(position);
        holder.Name.setText(friend.getName());
        holder.Phone.setText(friend.getPhone());
    }

    @Override
    public int getItemCount() {
        return contactsList.size();
    }
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView Name, Phone;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            Name = itemView.findViewById(R.id.tvName);
            Phone = itemView.findViewById(R.id.tvPhone);
        }
    }
}
