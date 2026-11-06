package com.p280.chargeguard;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;

public class MainActivity extends Activity {

    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 40, 30, 30);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("P280 ChargeGuard");
        title.setTextSize(25);

        status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(0, 35, 0, 35);
        status.setText("Ready to test root access.");

        Button testButton = new Button(this);
        testButton.setText("Test Root Access");

        layout.addView(title);
        layout.addView(status);
        layout.addView(testButton);

        setContentView(layout);

        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testRoot();
            }
        });
    }

    private void testRoot() {
        status.setText("Requesting root access...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                String result;

                try {
                    Process process =
                        new ProcessBuilder("/system/xbin/su")
                            .redirectErrorStream(true)
                            .start();

                    OutputStream input = process.getOutputStream();

                    input.write(
                        ("id\n" +
                         "cat /sys/class/power_supply/battery/capacity\n" +
                         "cat /sys/class/power_supply/usb/online\n" +
                         "cat /sys/class/power_supply/ac/online\n" +
                         "exit\n").getBytes("UTF-8")
                    );

                    input.flush();
                    input.close();

                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream())
                    );

                    StringBuilder output = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }

                    int exitCode = process.waitFor();

                    result = "Exit code: " + exitCode +
                             "\n\nRoot and battery test:\n" +
                             output.toString();

                    if (output.toString().contains("uid=0(root)")) {
                        result += "\nROOT ACCESS: SUCCESS";
                    } else {
                        result += "\nROOT ACCESS: NOT CONFIRMED";
                    }

                } catch (Exception e) {
                    result = "Error: " + e.getMessage();
                }

                final String finalResult = result;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        status.setText(finalResult);
                    }
                });
            }
        }).start();
    }
}