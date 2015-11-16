package cn.edu.fudan.se.weibo;

import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.net.RequestListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by Jack on 2015/11/11.
 */
public class FriendsListRequest implements RequestListener {

    @Override
    public void onComplete(String response) {

        System.out.println("\n Hello World Begin : \n" + response + "\n Hello World End : \n");
        //TODO send the friend list to the server.
    }

    @Override
    public void onComplete4binary(ByteArrayOutputStream responseOS) {

    }

    @Override
    public void onIOException(IOException e) {

    }

    @Override
    public void onError(WeiboException e) {

    }
}
