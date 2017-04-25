package com.example.comingbear.self_decode;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by deonew on 4/16/17.
 */

public class PhoneServer {
    MainActivity mainActivity;
    public PhoneServer(MainActivity ac){
        this.mainActivity = ac;
    }


    public void start(){
        new Listen().start();
    }
    class Listen extends  Thread{
        @Override
        public void run() {
            super.run();
            try {
                System.out.println("开始监听");
                ServerSocket ss = new ServerSocket(18888);
                Log.d("ssssssssssssssss","listen");
                while (true) {
                    Socket s = ss.accept();
                    InputStream is = s.getInputStream();
                    byte[] recBuff = new byte[1024];
                    int size = 0;
                    while((size = is.read(recBuff))!=-1){
                        //put to queue
                        byte[] toOffer = new byte[size];
                        System.arraycopy(recBuff,0,toOffer,0,size);
                        mainActivity.getVideoDataQueue().offer(toOffer);
                    }
                }
            }catch (IOException e){}
        }
    }
}
