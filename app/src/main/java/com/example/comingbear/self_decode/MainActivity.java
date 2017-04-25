package com.example.comingbear.self_decode;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MainActivity extends Activity {

    private String TAG = "mmmmmmmmmmmmmmmmmmmmmm";

    private SurfaceView mPlaySurface = null;
    private TextureView mPlayTexture = null;
    private SurfaceHolder mPlaySurfaceHolder;
    private Thread mDecodeThread;
    private MediaCodec mPlayCodec;
    private boolean mStopFlag = false;
    private DataInputStream mPlayInputStream;
    private String FileName = "test.h264";
    private int Video_Width = 500;
    private int Video_Height = 300;
    private int PlayFrameRate = 15;
    private Boolean isUsePpsAndSps = false;


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //标记一下
            Toast.makeText(MainActivity.this, "播放结束!", Toast.LENGTH_LONG).show();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);


//        fileToQueue();

        String filePath =  Environment.getExternalStorageDirectory() + "/carTempRecv.264";
        init(filePath);


//        PhoneServer phoneServer = new PhoneServer(this);
//        phoneServer.start();


        WifiManager wifiManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        String ipString = (ipAddress & 0xFF ) + "." +
                ((ipAddress >> 8 ) & 0xFF) + "." +
                ((ipAddress >> 16 ) & 0xFF) + "." +
                ( ipAddress >> 24 & 0xFF);
//        ipinfo.setText(ipString);
        Toast.makeText(this, ipString, Toast.LENGTH_LONG).show();

//
//        fileToQueue();
//
//        initFos();
    }

