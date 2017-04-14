package com.example.comingbear.self_decode;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
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
        //保持屏幕常亮
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);


//        fileToQueue();

        String filePath =  Environment.getExternalStorageDirectory() + "/carTempRecv.264";
        init(filePath);





//
//        fileToQueue();
//
//        initFos();
    }

    private FileOutputStream fos;
    public void initFos (){
        try{
            fos = new FileOutputStream(new File(Environment.getExternalStorageDirectory() + "/carTempRecvEveryFrameToPut.264"));
        }catch (IOException e){}
    }

    private boolean isPlay = false;
    public void startPlay(View v){
//        String filePath =  Environment.getExternalStorageDirectory() + "/carTempRecv.264";
//        init(filePath);
        startDecodingThread();

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
        mPlaySurface = (SurfaceView) findViewById(R.id.preview);
        mPlaySurfaceHolder = mPlaySurface.getHolder();
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
                //https://developer.android.com/reference/android/media/MediaFormat.html#KEY_MAX_INPUT_SIZE
                //设置配置参数，参数介绍 ：
                // format   如果为解码器，此处表示输入数据的格式；如果为编码器，此处表示输出数据的格式。
                //surface   指定一个surface，可用作decode的输出渲染。
                //crypto    如果需要给媒体数据加密，此处指定一个crypto类.
                //   flags  如果正在配置的对象是用作编码器，此处加上CONFIGURE_FLAG_ENCODE 标签。
                mPlayCodec.configure(mediaformat, holder.getSurface(), null, 0);
//                startDecodingThread();
            }
            //这两个函数的重写，大概是为了不想继承父类该函数的效果？也许
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
//                        recvSocket = new Socket("10.105.36.224",18888);
                        recvSocket = new Socket("192.168.1.105",18888);
