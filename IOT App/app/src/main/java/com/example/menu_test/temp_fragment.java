package com.example.menu_test;

import androidx.fragment.app.Fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class temp_fragment extends Fragment {

    private SeekBar seekBar;
    private TextView seekBarTextView;
    private TextView tempTextView;
    private Button tempButton;
    private double currentTempValue = 0.0;
    private boolean isActivated = false;
    private temp_fragment.TempSensorListener listener;
    private Handler handler = new Handler();
    private Runnable sensorRunnable;

    private int minValue = -5;
    private int maxValue = 80;

    // Interface for communication with MainActivity
    public interface TempSensorListener {
        void onTempSensorValueChanged(double value);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.temp_fragment, container, false);

        seekBar = view.findViewById(R.id.seekBar);
        seekBarTextView = view.findViewById(R.id.SeekBarTextView);
        tempTextView = view.findViewById(R.id.tempTextView);
        tempButton = view.findViewById(R.id.temp_button);

        seekBar.setMax(maxValue - minValue);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int actualValue = minValue + progress;
                currentTempValue = actualValue;
                seekBarTextView.setVisibility(TextView.VISIBLE);
                updateTempSensor(actualValue);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        tempButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isActivated) {
                    Toast.makeText(getActivity(), "Temperature Sensor Activated", Toast.LENGTH_SHORT).show();
                    tempButton.setText("Press to deactivate Temperature Sensor");
                    startSendingSensorData();
                }else {
                    Toast.makeText(getActivity(), "Temperature Sensor Deactivated", Toast.LENGTH_SHORT).show();
                    tempButton.setText("Press to activate Temperature Sensor");
                    stopSendingSensorData();
                }
                isActivated = !isActivated;
            }
        });

        return view;
    }

    private void updateTempSensor(int value) {
        seekBarTextView.setText(String.format(Locale.US, "%d °C", value));
        if (value > 50) {
            seekBarTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }else {
            seekBarTextView.setTextColor(getResources().getColor(android.R.color.black));
        }
    }

    private void startSendingSensorData() {
        sensorRunnable = new Runnable() {
            @Override
            public void run() {
                if (listener != null && isActivated) {
                    listener.onTempSensorValueChanged(currentTempValue);
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.removeCallbacks(sensorRunnable);
        handler.post(sensorRunnable);
    }

    private void stopSendingSensorData() {
        handler.removeCallbacks(sensorRunnable);
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof TempSensorListener) {
            listener = (TempSensorListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement TempSensorListener");
        }
    }

    public void onDetach() {
        super.onDetach();
        stopSendingSensorData();
        listener = null;
    }
}