//    private FileOutputStream fos;
//    public void initFos (){
//        try{
//            fos = new FileOutputStream(new File(Environment.getExternalStorageDirectory() + "/carTempRecvEveryFrameToPut.264"));
//        }catch (IOException e){}
//    }

    private boolean isPlay = false;

    public void startPlay(View v){
//        String filePath =  Environment.getExternalStorageDirectory() + "/carTempRecv.264";
//        init(filePath);
        isPlay = true;
        startDecodingThread();
        new decodeH2Thread().start();
//        isPlay = true;
    }
    public void startRecv(View v){
        isRecv = true;
        //
        Log.d("ssssssssssssssss","thread start");
        new recvSocketThread().start();

    }
    private void init(String filePath){
        File f = new File(filePath);
        if (null == f || !f.exists() || f.length() == 0) {
            Toast.makeText(this, "指定文件不存在", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            //获取文件输入流
            mPlayInputStream = new DataInputStream(new FileInputStream(new File(filePath)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

//        mPlaySurface = (SurfaceView) findViewById(R.id.preview);
        mPlayTexture = (TextureView) findViewById(R.id.preview);

        mPlaySurfaceHolder = mPlaySurface.getHolder();
//        mPlaySurfaceHolder = mPlayTexture.getHolder();

        //回调函数来啦
        mPlaySurfaceHolder.addCallback(new SurfaceHolder.Callback(){
            @Override
            public void surfaceCreated(SurfaceHolder holder){
                try {
                    //通过多媒体格式名创建一个可用的解码器
                    mPlayCodec = MediaCodec.createDecoderByType("video/avc");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //初始化编码器
                final MediaFormat mediaformat = MediaFormat.createVideoFormat("video/avc", Video_Width, Video_Height);

                //获取h264中的pps及sps数据
                if (isUsePpsAndSps) {
                    byte[] header_sps = {0, 0, 0, 1, 103, 66, 0, 42, (byte) 149, (byte) 168, 30, 0, (byte) 137, (byte) 249, 102, (byte) 224, 32, 32, 32, 64};
                    byte[] header_pps = {0, 0, 0, 1, 104, (byte) 206, 60, (byte) 128, 0, 0, 0, 1, 6, (byte) 229, 1, (byte) 151, (byte) 128};
                    mediaformat.setByteBuffer("csd-0", ByteBuffer.wrap(header_sps));
                    mediaformat.setByteBuffer("csd-1", ByteBuffer.wrap(header_pps));
                }
                //设置帧率
                mediaformat.setInteger(MediaFormat.KEY_FRAME_RATE, PlayFrameRate);
                mPlayCodec.configure(mediaformat, mPlaySurfaceHolder.getSurface(), null, 0);
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }
    private void startDecodingThread() {
        mPlayCodec.start();
        mDecodeThread = new Thread(new decodeH264Thread());
        mDecodeThread.start();
    }
    private boolean isRecv = false;
    private Socket recvSocket = null;
    private class recvSocketThread extends Thread{
        @Override
        public void run() {
            super.run();
//            while(true){
//                if (isRecv){
                    try {
                        
                        recvSocket = new Socket("10.105.36.224",18888);
//                        recvSocket = new Socket("192.168.1.105",18888);
//                        recvSocket = new Socket("10.1.1.1",8888);
//                        recvSocket = new Socket("10.202.0.199",18888);
//                        recvSocket = new Socket("10.202.1.69",18888);
                        Log.d("ssssssssssssssss","okay");
                        InputStream ins = recvSocket.getInputStream();

                        getVideoDataQueue().clear();
                        while(true){
                        while(isRecv){
                            byte[] readByte = new byte[2000];
                            int n;
                            int cnt = 0;
                            while((n = ins.read(readByte))!=-1){
                                cnt++;
                                Log.d("ssssssssssss","receive"+cnt);
                                byte[] toOffer = new byte[n];
                                System.arraycopy(readByte,0,toOffer,0,n);
                                getVideoDataQueue().offer(toOffer);
                                Log.d("ssssssssssssssss",""+getVideoDataQueue().size());
                            }
                        }
                        }
//                        isRecv = false;

                    }catch (IOException e){
                        Log.d("ssssssssssssssss","wrong");
                    }
//                }

//            }

        }
    }

    private class decodeH264Thread implements Runnable{
        @Override
        public void run() {

                    try {
                        decodeLoop();
//                        isPlay = false;
        //                decodeLoop1();
                    } catch (Exception e) {}

        }
        //标记 camera旧api 已改

//        boolean mStopFlag =false;
        private byte[] streamBuffer = null;
        private void decodeLoop(){

//            ByteBuffer[] inputBuffers = mPlayCodec.getInputBuffers();
            //解码后的数据，包含每一个buffer的元数据信息，例如偏差，在相关解码器中有效的数据大小


            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            long startMs = System.currentTimeMillis();
            long timeoutUs = 10000;


            while (true){
                if(isRecv){
                    int inIndex = mPlayCodec.dequeueInputBuffer(timeoutUs);
                    if (inIndex >= 0) {
                        ByteBuffer byteBuffer = mPlayCodec.getInputBuffer(inIndex);
//                        ByteBuffer byteBuffer = inputBuffers[inIndex];
                        byteBuffer.clear();

                        //放入一帧数据
//                        byte[] b = getAFrame();
//                        byteBuffer.put(b);

                        if (getVideoDataQueue().size()>10){
                        byte[] b = getOneNalu();
//                        byte[] b= (byte[]) getVideoDataQueue().poll();
                            byteBuffer.put(b);
                            Log.d(TAG,"put one nalu");
//                        try{
//                            Thread.sleep(0);
//                        }catch (InterruptedException e){}
                            mPlayCodec.queueInputBuffer(inIndex, 0, b.length, 0, 0);
                        }else {
                            mPlayCodec.queueInputBuffer(inIndex, 0, 0, 0, 0);
                        }

                    } else {
                        continue;
                    }
//                    int outIndex = mPlayCodec.dequeueOutputBuffer(info, timeoutUs);
//                    if (outIndex >= 0) {
//                        while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
//                            try {
//                                Thread.sleep(100);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
//
//                        boolean doRender = (info.size != 0);
//                        mPlayCodec.releaseOutputBuffer(outIndex, doRender);
//                    } else {
//                        Log.e(TAG, "no output");
//                    }
                }
            }

        }

    }

//        int startIndex = 0;
//        private  int nextFrameStart = 0;
//        private int end;
//        private byte[] naluHeader = new byte[]{0, 0, 0, 1};
//        public byte[] getAFrame(){
//            nextFrameStart = KMPMatch(naluHeader, streamBuffer, startIndex + 2, end);
//            byte[] result = new byte[nextFrameStart - startIndex];
//            System.arraycopy(streamBuffer, startIndex, result,0, nextFrameStart - startIndex);
//            startIndex = nextFrameStart;
//            return result;
//        }


//    public static byte[] getBytes(InputStream is) throws IOException {
//        int len;
//        int size = 1024;
//        byte[] buf;
//        if (is instanceof ByteArrayInputStream) {
//            size = is.available();
//            buf = new byte[size];
//            len = is.read(buf, 0, size);
//        } else {
////            BufferedOutputStream bos=new BufferedOutputStream(new ByteArrayOutputStream());
//            ByteArrayOutputStream bos = new ByteArrayOutputStream();
//            buf = new byte[size];
//            while ((len = is.read(buf, 0, size)) != -1)
//                bos.write(buf, 0, len);
//            buf = bos.toByteArray();
//        }
//        Log.e("----------", "bbbb");
//        return buf;
//    }
//
//    private int KMPMatch(byte[] pattern, byte[] bytes, int start, int remain) {
//        try {
//            Thread.sleep(30);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        int[] lsp = computeLspTable(pattern);
//
//        int j = 0;  // Number of chars matched in pattern
//        for (int i = start; i < remain; i++) {
//            while (j > 0 && bytes[i] != pattern[j]) {
//                // Fall back in the pattern
//                j = lsp[j - 1];  // Strictly decreasing
//            }
//            if (bytes[i] == pattern[j]) {
//                // Next char matched, increment position
//                j++;
//                if (j == pattern.length)
//                    return i - (j - 1);
//            }
//        }
//        return -1;  // Not found
//    }
//
//    private int[] computeLspTable(byte[] pattern) {
//        int[] lsp = new int[pattern.length];
//        lsp[0] = 0;  // Base case
//        for (int i = 1; i < pattern.length; i++) {
//            // Start by assuming we're extending the previous LSP
//            int j = lsp[i - 1];
//            while (j > 0 && pattern[i] != pattern[j])
//                j = lsp[j - 1];
//            if (pattern[i] == pattern[j])
//                j++;
//            lsp[i] = j;
//        }
//        return lsp;
//    }


    //-----------------------------------------------------------
    private BlockingQueue<byte[]> video_data_Queue = new ArrayBlockingQueue<byte[]>(10000);
    public BlockingQueue getVideoDataQueue(){
        return video_data_Queue;
    }
    private byte[] currentBuff = new byte[102400];

    private byte[] naluHead = {0,0,0,1};
    private byte[] lsp = {0,1,2,0};
    private int currentBuffStart = 0;//valid data start
    private int currentBuffEnd = 0;
    int cnt = 0;

    public byte[] getOneNalu(){
        int n = getNextIndex();
        if (n == -1)return null;
//        Log.d(TAG,"get one"+n);
        byte[] naluu = new byte[n-currentBuffStart];
        System.arraycopy(currentBuff, currentBuffStart, naluu, 0, n-currentBuffStart);

        //handle currentBuff
        System.arraycopy(currentBuff, n , currentBuff, 0, currentBuff.length - n);

        //set index
        currentBuffStart = 0;
        currentBuffEnd = currentBuffEnd - naluu.length;
        return naluu;
    }
    //added by deonew
    private int nextNaluHead = -1;
    public int getNextIndex(){
        nextNaluHead = getNextIndexOnce();

        //currentBuff don't contain a nalu
        //poll data
        while(nextNaluHead == -1) {
            if (getVideoDataQueue().isEmpty()){break;}
//                break;
            byte[] tmp = (byte[])getVideoDataQueue().poll();
//            if (tmp == null)
//                return nextNaluHead;

            System.arraycopy(tmp,0,currentBuff,currentBuffEnd,tmp.length);
            currentBuffEnd = currentBuffEnd + tmp.length;
            nextNaluHead = getNextIndexOnce();
            cnt++;
//            Log.d(TAG,"poll"+cnt);
        }
        nextNaluHead = nextNaluHead - 3;
        // currentBuffStart = nextNaluHead;
        return nextNaluHead;
    }

    //get next 000000[01]
    public int getNextIndexOnce(){
        int nextIndex = -1;
        byte[] naluHead = {0,0,0,1};
        byte[] correctBuff = {0,1,2,0};
        int i = 0;
        int index = 0;
        for(i = currentBuffStart+1; i < currentBuffEnd;i++){
            while (index > 0 && currentBuff[i] != naluHead[index]) {
                index = correctBuff[index - 1];
            }
            if (currentBuff[i] == naluHead[index]) {
                index++;
                if (index == 4){
                    nextIndex = i;//i = 00000001中的01
                    break;
                }
            }
        }
        return nextIndex;
    }

    class decodeH2Thread extends Thread{
        @Override
        public void run() {
            super.run();
            while(true){
                while (isPlay){
                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                    long startMs = System.currentTimeMillis();
                    long timeoutUs = 10000;
                    int outIndex = mPlayCodec.dequeueOutputBuffer(info, timeoutUs);
                    Log.d(TAG,"get output");
//                    switch (outIndex){
//                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
//                            Log.d(TAG, "New format " + mPlayCodec.getOutputFormat());
//                            break;
//                        case MediaCodec.INFO_TRY_AGAIN_LATER:
//                            try {
//                                sleep(10);
//                            } catch (InterruptedException e1) {
//                                e1.printStackTrace();
//                                return;
//                            }
//                            break;
//                        default:
//                            while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
//                                try {
//                                    Thread.sleep(100);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                            }
////                            mPlayCodec.releaseOutputBuffer(outIndex, true);
//                            boolean doRender = (info.size != 0);
//                            mPlayCodec.releaseOutputBuffer(outIndex, doRender);
//                            break;
//                    }
                    if (outIndex<0){
                        Log.d(TAG,outIndex+"");//-1
                        continue;
                    }
                    if (outIndex >= 0) {
                        while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.d(TAG,"render");
                        boolean doRender = (info.size != 0);
                        mPlayCodec.releaseOutputBuffer(outIndex, doRender);
                        Log.e(TAG, "no output");
                        try {
                            Log.e(TAG, "sleep");
                            Thread.sleep(10);
//                            isPlay = true;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {

                    }
                }
            }
        }
    }
}

