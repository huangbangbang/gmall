package com.atguigu.gmall.cart;

import java.util.Scanner;

public class huawei030 {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        String[] line,light,right;
        String input ,output;
        while (in.hasNext()){
            input = in.nextLine();
            if (input.contains("joker JOKER")){
                output = "joker JOKER";
            }else {
                line =input.split("-");
                light = line[0].split(" ");
                right = line[1].split(" ");
                if (light.length==4&&right.length!=4){
                    output=line[0];
                }else if(light.length!=4&&right.length==4){
                    output=line[1];
                }else if (light.length==right.length){
                    if (count(light[0])>count(right[0])){
                        output=line[0];
                    }else {
                        output=line[1];
                    }
                }else {
                    output="error";
                }
            }


            System.out.println(output);
        }

    }

    private static int count(String s) {
        return "345678910JQKA2jokerJOKER".indexOf(s);
    }
}
