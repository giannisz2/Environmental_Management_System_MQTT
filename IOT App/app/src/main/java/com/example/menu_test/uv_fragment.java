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


public class uv_fragment extends Fragment {

    private SeekBar seekBar;
    private TextView seekBarTextView;
    private TextView uvTextView;
    private Button uvButton;
    private double currentUvValue = 0.0;
    private boolean isActivated = false;
    private uv_fragment.UvSensorListener listener;
    private Handler handler = new Handler();
    private Runnable sensorRunnable;

    // Interface for communication with MainActivity
    public interface UvSensorListener {
        void onUvSensorValueChanged(double value);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_uv_fragment, container, false);

        seekBar = view.findViewById(R.id.seekBar);
        seekBarTextView = view.findViewById(R.id.SeekBarTextView);
        uvTextView = view.findViewById(R.id.uvTextView);
        uvButton = view.findViewById(R.id.uv_button);

        seekBar.setMax(11);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentUvValue = progress;
                seekBarTextView.setVisibility(TextView.VISIBLE);
                updateUVSensor(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        uvButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isActivated) {
                    Toast.makeText(getActivity(), "UV Sensor Activated", Toast.LENGTH_SHORT).show();
                    uvButton.setText("Press to deactivate UV Sensor");
                    startSendingSensorData();
                }else {
                    Toast.makeText(getActivity(), "UV Sensor Deactivated", Toast.LENGTH_SHORT).show();
                    uvButton.setText("Press to activate UV Sensor");
                    stopSendingSensorData();
                }
                isActivated = !isActivated;
            }
        });

        return view;
    }

    private void updateUVSensor(double value) {
        seekBarTextView.setText(String.format(Locale.US, "%.3f", value));
        if (value > 6) {
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
                    listener.onUvSensorValueChanged(currentUvValue);
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
        if(context instanceof UvSensorListener) {
            listener = (UvSensorListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement UvSensorListener");
        }
    }

    public void onDetach() {
        super.onDetach();
        stopSendingSensorData();
        listener = null;
    }
}