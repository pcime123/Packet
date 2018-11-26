package com.sscctv.packetgenerator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private DataOutputStream opt;
    private Timer receiveTimer;
    private Timer sendTimer;
    private static final String TAG = "Packet Generator";
    private String myIP;
    private InputMethodManager imm;
    private RecyclerView recyclerView;
    private RecyclerAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<ReceivePacket> mItems = new ArrayList<>();

    private final static int Init = 0;
    private final static int Run = 1;
    private final static int Pause = 2;

    private int se_Status; //현재의 상태를 저장할변수를 초기화함.
    private int re_Status; //현재의 상태를 저장할변수를 초기화함.
    private int myCount = 1;
    private long myBaseTime;
    private long myPauseTime;
    private long outTime;
    private long now;
    private long mLastClickTime;
    private boolean auto;

    private TimerTask receiveTask;
    private TimerTask sendTask;
    private Toast mActiveToast;

    private int receiveLeng;
    private String receiveData;
    private String serverIp;

    private int rePort;
    private DatagramSocket reSocket;
    private DatagramPacket rePacket;


    @BindView(R.id.input_ip)
    EditText input_ip;
    @BindView(R.id.input_port)
    EditText input_port;
    @BindView(R.id.input_data)
    EditText input_data;
    @BindView(R.id.input_interval)
    EditText input_interval;

    @BindView(R.id.input_host)
    EditText input_host;
    @BindView(R.id.input_host_port)
    EditText host_port;

    @BindView(R.id.button_start)
    Button button_start;
    @BindView(R.id.button_stop)
    Button button_stop;

    @BindView(R.id.button_bind)
    Button button_bind;

    @BindView(R.id.checkBox)
    CheckBox auto_send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        PortSet();
        ipInput();


        recyclerView = findViewById(R.id.receive_packet);
        recyclerView.setHasFixedSize(true);
        mAdapter = new RecyclerAdapter(mItems);
        recyclerView.setAdapter(mAdapter);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);


        myIP = getEthernetIPAddress();
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);


        auto_send.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    input_interval.setEnabled(true);
                } else {
                    input_interval.setEnabled(false);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        se_Status = Run;
        re_Status = Run;

        myBaseTime = SystemClock.elapsedRealtime();

        input_ip.setText(myIP);
        input_port.setText(R.string.def_port);

        input_host.setEnabled(false);
        input_host.setText(myIP);
        host_port.setText(R.string.def_port);

        input_interval.setText("1");
        if(auto_send.isChecked()) {
            input_interval.setEnabled(true);
        } else {
            input_interval.setEnabled(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        se_Status = Pause;
        re_Status = Pause;
        if (receiveTimer != null) {
            receiveTimer.cancel();
            receiveTimer = null;
        }

        if (sendTimer != null) {
            sendTimer.cancel();
            sendTimer = null;
        }
    }


    public TimerTask receiveTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                UDPReceive();

            }
        };
    }

    public TimerTask sendTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                if(auto) {
                    UDPSend();
                } else {
                    UDPSend();
                    if (sendTimer != null) {
                        sendTimer.cancel();
                        sendTask.cancel();
                        sendTimer = null;
                    }
                }
            }
        };
    }


    @OnClick(R.id.button_bind)
    void onBindButtonClicked() {
        hideKeyboard();
        if(SystemClock.elapsedRealtime() - mLastClickTime < 500) {
            return;
        }
        mLastClickTime = SystemClock.elapsedRealtime();
        switch (re_Status) {
            case Pause:
                if (receiveTimer != null) {
                    receiveTimer.cancel();
                    receiveTask.cancel();
                    receiveTimer = null;

                }

                re_Status = Run;
                myPauseTime = SystemClock.elapsedRealtime();
                button_bind.setText(R.string.bind);
                host_port.setEnabled(true);


                break;

            case Run:

                if (getHostIp() != null && getHostPort() != 0) {
                    re_Status = Pause;
                    myBaseTime = SystemClock.elapsedRealtime();
                    host_port.setEnabled(false);

                    button_bind.setText(R.string.stop);
                    receiveTask = receiveTimerTask();
                    if (receiveTimer == null) {
                        receiveTimer = new Timer();
                        receiveTimer.schedule(receiveTask, 100, 500);
                    }

                    Log.d(TAG, " Button Run");
                } else if (getHostIp() == null) {
                    showToast(R.string.not_host_ip);
                } else if (getHostPort() == 0) {
                    showToast(R.string.not_host_port);
                }
                break;
        }
    }

    @OnClick(R.id.button_start)
    void onStarButtonClicked() {
        if(SystemClock.elapsedRealtime() - mLastClickTime < 500) {
            return;
        }
        mLastClickTime = SystemClock.elapsedRealtime();
        hideKeyboard();
        switch (se_Status) {
            case Pause:
                se_Status = Run;
                myPauseTime = SystemClock.elapsedRealtime();
                button_start.setText(R.string.send);

                if (sendTimer != null) {
                    sendTimer.cancel();
                    sendTask.cancel();
                    sendTimer = null;

                }
                input_ip.setEnabled(true);
                input_port.setEnabled(true);
                input_interval.setEnabled(true);
                input_data.setEnabled(true);
                Log.d(TAG, " Button Pause");

                break;

            case Run:

                if (getIp() != null && getData() != null && getPort() != 0) {
                    myBaseTime = SystemClock.elapsedRealtime();

                    auto = auto_send.isChecked();

                    sendTask = sendTimerTask();
                    if (sendTimer == null) {
                        sendTimer = new Timer();
                        sendTimer.schedule(sendTask, 100, getInterval());
                    }

                    if(auto) {

                        button_start.setText(R.string.stop);
                        se_Status = Pause;

                        input_ip.setEnabled(false);
                        input_port.setEnabled(false);
                        input_interval.setEnabled(false);
                        input_data.setEnabled(false);
                    } else {


                        button_start.setText(R.string.send);
                        se_Status = Run;

                        input_ip.setEnabled(true);
                        input_port.setEnabled(true);
                        input_interval.setEnabled(true);
                        input_data.setEnabled(true);

                    }

                    Log.d(TAG, " Button Run");
                } else if (getIp() == null) {
                    showToast(R.string.not_input_ip);
                } else if (getPort() == 0) {
                    showToast(R.string.not_input_port);
                } else if (getData() == null) {
                    showToast(R.string.not_input_data);
                }
                break;
        }


    }


    @OnClick(R.id.button_stop)
    void onStopButtonClicked() {
        mItems.clear();
        mAdapter.notifyDataSetChanged();

        getPort();
    }


    private void hideKeyboard() {
        imm.hideSoftInputFromWindow(input_data.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(input_interval.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(input_port.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(input_ip.getWindowToken(), 0);
    }

    public String getTimeOut() {
        now = SystemClock.elapsedRealtime(); //애플리케이션이 실행되고나서 실제로 경과된 시간(??)^^;
        outTime = now - myBaseTime;
        String easy_outTime = String.format("%02d:%02d:%02d", outTime / 1000 / 60, (outTime / 1000) % 60, (outTime % 1000) / 10);
        return easy_outTime;

    }

    private static String getEthernetIPAddress() {
        String sValue = "";
        try {
            Process p = Runtime.getRuntime().exec("ifconfig eth0");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String sLine;
            while ((sLine = input.readLine()) != null) {
                if (sLine.contains("eth0:")) {
                    Pattern pIPAddress = Pattern.compile("ip (.+?) ");
                    Matcher matcher = pIPAddress.matcher(sLine);
                    if (matcher.find()) {
                        sValue = matcher.group(1);
                        break;
                    }
                }
            }
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sValue;
    }

    private String getIp() {
        String value;
        ;
        if (input_ip.getText().length() == 0) {
//            this.runOnUiThread(new Runnable() {
////                @Override
////                public void run() {
////                    Toast.makeText(MainActivity.this, "IP 주소를 입력해주세요", Toast.LENGTH_SHORT).show();
////                }
////            });
            value = null;
        } else {
            value = input_ip.getText().toString();

        }

        return value;
    }

    private String getHostIp() {
        String value;

        if (input_host.getText().length() == 0) {
//            this.runOnUiThread(new Runnable() {
////                @Override
////                public void run() {
////                    Toast.makeText(MainActivity.this, "IP 주소를 입력해주세요", Toast.LENGTH_SHORT).show();
////                }
////            });
            value = null;
        } else {
            value = input_host.getText().toString();
        }

        return value;
    }

    private int getPort() {
        int value;

        if (input_port.getText().length() == 0) {
//            Toast.makeText(this, "Port를 입력해주세요", Toast.LENGTH_SHORT).show();

            value = 0;
        } else {
            value = Integer.valueOf(input_port.getText().toString());
        }

        return value;
    }

    private int getHostPort() {
        int value;

        if (host_port.getText().length() == 0) {
//            Toast.makeText(this, "Port를 입력해주세요", Toast.LENGTH_SHORT).show();

            value = 0;
        } else {
            value = Integer.valueOf(host_port.getText().toString());
        }

        return value;
    }

    private String getData() {
        String value;

        if (input_data.getText().length() == 0) {
//            this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(MainActivity.this, "Data를 입력해주세요", Toast.LENGTH_SHORT).show();
//                }
//            });

            value = null;
        } else {
            value = input_data.getText().toString();
        }

        return value;
    }

    private int getInterval() {
        int value = 0;

        if (input_port.getText().length() == 0) {
            Toast.makeText(this, "Interval을 입력해주세요", Toast.LENGTH_SHORT).show();

            value = 1000;
        } else {
            value = Integer.valueOf(input_interval.getText().toString()) * 1000;
        }

        return value;
    }

    private void ipInput() {
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart) +
                            source.subSequence(start, end) + destTxt.substring(dend);
                    if (!resultingTxt.matches("^\\d{1,3}(\\." + "(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i = 0; i < splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }
        };
        input_ip.setFilters(filters);

    }


    void UDPSend() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (getIp() != null) {
                        InetAddress targetAddr = InetAddress.getByName(getIp());
                        DatagramSocket socket = new DatagramSocket();
                        int port = getPort();
                        byte[] buf = (getData()).getBytes();
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, targetAddr, port);
                        Log.d(TAG, "S --->  IP: " + getIp() + " Port: " + getPort() + " Data: " + getData());
                        socket.send(packet);
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    void UDPReceive() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    rePort = getHostPort();
                    reSocket = new DatagramSocket(rePort);

                    while (re_Status != Run) {

                        byte[] buf = new byte[1024];
                        rePacket = new DatagramPacket(buf, buf.length);
                        reSocket.receive(rePacket);

                        receiveLeng = rePacket.getLength();
                        receiveData = new String(rePacket.getData(), 0, rePacket.getLength());
                        reSocket.close();

                        serverIp = rePacket.getAddress().getHostAddress();
                        myTimer.sendEmptyMessage(0);

                        Log.d(TAG, "R <----  IP: " + serverIp + " Port: " + getPort() + " Data: " + receiveData);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "R: Error");
                }
            }
        }).start();
    }


    @SuppressLint("HandlerLeak")
    Handler myTimer = new Handler() {
        public void handleMessage(Message msg) {

            String leng = String.valueOf(receiveLeng);
            int count;
            count = mAdapter.getItemCount();
            Log.d(TAG, "Write Packer " + leng + "   " + msg);

            mItems.add(new ReceivePacket(String.valueOf(count), getTimeOut(), serverIp, getIp(), "UDP", leng, receiveData));

            recyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
            mAdapter.notifyDataSetChanged();

        }
    };


    private void PortSet() {
        try {
            Runtime command = Runtime.getRuntime();
            Process proc;

            proc = command.exec("su");
            opt = new DataOutputStream(proc.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            throw new SecurityException();
        }
    }

    private void showToast(int message) {
        if (mActiveToast != null) {
            mActiveToast.cancel();
        }

        mActiveToast = Toast.makeText(MainActivity.this, getString(message), Toast.LENGTH_SHORT);

        mActiveToast.show();
    }


    public static boolean isIpAddress(String ipAddress) {

        boolean returnValue = false;

        String regex = "^([0-9]{1,3}).([0-9]{1,3}).([0-9]{1,3}).([0-9]{1,3})$";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(ipAddress);

        if (m.matches()) {
            returnValue = true;
        }
        return returnValue;
    }


    public void packtGen() {

        try {
            opt.writeBytes("echo 1 > /sys/class/misc/mv88e6176/pkgen\n");
            Log.d(TAG, "---->");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

