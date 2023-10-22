package com.example.blesensor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.*;
import android.hardware.usb.*;
import android.os.*;
import android.text.SpannableStringBuilder;
import android.util.*;
import android.view.*;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.felhr.usbserial.BuildConfig;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    FileOutputStream dataOutS;
    OutputStreamWriter dataWriter;
    //MAC Addresses don't change
    HashSet<String> macs = new HashSet<String>();
    Hashtable<String, Integer> csv = new Hashtable<String, Integer>();
    private static final int WRITE_WAIT_MILLIS = 2000;
    private static final int READ_WAIT_MILLIS = 2000;
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    private BroadcastReceiver broadcastReceiver;
    private TextView receiveText;
    private SerialInputOutputManager usbIoManager;
    private UsbSerialPort usbSerialPort;
    private enum UsbPermission { Unknown, Requested, Granted, Denied }
    private UsbPermission usbPermission = UsbPermission.Unknown;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Switch lever = findViewById(R.id.lever);
        setSwitchCheckedChangeListener(lever);
    }

    public void setSwitchCheckedChangeListener(Switch lever) {
        lever.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TextView timeStamp = findViewById(R.id.timeStamp);
                TextView rssi = findViewById(R.id.rssi);
                TextView macAddress = findViewById(R.id.macAddress);
                if (isChecked) {
                    try {
                        Receiver(getApplicationContext());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    // Handle the case when the Switch is checked (ON)
                    // You can add your code here to perform actions when the switch is turned on
//                    timeStamp.setText("TimeStamp: 1");
                    rssi.setText("RSSI: 2");
                    macAddress.setText("MAC Address: 3");
                } else {
                    // Handle the case when the Switch is unchecked (OFF)
                    // You can add your code here to perform actions when the switch is turned off
                    timeStamp.setText("TimeStamp: None");
                    rssi.setText("RSSI: None");
                    macAddress.setText("MAC Address: None");
                }
            }
        });
    }

    public void Receiver(Context context) throws IOException {
        //takes in string
        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            return;
        }

        UsbSerialPort port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        port.open(connection);
        port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        HandlerThread handlerThread = new HandlerThread("MyHandlerThread");
        handlerThread.start();

        Handler handler = new Handler(handlerThread.getLooper());
        TextView timeStamp = findViewById(R.id.timeStamp);
        handler.post(new Runnable() {
            StringBuilder currentLine = new StringBuilder();
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    byte[] response = new byte[8192];
                    int len = 0;
                    try {
                        len = port.read(response, READ_WAIT_MILLIS);
                    } catch (IOException e) {
                        System.out.println("connection lost: " + e.getMessage());
                    }
                    byte[] data = Arrays.copyOf(response, len);
                    for (byte b : data) {
                        // If newline character is encountered, process and print the current line
                        if (b == '\n') {
                            final String csvLine = currentLine.toString();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (data.length > 0) {
                                        try {
                                            timeStamp.setText("TimeStamp: " + new String(data, "UTF-8"));
                                        } catch (UnsupportedEncodingException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }
                            });
                            currentLine.setLength(0);
                        }
                    }
                }
            }
        });
    }
    private void setUpFile() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss", Locale.getDefault());
        String currDate = sdf.format(new Date());

        String filename = currDate + "-data.csv";

        File path = new File(getExternalFilesDir(null)+ "/");
        if (!path.exists())
            path.mkdir();
        String dataFilePath = path + "/" + filename;

        Log.i("DIR", dataFilePath);

        try {
            dataOutS = new FileOutputStream(dataFilePath);
        } catch (FileNotFoundException e) {
            // TODO do something here
        }
        dataWriter = new OutputStreamWriter(dataOutS);
    }
//    dataWriter.append("whatever")
}