//                        recvSocket = new Socket("10.1.1.1",8888);
                        Log.d("ssssssssssssssss","okay");
                        InputStream ins = recvSocket.getInputStream();

                        video_data_Queue.clear();
                        while(true){
                        while(isRecv){
                            byte[] readByte = new byte[2000];
                            int n;
                            while((n = ins.read(readByte))!=-1){
                                Log.d("ssssssssssss","receive");
                                byte[] toOffer = new byte[n];
                                System.arraycopy(readByte,0,toOffer,0,n);
                                video_data_Queue.offer(toOffer);
                                Log.d("ssssssssssssssss",""+video_data_Queue.size());
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

//            while(true){
//                if (isPlay){
//                    try {
//                        decodeLoop();
//                        isPlay = false;
//        //                decodeLoop1();
//                    } catch (Exception e) {}
//                }
//            }

        }
        //标记 camera旧api 已改

        private byte[] streamBuffer = null;
        private void decodeLoop(){

//            ByteBuffer[] inputBuffers = mPlayCodec.getInputBuffers();
            //解码后的数据，包含每一个buffer的元数据信息，例如偏差，在相关解码器中有效的数据大小


            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            long startMs = System.currentTimeMillis();
            long timeoutUs = 10000;


            try {
                streamBuffer = getBytes(mPlayInputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
//            int bytes_cnt = 0;
            while (mStopFlag == false){
                end = streamBuffer.length;
                while (!mStopFlag){
                    int inIndex = mPlayCodec.dequeueInputBuffer(timeoutUs);
                    if (inIndex >= 0) {
                        ByteBuffer byteBuffer = mPlayCodec.getInputBuffer(inIndex);
//                        ByteBuffer byteBuffer = inputBuffers[inIndex];
                        byteBuffer.clear();

                        //放入一帧数据
//                        byte[] b = getAFrame();
//                        byteBuffer.put(b);

                        //队列获取文件数据
                        byte[] b = getOneNalu();
                        byteBuffer.put(b);
                        try{
                            Thread.sleep(30);
                        }catch (InterruptedException e){}
//                        Log.d("llllllll",b.length+"");

                        //在给指定Index的inputbuffer[]填充数据后，调用这个函数把数据传给解码器
//                        mPlayCodec.queueInputBuffer(inIndex, 0, nextFrameStart - startIndex, 0, 0);
                        mPlayCodec.queueInputBuffer(inIndex, 0, b.length, 0, 0);

                    } else {
                        continue;
                    }
                    int outIndex = mPlayCodec.dequeueOutputBuffer(info, timeoutUs);
                    if (outIndex >= 0) {
                        while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        boolean doRender = (info.size != 0);
                        mPlayCodec.releaseOutputBuffer(outIndex, doRender);
                    } else {
                        Log.e(TAG, "no output");
                    }
                }
                mStopFlag = true;
                mHandler.sendEmptyMessage(0);
            }

        }

        int startIndex = 0;
        private  int nextFrameStart = 0;
        private int end;
        private byte[] naluHeader = new byte[]{0, 0, 0, 1};
        public byte[] getAFrame(){
            nextFrameStart = KMPMatch(naluHeader, streamBuffer, startIndex + 2, end);
            byte[] result = new byte[nextFrameStart - startIndex];
            System.arraycopy(streamBuffer, startIndex, result,0, nextFrameStart - startIndex);
            startIndex = nextFrameStart;
            return result;
        }

    }

    public static byte[] getBytes(InputStream is) throws IOException {
        int len;
        int size = 1024;
        byte[] buf;
        if (is instanceof ByteArrayInputStream) {
            size = is.available();
            buf = new byte[size];
            len = is.read(buf, 0, size);
        } else {
//            BufferedOutputStream bos=new BufferedOutputStream(new ByteArrayOutputStream());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            buf = new byte[size];
            while ((len = is.read(buf, 0, size)) != -1)
                bos.write(buf, 0, len);
            buf = bos.toByteArray();
        }
        Log.e("----------", "bbbb");
        return buf;
    }

    private int KMPMatch(byte[] pattern, byte[] bytes, int start, int remain) {
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int[] lsp = computeLspTable(pattern);

        int j = 0;  // Number of chars matched in pattern
        for (int i = start; i < remain; i++) {
            while (j > 0 && bytes[i] != pattern[j]) {
                // Fall back in the pattern
                j = lsp[j - 1];  // Strictly decreasing
            }
            if (bytes[i] == pattern[j]) {
                // Next char matched, increment position
                j++;
                if (j == pattern.length)
                    return i - (j - 1);
            }
        }
        return -1;  // Not found
    }

    private int[] computeLspTable(byte[] pattern) {
        int[] lsp = new int[pattern.length];
        lsp[0] = 0;  // Base case
        for (int i = 1; i < pattern.length; i++) {
            // Start by assuming we're extending the previous LSP
            int j = lsp[i - 1];
            while (j > 0 && pattern[i] != pattern[j])
                j = lsp[j - 1];
            if (pattern[i] == pattern[j])
                j++;
            lsp[i] = j;
        }
        return lsp;
    }

    public void fileToQueue(){
        File f = new File(Environment.getExternalStorageDirectory() + "/carTempRecv.264");
        try {
            FileInputStream fin = new FileInputStream(f);
            int cnt = 0;
            int hhh = 0;
            do{
                byte[] i = new byte[1024];
                hhh = fin.read(i);
                video_data_Queue.offer(i);
                cnt++;
            }while(hhh != -1);

//            Log.d("aaaaaaaaaaaaaaaaaaaaaa",cnt+"");
        }catch (IOException e){}
//        catch (InterruptedException e){}
    }


    //-----------------------------------------------------------
    private BlockingQueue<byte[]> video_data_Queue = new ArrayBlockingQueue<byte[]>(10000);
    private byte[] currentBuff = new byte[102400];

    private byte[] naluHead = {0,0,0,1};
    private byte[] lsp = {0,1,2,0};
    private int currentBuffStart = 0;//valid data start
    private int currentBuffEnd = 0;
    int cnt = 0;

    public byte[] getOneNalu(){
        int n = getNextIndex();
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
            if (video_data_Queue.isEmpty()){break;}
//                break;
            byte[] tmp = video_data_Queue.poll();
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
}

