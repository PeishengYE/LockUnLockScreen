package com.radioyps.lockunlockscreen;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by developer on 20/10/16.
 */


    public class SocketConnectServer extends Service{
        private static final String TAG = SocketConnectServer.class.getSimpleName();


        ServerSocket serverSocket=null;
        Socket mSocket = null;

        int count = 0;
        static final int socketServerPORT = 33333;
        public String uid = "";
        private static final int len_sensorID = 8;
        private static final int len_data = 10;
        private  static  boolean isServerStarted = false;

        public SocketConnectServer() {
            super();
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }




        private class initSocketConnection extends Thread {


            public void run() {
                try {
                    isServerStarted = true;
                    serverSocket = new ServerSocket(socketServerPORT);
                    Log.d("GroupOwnerSocketHandler", "Socket Started");
                    while (true) {

                        mSocket = serverSocket.accept();
                        Thread socketServerThread = new Thread(new SocketServerThread());
                        socketServerThread.start();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                isServerStarted = false;
            }
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            ;

            if(!isServerStarted){
                Log.i(TAG, "start service .. ");
                Thread initThread = new Thread(new initSocketConnection());
                initThread.start();
            }else{
                Log.i(TAG, "Service already started.. ");
            }

            return super.onStartCommand(intent, flags, startId);
        }




        public void onDestroy() {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }


        private void closeSocket(Socket socketInput){
            if (socketInput != null) {
                try {
                    socketInput.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        public static void sendMessage(int messageFlag, String message ){
            Message.obtain(LockScreenActivity.mHandler,
                    messageFlag,
                    message).sendToTarget();

        }

        public class SocketServerThread extends Thread {
            InputStream inputStream=null;
            Socket localSocket = mSocket;

            @Override
            public void run() {
                String message = "";
                try {

                        inputStream = localSocket.getInputStream();
                        count++;
                        Log.i(TAG, "SocketServerThread()>>  we have " + count +"connections");

                        message += "#" + count + " from "
                                + localSocket.getInetAddress() + ":"
                                + localSocket.getPort() + "\n";

                        Log.i(TAG, "SocketServerThread()>>  message: " + message);
                        sendMessage(CommonConstants.MSG_UPDATE_CURRENT_CONNECTED_IP, message);


                        byte[] buffer = new byte[1024];
                        int length;
                        StringBuffer sBuffer = new StringBuffer("");
                        try {
                            while ((length = inputStream.read(buffer)) != -1) {
                                byte[] myBytes = Arrays.copyOfRange(buffer, 0, length);
                                sBuffer.append(myBytes);

                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        Log.i(TAG, "SocketServerThread()>> msg: " +sBuffer.toString());
                        sendMessage(CommonConstants.MSG_UPDATE_INFO_RECEVIED, sBuffer.toString());



                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } finally {
                    closeSocket(localSocket);
                }
            }

        }



    /*Fixme  the following code is not used, reserved for future */
        public class SocketServerReplyThread extends Thread {

            private Socket hostThreadSocket;
            int cnt;

            SocketServerReplyThread(Socket socket, int c) {
                hostThreadSocket = socket;
                cnt = c;
            }

            @Override
            public void run() {
                OutputStream outputStream;
                String msgReply = "Hello from SocketConnectServer, you are #" + cnt;
                String message = "";
                try {

                    outputStream = hostThreadSocket.getOutputStream();



                    PrintStream printStream = new PrintStream(outputStream);
                    printStream.print(msgReply);
                    printStream.close();

                    message += "replayed: " + msgReply + "\n";
                    sendMessage(CommonConstants.MSG_UPDATE_INFO_RECEVIED, message);


                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    message += "Something wrong! " + e.toString() + "\n";
                }
                sendMessage(CommonConstants.MSG_UPDATE_INFO_RECEVIED, message);
            }

        }

        public void processData(byte[] mbytes) {

            String temp = new String(mbytes);
            boolean newAck = false;
            Random random = new Random();
            int rand = random.nextInt(100) + 1;

            // compare if new patient

            if (temp.contains("New")) {

                System.out.println("Add Patient");
                newAck = true;
            }

            if ((mbytes.length) == len_sensorID) {


                if (uid.equalsIgnoreCase(temp)) {

                    uid = temp;
                    System.out.println("same ID: " + uid);
                    //Do something
                }

                if (uid.equalsIgnoreCase("")) {
                    // Add new Patient in DB
                    uid = temp;
                    System.out.println("Store ID " + uid);
                    System.out.println("Add Patient with " + uid + " unique ID");
                }

            }
            if ((mbytes.length) == len_data) {
                System.out.println("480 bytes received");
                // compare threshold

                for (int m = 0; m < mbytes.length; m += 2) {

                    int threshold = 1024;
                    byte[] b = mbytes;
                    byte b1 = b[m];
                    byte b2 = b[m + 1];
                    int output = (b2 << 8) | b1;

                    if (output < threshold) {
                        System.out.println(" Hex below threshold " + output);
                    }
                    if (output == threshold) {
                        System.out.println(" Hex at threshold " + output);
                    }
                    if (output > threshold) {
                        System.out.println(" Hex above threshold " + output);
                    }

                }
            }
        }


        public String getIpAddress() {
            String ip = "";
            try {
                Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                        .getNetworkInterfaces();
                while (enumNetworkInterfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = enumNetworkInterfaces
                            .nextElement();
                    Enumeration<InetAddress> enumInetAddress = networkInterface
                            .getInetAddresses();
                    while (enumInetAddress.hasMoreElements()) {
                        InetAddress inetAddress = enumInetAddress
                                .nextElement();

                        if (inetAddress.isSiteLocalAddress()) {
                            ip += "SocketConnectServer running at : "
                                    + inetAddress.getHostAddress();
                        }
                    }
                }

            } catch (SocketException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                ip += "Something Wrong! " + e.toString() + "\n";
            }
            return ip;
        }
    }


