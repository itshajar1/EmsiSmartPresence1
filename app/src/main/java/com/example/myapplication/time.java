package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class time extends AppCompatActivity {

    private ExpandableListView scheduleListView;
    private ScheduleExpandableListAdapter listAdapter;
    private List<String> daysList = new ArrayList<>();
    private Map<String, List<ScheduleItem>> scheduleMap = new LinkedHashMap<>();

    // Define the order of days for sorting
    private final List<String> dayOrder = new ArrayList<String>() {{
        add("Lundi");
        add("Mardi");
        add("Mercredi");
        add("Jeudi");
        add("Vendredi");
        add("Samedi");

    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.time);

        // Initialize ExpandableListView
        scheduleListView = findViewById(R.id.elv_schedule);

        // Initialize the adapter
        listAdapter = new ScheduleExpandableListAdapter();
        scheduleListView.setAdapter(listAdapter);

        // Auto-expand all groups
        scheduleListView.setOnGroupClickListener((parent, v, groupPosition, id) -> true);

        // Load schedule data from Firebase
        loadSchedule();
    }

    private void loadSchedule() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("schedule")
                .whereEqualTo("teacherId", uid)
                .orderBy("day")
                .orderBy("startTime")
                .get()
                .addOnSuccessListener(query -> {
                    // Clear existing data
                    scheduleMap.clear();
                    daysList.clear();

                    // Process query results
                    for (DocumentSnapshot doc : query) {
                        String day = doc.getString("day");

                        // Get or create the list for this day
                        List<ScheduleItem> daySchedule = scheduleMap.get(day);
                        if (daySchedule == null) {
                            daySchedule = new ArrayList<>();
                            scheduleMap.put(day, daySchedule);
                            daysList.add(day);
                        }

                        // Add schedule item to the day's list with the new class field
                        daySchedule.add(new ScheduleItem(
                                doc.getString("startTime"),
                                doc.getString("endTime"),
                                doc.getString("subject"),
                                doc.getString("group"),
                                doc.getString("room"),
                                doc.getString("class")  // New field
                        ));
                    }

                    // Sort days according to dayOrder
                    daysList.sort((day1, day2) -> {
                        int index1 = dayOrder.indexOf(day1);
                        int index2 = dayOrder.indexOf(day2);
                        if (index1 != -1 && index2 != -1) {
                            return Integer.compare(index1, index2);
                        } else if (index1 != -1) {
                            return -1;
                        } else if (index2 != -1) {
                            return 1;
                        } else {
                            return day1.compareTo(day2);
                        }
                    });

                    // Notify adapter that data has changed
                    listAdapter.notifyDataSetChanged();

                    // Expand all groups
                    for (int i = 0; i < listAdapter.getGroupCount(); i++) {
                        scheduleListView.expandGroup(i);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading schedule: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    // Adapter for the expandable list view
    private class ScheduleExpandableListAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return daysList.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            String day = daysList.get(groupPosition);
            return scheduleMap.get(day).size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return daysList.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            String day = daysList.get(groupPosition);
            return scheduleMap.get(day).get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.group_day_header, parent, false);
            }

            TextView dayHeader = convertView.findViewById(R.id.tv_day_header);
            dayHeader.setText(daysList.get(groupPosition));

            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_schedule, parent, false);
            }

            ScheduleItem item = (ScheduleItem) getChild(groupPosition, childPosition);

            TextView timeText = convertView.findViewById(R.id.tv_time);
            TextView subjectText = convertView.findViewById(R.id.tv_subject);
            TextView classText = convertView.findViewById(R.id.tv_class);  // New TextView for class
            TextView groupText = convertView.findViewById(R.id.tv_group);
            TextView roomText = convertView.findViewById(R.id.tv_room);

            timeText.setText(item.getStartTime() + "-" + item.getEndTime());
            subjectText.setText(item.getSubject());
            classText.setText(item.getClassName());  // Set the class name
            groupText.setText(item.getGroup());
            roomText.setText(item.getRoom());

            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }
    }

    // Inner class to represent schedule items
    private static class ScheduleItem {
        private final String startTime;
        private final String endTime;
        private final String subject;
        private final String group;
        private final String room;

        private final String className;

        public ScheduleItem(String startTime, String endTime,
                            String subject, String group, String room,String className) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.subject = subject;
            this.group = group;
            this.room = room;
            this.className = className;
        }

        // Getters
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
        public String getSubject() { return subject; }
        public String getGroup() { return group; }
        public String getRoom() { return room; }
        public String getClassName() { return className; }
    }
}