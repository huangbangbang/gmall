package com.atguigu.gmall.cart;

import com.atguigu.gmall.CartProvider;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class test {
    public static void main(String[] args) {
        MyClassLoader myClassLoader = new MyClassLoader("D:/");

        try {
            Class<?> cartServiceImpl = myClassLoader.loadClass("User");
            System.out.println(cartServiceImpl.getClassLoader().getClass().getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}

class MyClassLoader extends ClassLoader{
    private String path;

    public MyClassLoader(ClassLoader parent, String path) {
        super(parent);
        this.path = path;
    }

    public MyClassLoader(String path) {
        this.path = path;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        BufferedInputStream bis = null;
        ByteArrayOutputStream baos = null;
        try {
            String file = path + name + ".class";
            bis = new BufferedInputStream(new FileInputStream(file));
            baos = new ByteArrayOutputStream();

            int len;
            byte[] data = new byte[1024];
            while ((len = bis.read(data)) != -1) {
                baos.write(data, 0, len);
            }
            byte[] bytes = baos.toByteArray();
            Class<?> aClass = defineClass(null, bytes, 0, bytes.length);
            return aClass;
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if (bis!=null){
                bis.close();
            }
            } catch (IOException e) {
                e.printStackTrace();
            }
            ;
            try {
                if (baos!=null){
                    baos.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
