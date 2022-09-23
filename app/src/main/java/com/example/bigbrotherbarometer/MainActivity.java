package com.example.bigbrotherbarometer;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;
import android.hardware.Sensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {
    private final int TYPE_TOUCH = -27;
    private final String[] phaseNames = {"diglett", "10key"};
    // Views and widgets
    private RelativeLayout relativeLayout;
    private Button phaseSwitch;
    private TextView currentPhase;
    private Button toggleRecording;
    private ImageView field;

    private AppDatabase db;
    private TidbitDao dao;
    SensorManager sm;
    List<Sensor> sensors;
    boolean recording;
    int currentPhaseID;

    Set<Tidbit> data;

    SensorEventListener sel = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        public void onSensorChanged(SensorEvent event) {
            float[] values = event.values;
            if (recording) {
                int type = event.sensor.getType();
                Tidbit tidbit = new Tidbit(type, event.timestamp, values);
                data.add(tidbit);
            }
        }
        private String valueString(float[] values) {
            if (values.length == 1) {
                return "" + values[0];
            } else if (values.length == 2) {
                return "x: " + values[0] + "\n"
                        + "y: " + values[1];
            } else if (values.length == 3) {
                return "x: " + values[0] + "\n"
                        + "y: " + values[1] + "\n"
                        + "z: " + values[2];
            } else return Arrays.toString(values);
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.relativeLayout = findViewById(R.id.relativeLayout);
        this.phaseSwitch = findViewById(R.id.phaseSwitch);
        this.currentPhase = findViewById(R.id.currentPhase);
        this.toggleRecording = findViewById(R.id.toggleRecording);
        this.field = findViewById(R.id.field);

        db = AppDatabase.getInstance(this);
        dao = db.tidbitDao();

        recording = false;
        currentPhaseID = 0;

        // Get sensors and register listeners
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensors = new LinkedList<Sensor>();
        int[] sensorCodes = {Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_PRESSURE, Sensor.TYPE_GYROSCOPE};
        for (int sensorCode : sensorCodes) {
            Sensor currSensor = sm.getDefaultSensor(sensorCode);
            sensors.add(currSensor);
            sm.registerListener(sel, currSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        // Create phase button, start with first phase
        phaseSwitch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switchPhase();
            }
        });
        switchPhaseTo(0);

        toggleRecording.setOnClickListener(new View.OnClickListener() {
           public void onClick(View v) {
               toggle();
           }
        });

        relativeLayout.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (recording) {
                    // event.getDownTime() uses a different clock than the sensor clock. That's okay,
                    // we can just fix this in R using two clocking tidbits.
                    Tidbit tidbit = new Tidbit(TYPE_TOUCH,  event.getDownTime(),
                            event.getX(), event.getY());
                    System.out.println(tidbit);
                    data.add(tidbit);
                }
                return true;
            }
        });
    }


    private void switchPhase() {
        currentPhaseID = (currentPhaseID + 1) % phaseNames.length;
        switchPhaseTo(currentPhaseID);
    }

    private void switchPhaseTo(int phaseID) {
        String phase = phaseNames[phaseID];
        // TODO cd .. implement phases here!!
        switch(phase) {
            case "diglett":
                // this.relativeLayout.setBackgroundColor(Color.parseColor("#009A17"));
                this.relativeLayout.setBackgroundResource(R.drawable.grass);
                this.currentPhase.setText(R.string.collecting);
                this.currentPhase.setTextColor(Color.parseColor("#333333"));
                // this.relativeLayout.setAlpha((float) 0.1);
                this.field.setAlpha((float) 0);
                break;
            case "10key":
                this.relativeLayout.setBackgroundColor(Color.parseColor("#080808"));
                this.currentPhase.setText(R.string.cracking);
                this.currentPhase.setTextColor(Color.parseColor("#BBBBBB"));
                this.field.setAlpha((float) 1);
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void toggle() {
        recording = !recording;
        if (recording) {
            System.out.println("Recording...");
            data = new TreeSet<>();
            timestamp();
            this.toggleRecording.setText("STOP");
        } else {
            Toast.makeText(MainActivity.this, "Logging..", Toast.LENGTH_SHORT).show();
            timestamp();
            // write the data to database
            for (Tidbit tidbit : data) {
                dao.insert(tidbit);
            }
            // empty the data field
            data = null;
            // Toast.makeText(MainActivity.this, "Logged!", Toast.LENGTH_SHORT).show();
            this.toggleRecording.setText("START");
            this.log();
        }
    }

    /* Create csv file from internal database */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void log() {
        List<Tidbit> tidbits = dao.tidbits();

        String filename = "db " + timeString() + ".csv";

        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), filename);
        String filepath = file.getPath();
        try {
            OutputStream os = new FileOutputStream(file);
            String firstline = "type,time,one,two,three\n";
            os.write(firstline.getBytes());
            for (Tidbit tidbit : tidbits) {
                String line = tidbit.toString() + "\n";
                os.write(line.getBytes()); // charset unspecified; may not use the correct encoding.
            }
            os.close();
            Toast.makeText(MainActivity.this, tidbits.size() + " tidbits stored to " + filepath,
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.w("ExternalStorage", "Error writing " + file, e);
        }
    }

    /* Returns M-D-YYYY H-M-S
    *  No leading zeros*/
    private String timeString() {
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int year = calendar.get(Calendar.YEAR);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);

        String date = month + "-" + day + "-" + year;
        String time = hour + "-" + minute + "-" + second;

        return date + " " + time;
    }



    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void timestamp() {
        Tidbit millis = new Tidbit(-1, SystemClock.uptimeMillis(), 0);
        Tidbit nanos = new Tidbit(-2, SystemClock.elapsedRealtimeNanos(), 0);
        data.add(millis);
        data.add(nanos);
    }


    @Override
    protected void onStop() {
        if (sensors.size() > 0) {
            sm.unregisterListener(sel);
        }
        dao.emptyTables();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}