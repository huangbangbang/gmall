package com.atguigu.gmall.cart;

import java.util.Scanner;

public class huawei010 {
    public static void main(String[] args) {
        int m ,n;
        int a ,b;
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入人数和操作次数");
        while (scanner.hasNext()){
            m=scanner.nextInt();
            n=scanner.nextInt();
            //System.out.println(m+"  "+n);
            System.out.println("请分别输入各人的分数");
            int[] score = new int[m];
            for (int i = 0;i<m;i++){
                score[i] = scanner.nextInt();
            }
            String o = null;
            System.out.println("请输入操作 例如 Q 1 5");
            for (int i = 0;i<n;i++){
                o=scanner.next();
                a=scanner.nextInt();
                b=scanner.nextInt();
                operation(o,a,b,score);
            }
            scanner.close();
        }

    }

    private static void operation(String o, int a, int b, int[] score) {
        int begin;
        int end;
        int max;
        if (o.equals("Q")){
            end = Math.max(a,b);
            begin= Math.min(a,b)-1;
            max = score[begin];
            for (int i =begin;i<end;i++){
                if (max<score[i]){
                    max=score[i];
                }
            }
            System.out.println("最高分  "+max);

        }else if (o.equals("U")){
            score[a-1]=b;
            System.out.println("更新成功");
        }
    }
}
