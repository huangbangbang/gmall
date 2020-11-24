package com.atguigu.gmall.manage.util;

import org.csource.common.MyException;
import org.csource.fastdfs.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class PmsUploadUtil {

    public static String upload(MultipartFile multipartFile) {
        TrackerServer ts = null;
        StorageServer ss = null;
        try {
            ClientGlobal.init("tracker.conf");
            TrackerClient tc = new TrackerClient();
            ts = tc.getConnection();
            ss = tc.getStoreStorage(ts);

            StorageClient sc = new StorageClient();
            byte[] buffFile = multipartFile.getBytes();
            String fileName = multipartFile.getOriginalFilename();
            String fileExtName = fileName.substring(fileName.lastIndexOf(".")+1);
            String[] result= sc.upload_file(buffFile,fileExtName,null);
            String url = "http://192.168.17.128";
            for (String s : result) {
                url+="/"+s;
            }
            return url;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }finally {

            if (ss!=null){
                try {
                    ss.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (ts!=null){
                try {
                    ts.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        }
        return null;
    }

}
