package com.example.myapplication;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

public class RattrapagesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RattrapageAdapter adapter;
    private List<Rattrapage> rattrapageList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private MaterialCalendarView calendarView;
    private TextView tvDayTitle, tvEmpty;

    private long selectedDateMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rattrapages);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerRattrapages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RattrapageAdapter(rattrapageList);
        recyclerView.setAdapter(adapter);

        calendarView = findViewById(R.id.calendarView);
        calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_SINGLE);
        calendarView.setSelectedDate(CalendarDay.today());
        tvDayTitle = findViewById(R.id.tvDayTitle);
        tvEmpty = findViewById(R.id.tvEmpty); // Make sure this TextView is in XML

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Default to today
        selectedDateMillis = System.currentTimeMillis();
        updateDayTitle(selectedDateMillis);

        // Set calendar listener
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            selectedDateMillis = date.getDate().getTime();
            updateDayTitle(selectedDateMillis);
            loadRattrapagesForDate(selectedDateMillis);
        });

        loadRattrapagesForDate(selectedDateMillis);
    }

    private void updateDayTitle(long dateMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault());
        tvDayTitle.setText("Rattrapages du " + sdf.format(dateMillis));
    }

    private void loadRattrapagesForDate(long dateMillis) {
        String teacherId = auth.getUid();
        if (teacherId == null) {
            Toast.makeText(this, "Non connecté", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SimpleDateFormat sdfTarget = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        String targetDateStr = sdfTarget.format(new Date(dateMillis));

        db.collection("rattrapages")
                .whereEqualTo("profId", teacherId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Rattrapage> allRattrapages = new ArrayList<>();
                    List<Rattrapage> filteredList = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot) {
                        Rattrapage r = doc.toObject(Rattrapage.class);
                        if (r != null) {
                            allRattrapages.add(r);
                            if (r.isOnDate(targetDateStr)) {
                                filteredList.add(r);
                            }
                        }
                    }

                    // Update UI for selected date
                    if (filteredList.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);
                        adapter.updateList(filteredList);
                    }

                    // Update calendar with all rattrapage dates
                    updateCalendarEvents(allRattrapages);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erreur de chargement", Toast.LENGTH_SHORT).show());
    }

    private void updateCalendarEvents(List<Rattrapage> allRattrapages) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        List<CalendarDay> eventDays = new ArrayList<>();

        // Collect all unique dates with rattrapages
        for (Rattrapage r : allRattrapages) {
            try {
                Date date = sdf.parse(r.getDate());
                CalendarDay day = CalendarDay.from(date);
                if (!eventDays.contains(day)) {
                    eventDays.add(day);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Remove old decorators and add new ones
        calendarView.removeDecorators();
        calendarView.addDecorator(new EventDecorator(eventDays));
    }

    public static class Rattrapage {
        private String matiere;
        private String groupe;
        private String classe;
        private String date; // Format: dd-MM-yyyy
        private String heure;


        public Rattrapage() {}

        public String getMatiere() { return matiere; }
        public String getGroupe() { return groupe; }
        public String getClasse() { return classe; }
        public String getDate() { return date; }
        public String getHeure() { return heure; }


        public boolean isOnDate(String targetDateStr) {
            return date != null && date.equals(targetDateStr);
        }
    }

    public class RattrapageAdapter extends RecyclerView.Adapter<RattrapageAdapter.ViewHolder> {
        private List<Rattrapage> list = new ArrayList<>();

        public RattrapageAdapter(List<Rattrapage> rattrapageList) {
            this.list = new ArrayList<>(rattrapageList);
        }


        public void updateList(List<Rattrapage> newList) {
            list.clear();
            list.addAll(newList);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_rattrapage, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Rattrapage r = list.get(position);
            holder.textDateHeure.setText(r.getDate() + " à " + r.getHeure());
            holder.textMatiere.setText(r.getMatiere());
            holder.textClasseGroupe.setText(r.getClasse() + " - " + r.getGroupe());
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView textDateHeure, textMatiere, textClasseGroupe;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textDateHeure = itemView.findViewById(R.id.textDateHeure);
                textMatiere = itemView.findViewById(R.id.textMatiere);
                textClasseGroupe = itemView.findViewById(R.id.textClasseGroupe);
            }
        }
    }

    public class EventDecorator implements DayViewDecorator {
        private final List<CalendarDay> dates;
        private final Drawable backgroundDrawable;
        private final int color = Color.GRAY;

        public EventDecorator(List<CalendarDay> dates) {
            this.dates = dates;
            backgroundDrawable = new ColorDrawable(color);
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return dates.contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            // Add background color
            view.setBackgroundDrawable(backgroundDrawable);

            // Add dot indicator (optional)
            view.addSpan(new DotSpan(5, Color.WHITE));

            // Make text white for better visibility
            view.addSpan(new ForegroundColorSpan(Color.WHITE));
        }
    }
}