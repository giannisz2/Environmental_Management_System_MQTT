package com.example.menu_test;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;


public class smoke_fragment extends Fragment {

    private SeekBar seekBar;
    private TextView seekBarTextView;
    private TextView smokeTextView;
    private Button smokeButton;
    private double currentSmokeValue = 0.0;
    private boolean isActivated = false;
    private SmokeSensorListener listener;
    private Handler handler = new Handler();
    private Runnable sensorRunnable;

    // Interface for communication with MainActivity
    public interface SmokeSensorListener {
        void onSmokeSensorValueChanged(double value);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_smoke_fragment, container, false);

        seekBar = view.findViewById(R.id.seekBar);
        seekBarTextView = view.findViewById(R.id.SeekBarTextView);
        smokeTextView = view.findViewById(R.id.smokeTextView);
        smokeButton = view.findViewById(R.id.smoke_button);

        seekBar.setMax(25);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentSmokeValue = progress / 100.0;
                seekBarTextView.setVisibility(TextView.VISIBLE);
                updateSmokeSensor(progress / 100.0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        smokeButton.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                if(!isActivated) {
                    Toast.makeText(getActivity(), "Smoke Sensor Activated", Toast.LENGTH_SHORT).show();
                    smokeButton.setText("Press to deactivate Smoke Sensor");
                    startSendingSensorData();

                }else {
                    Toast.makeText(getActivity(), "Smoke Sensor Deactivated", Toast.LENGTH_SHORT).show();
                    smokeButton.setText("Press to activate Smoke Sensor");
                    stopSendingSensorData();
                }
                isActivated = !isActivated;
            }
        });

        return view;
    }

    private void updateSmokeSensor(double value) {
        seekBarTextView.setText(String.format(Locale.US, "%.3f", value));
        if (value > 0.14) {
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
                    listener.onSmokeSensorValueChanged(currentSmokeValue);
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
        if (context instanceof SmokeSensorListener) {
            listener = (SmokeSensorListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement SmokeSensorListener");
        }
    }

    public void onDetach() {
        super.onDetach();
        stopSendingSensorData();
        listener = null;
    }
}