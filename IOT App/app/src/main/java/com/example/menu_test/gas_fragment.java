package com.example.menu_test;

import android.annotation.SuppressLint;
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


public class gas_fragment extends Fragment {

    private SeekBar seekBar;
    private TextView seekBarTextView;
    private TextView gasTextView;
    private Button gasButton;
    private double currentGasValue = 0.0;
    private boolean isActivated = false;
    private gas_fragment.GasSensorListener listener;
    private Handler handler = new Handler();
    private Runnable sensorRunnable;

    // Interface for communication with MainActivity
    public interface GasSensorListener {
        void onGasSensorValueChanged(double value);
    }


    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_gas_fragment, container, false);

        seekBar = view.findViewById(R.id.seekBarGas);
        seekBarTextView = view.findViewById(R.id.SeekBarTextView);
        gasTextView = view.findViewById(R.id.gasTextView);
        gasButton = view.findViewById(R.id.gas_button);

        seekBar.setMax(11);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentGasValue = progress;
                seekBarTextView.setVisibility(TextView.VISIBLE);
                updateGasSensor(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        gasButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isActivated) {
                    Toast.makeText(getActivity(), "Gas Sensor Activated", Toast.LENGTH_SHORT).show();
                    gasButton.setText("Press to deactivate Gas Sensor");
                    startSendingSensorData();
                }else {
                    Toast.makeText(getActivity(), "Gas Sensor Deactivated", Toast.LENGTH_SHORT).show();
                    gasButton.setText("Press to activate Gas Sensor");
                    stopSendingSensorData();
                }
                isActivated = !isActivated;
            }
        });

        return view;
    }

    private void updateGasSensor(double value) {
        seekBarTextView.setText(String.format(Locale.US, "%.3f", value));
        if (value > 9.15) {
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
                    listener.onGasSensorValueChanged(currentGasValue);
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
        if(context instanceof GasSensorListener) {
            listener = (GasSensorListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement GasSensorListener");
        }
    }

    public void onDetach() {
        super.onDetach();
        stopSendingSensorData();
        listener = null;
    }
}