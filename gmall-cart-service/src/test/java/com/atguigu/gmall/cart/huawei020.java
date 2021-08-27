package com.atguigu.gmall.cart;

import java.util.*;

public class huawei020 {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        String file = null;
        int line;
        int index;
        ErrorLog errorLog;
        Map<String,ErrorLog> map = new HashMap<>();
        while (in.hasNext()){

            file = in.next();
            line = in.nextInt();
            index = file.lastIndexOf("\\");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(index>0?file.substring(index+1):file).append(" ").append(line);

            String key = stringBuilder.toString();
            errorLog = map.get(key);
            if (errorLog==null){
                map.put(key,new ErrorLog(key,1));
            }else {
                errorLog.size++;
            }
        }

        in.close();

        List<ErrorLog> list = new ArrayList<>(map.values());

        Comparator<ErrorLog> comparator = new Comparator<ErrorLog>() {
            @Override
            public int compare(ErrorLog o1, ErrorLog o2) {
                return o2.size-o1.size==0?(o1.mark-o2.mark):o2.size-o1.size;
            }
        };
        Collections.sort(list,comparator);
        int length = list.size()<8?list.size():8;
        for (int i = 0;i<length;i++){


            System.out.println(list.get(i).name.lastIndexOf(" ")>16?list.get(i).name.substring(list.get(i).name.lastIndexOf(" ")-16):list.get(i).name+" "+list.get(i).size);
        }



    }
}
class ErrorLog{
    private static int count;
    String name;
    int mark=count++;
    int size;

    public ErrorLog(String name, int size) {
        this.name = name;
        this.size = size;
    }
}

