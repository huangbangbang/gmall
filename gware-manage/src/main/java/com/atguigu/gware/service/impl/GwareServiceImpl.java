package com.atguigu.gware.service.impl;


import com.alibaba.fastjson.JSON;

import com.atguigu.gware.bean.WmsWareInfo;
import com.atguigu.gware.bean.WmsWareSku;
import com.atguigu.gware.util.ActiveMQUtil;
import com.atguigu.gware.util.HttpclientUtil;
import com.atguigu.gware.bean.WmsWareOrderTask;
import com.atguigu.gware.bean.WmsWareOrderTaskDetail;
import com.atguigu.gware.enums.TaskStatus;
import com.atguigu.gware.mapper.WareInfoMapper;
import com.atguigu.gware.mapper.WareOrderTaskDetailMapper;
import com.atguigu.gware.mapper.WareOrderTaskMapper;
import com.atguigu.gware.mapper.WareSkuMapper;
import com.atguigu.gware.service.GwareService;
import org.apache.activemq.command.ActiveMQMapMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.*;



@Service
public class GwareServiceImpl implements GwareService {

     @Autowired
     WareSkuMapper wareSkuMapper;

    @Autowired
    WareInfoMapper wareInfoMapper;

    @Autowired
    WareOrderTaskMapper wareOrderTaskMapper;

    @Autowired
    WareOrderTaskDetailMapper wareOrderTaskDetailMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Value("${order.split.url}")
    private String ORDER_URL;

     public Integer  getStockBySkuId(String skuid){
         Integer stock = wareSkuMapper.selectStockBySkuid(skuid);

         return stock;
     }


    public boolean  hasStockBySkuId(String skuid,Integer num){
        Integer stock = getStockBySkuId(  skuid);

        if(stock==null||stock<num){
            return false;
        }
        return true;
    }


    public List<WmsWareInfo> getWareInfoBySkuid(String skuid){
        List<WmsWareInfo> wmsWareInfos = wareInfoMapper.selectWareInfoBySkuid(skuid);
        return wmsWareInfos;
    }

    public List<WmsWareInfo> getWareInfoList(){
        List<WmsWareInfo> wmsWareInfos = wareInfoMapper.selectAll();
        return wmsWareInfos;
    }


    public void addWareInfo(){
        WmsWareInfo wmsWareInfo =new WmsWareInfo();
        wmsWareInfo.setAddress("1123");
        wmsWareInfo.setAreacode("123123");
        wmsWareInfo.setName("123123");
        wareInfoMapper.insertSelective(wmsWareInfo);


        WmsWareSku wmsWareSku =new WmsWareSku();
        wmsWareSku.setId(wmsWareInfo.getId());
        wmsWareSku.setWarehouseId("991");
        wareSkuMapper.insertSelective(wmsWareSku);
    }


    public   Map<String,List<String>>  getWareSkuMap(List<String> skuIdlist){
        Example example=new Example(WmsWareSku.class);
        example.createCriteria().andIn("skuId",skuIdlist);
        List<WmsWareSku> wmsWareSkuList = wareSkuMapper.selectByExample(example);

        Map<String,List<String>> wareSkuMap=new HashMap<>();

        for (WmsWareSku wmsWareSku : wmsWareSkuList) {
            List<String>  skulistOfWare = wareSkuMap.get(wmsWareSku.getWarehouseId());
            if (skulistOfWare==null){
                skulistOfWare=new ArrayList<>();
            }
            skulistOfWare.add(wmsWareSku.getSkuId());
            wareSkuMap.put(wmsWareSku.getWarehouseId(),skulistOfWare);
        }

        return  wareSkuMap;

    }


    public  List<Map<String,Object>>  convertWareSkuMapList( Map<String,List<String>>  wareSkuMap){

        List<Map<String,Object>> wareSkuMapList=new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : wareSkuMap.entrySet()) {
            Map<String,Object> skuWareMap=new HashMap<>();
            String wareid= entry.getKey();
            skuWareMap.put("wareId",wareid);
            List<String> skuids = entry.getValue();
            skuWareMap.put("skuIds",skuids);
            wareSkuMapList.add(skuWareMap);
        }
        return wareSkuMapList;

    }


    public void addWareSku(WmsWareSku wmsWareSku){
        wareSkuMapper.insertSelective(wmsWareSku);
    }

    public List<WmsWareSku> getWareSkuList(){
        List<WmsWareSku> wmsWareSkuList = wareSkuMapper.selectWareSkuAll();
        return wmsWareSkuList;
    }

    public WmsWareOrderTask getWareOrderTask(String taskId){

        WmsWareOrderTask wareOrderTask  = wareOrderTaskMapper.selectByPrimaryKey(taskId);

        WmsWareOrderTaskDetail wmsWareOrderTaskDetail =new WmsWareOrderTaskDetail();
        wmsWareOrderTaskDetail.setTaskId(taskId);
        List<WmsWareOrderTaskDetail> details = wareOrderTaskDetailMapper.select(wmsWareOrderTaskDetail);
        wareOrderTask.setDetails(details);
        return wareOrderTask;
    }




    /***
     * 出库操作  减库存和锁定库存，
     * @param taskExample
     */
    @Transactional
    public void deliveryStock(WmsWareOrderTask taskExample)  {
        String trackingNo = taskExample.getTrackingNo();
        WmsWareOrderTask wareOrderTask=getWareOrderTask(  taskExample.getId());
        wareOrderTask.setTaskStatus(TaskStatus.DELEVERED);
        List<WmsWareOrderTaskDetail> details = wareOrderTask.getDetails();
        for (WmsWareOrderTaskDetail detail : details) {
                WmsWareSku wmsWareSku =new WmsWareSku();
                wmsWareSku.setWarehouseId(wareOrderTask.getWareId());
                wmsWareSku.setSkuId(detail.getSkuId());
                wmsWareSku.setStock(detail.getSkuNum());
                wareSkuMapper.deliveryStock(wmsWareSku);
        }

        wareOrderTask.setTaskStatus(TaskStatus.DELEVERED);
        wareOrderTask.setTrackingNo(trackingNo);
        wareOrderTaskMapper.updateByPrimaryKeySelective(wareOrderTask);
        try {
            sendToOrder(wareOrderTask);
        } catch(JMSException e){
            e.printStackTrace();
        }
    }


    public void sendToOrder(WmsWareOrderTask wareOrderTask) throws JMSException{
            Connection conn = activeMQUtil.getConnection();

            Session session = conn.createSession(true, Session.SESSION_TRANSACTED);
            Destination destination = session.createQueue("SKU_DELIVER_QUEUE");
            MessageProducer producer = session.createProducer(destination);
            MapMessage mapMessage=new ActiveMQMapMessage();
            mapMessage.setString("orderId",wareOrderTask.getOrderId());
            mapMessage.setString("status",wareOrderTask.getTaskStatus().toString()); //小细节 枚举
            mapMessage.setString("trackingNo",wareOrderTask.getTrackingNo());

            producer.send(mapMessage);
            session.commit();

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public  List<WmsWareOrderTask>   checkOrderSplit(WmsWareOrderTask wareOrderTask) {
        List<WmsWareOrderTaskDetail> details = wareOrderTask.getDetails();
        List<String> skulist = new ArrayList<>();
        for (WmsWareOrderTaskDetail detail : details) {
            skulist.add(detail.getSkuId());
        }
        Map<String, List<String>> wareSkuMap = getWareSkuMap(skulist);
        // 一次物流运输无法完成订单
        if (wareSkuMap.entrySet().size()<2) {
            Map.Entry<String, List<String>> entry = wareSkuMap.entrySet().iterator().next();
            String wareid = entry.getKey();
            wareOrderTask.setWareId(wareid);
        } else {
            //需要拆单
            List<Map<String, Object>> wareSkuMapList = convertWareSkuMapList(wareSkuMap);
            String jsonString = JSON.toJSONString(wareSkuMapList);
            Map<String, String> map = new HashMap<>();
            map.put("orderId", wareOrderTask.getOrderId());
            map.put("wareSkuMap", jsonString);
            
            // 调用订单系统拆单接口
            String resultJson = HttpclientUtil.doPost(  ORDER_URL, map);

            List<WmsWareOrderTask> wareOrderTaskList = JSON.parseArray(resultJson, WmsWareOrderTask.class);
            
            if(wareOrderTaskList==null){
                wareOrderTaskList = new ArrayList<>();
                for (WmsWareOrderTaskDetail detail : details) {
                    WmsWareOrderTask wareOrderTask1 = new WmsWareOrderTask();

                    List<WmsWareOrderTaskDetail> wmsWareOrderTaskDetails = new ArrayList<>();
                    wmsWareOrderTaskDetails.add(detail);
                    wareOrderTask1.setDetails(wmsWareOrderTaskDetails);
                    String skuId = detail.getSkuId();
                    WmsWareSku wmsWareSku = new WmsWareSku();
                    wmsWareSku.setSkuId(skuId);
                    wmsWareSku.setStock(null);
                    List<WmsWareSku> select = wareSkuMapper.select(wmsWareSku);
                    wareOrderTask1.setWareId(select.get(0).getWarehouseId());

                    wareOrderTaskList.add(wareOrderTask1);
                }
            }
            
            
            if (wareOrderTaskList.size()>=2){
//                for (WareOrderTask subOrderTask : wareOrderTaskList) {
//                    subOrderTask.setTaskStatus(TaskStatus.DEDUCTED);
//                    saveWareOrderTask(subOrderTask);
//                }
//                updateStatusWareOrderTaskByOrderId(wareOrderTask.getOrderId(),TaskStatus.SPLIT);
                return wareOrderTaskList;
            }else{
                throw new  RuntimeException("拆单异常!!");
            }

        }

        return  null;
    }


        public WmsWareOrderTask saveWareOrderTask(WmsWareOrderTask wareOrderTask ){
            wareOrderTask.setCreateTime(new Date());
            WmsWareOrderTask wareOrderTaskQuery=new WmsWareOrderTask();
            wareOrderTaskQuery.setOrderId(wareOrderTask.getOrderId());

            WmsWareOrderTask wareOrderTaskOrigin = wareOrderTaskMapper.selectOne(wareOrderTaskQuery);
            if(wareOrderTaskOrigin!=null){
                return  wareOrderTaskOrigin;
            }

            wareOrderTaskMapper.insert(wareOrderTask);

            List<WmsWareOrderTaskDetail> wmsWareOrderTaskDetails = wareOrderTask.getDetails();
            for (WmsWareOrderTaskDetail wmsWareOrderTaskDetail : wmsWareOrderTaskDetails) {
                wmsWareOrderTaskDetail.setTaskId(wareOrderTask.getId());
                wareOrderTaskDetailMapper.insert(wmsWareOrderTaskDetail);
            }
            return wareOrderTask;

        }


        public void updateStatusWareOrderTaskByOrderId(String orderId,TaskStatus taskStatus){
            Example example=new Example(WmsWareOrderTask.class);
            example.createCriteria().andEqualTo("orderId",orderId);
            WmsWareOrderTask wareOrderTask=new WmsWareOrderTask();
            wareOrderTask.setTaskStatus(taskStatus);
            wareOrderTaskMapper.updateByExampleSelective(wareOrderTask,example);
        }


    /***
     * 库存锁定成功，准备出库，发送消息，由订单系统消费，修改状态为商品准备出库
     * @param wareOrderTask
     * @throws JMSException
     */
    public void sendSkuDeductMQ(WmsWareOrderTask wareOrderTask) throws JMSException{
        Connection conn = activeMQUtil.getConnection();

    Session session = conn.createSession(true, Session.SESSION_TRANSACTED);
    Destination destination = session.createQueue("SKU_DEDUCT_QUEUE");
    MessageProducer producer = session.createProducer(destination);
    MapMessage mapMessage=new ActiveMQMapMessage();
        mapMessage.setString("orderId",wareOrderTask.getOrderId());
        mapMessage.setString("status",wareOrderTask.getTaskStatus().toString());
        producer.send(mapMessage);
        session.commit();
}

    @Transactional
    public void lockStock(WmsWareOrderTask wareOrderTask) {
        List<WmsWareOrderTaskDetail> wmsWareOrderTaskDetails = wareOrderTask.getDetails();
        String comment = "";
        for (WmsWareOrderTaskDetail wmsWareOrderTaskDetail : wmsWareOrderTaskDetails) {

            WmsWareSku wmsWareSku = new WmsWareSku();
            wmsWareSku.setWarehouseId(wareOrderTask.getWareId());
            wmsWareSku.setStockLocked(wmsWareOrderTaskDetail.getSkuNum());
            wmsWareSku.setSkuId(wmsWareOrderTaskDetail.getSkuId());

            int availableStock = wareSkuMapper.selectStockBySkuidForUpdate(wmsWareSku); //查询可用库存 加行级写锁 注意索引避免表锁
            if (availableStock - wmsWareOrderTaskDetail.getSkuNum() < 0) {
                comment += "减库存异常：名称：" + wmsWareOrderTaskDetail.getSkuName() + "，实际可用库存数" + availableStock + ",要求库存" + wmsWareOrderTaskDetail.getSkuNum();
            }
        }

        if (comment.length() > 0) {   //库存超卖 记录日志，返回错误状态
            wareOrderTask.setTaskComment(comment);
            wareOrderTask.setTaskStatus(TaskStatus.OUT_OF_STOCK);
            updateStatusWareOrderTaskByOrderId(wareOrderTask.getOrderId(),TaskStatus.OUT_OF_STOCK);

        } else {   //库存正常  进行减库存
            for (WmsWareOrderTaskDetail wmsWareOrderTaskDetail : wmsWareOrderTaskDetails) {

                WmsWareSku wmsWareSku = new WmsWareSku();
                wmsWareSku.setWarehouseId(wareOrderTask.getWareId());
                wmsWareSku.setStockLocked(wmsWareOrderTaskDetail.getSkuNum());
                wmsWareSku.setSkuId(wmsWareOrderTaskDetail.getSkuId());

                wareSkuMapper.incrStockLocked(wmsWareSku); //  加行级写锁 注意索引避免表锁

            }
            wareOrderTask.setTaskStatus(TaskStatus.DEDUCTED);
            updateStatusWareOrderTaskByOrderId(wareOrderTask.getOrderId(),TaskStatus.DEDUCTED);

        }

        try {
            sendSkuDeductMQ(wareOrderTask);
        } catch (JMSException e) {
            e.printStackTrace();
        }
        return;

    }



    public List<WmsWareOrderTask> getWareOrderTaskList(WmsWareOrderTask wareOrderTask){
             List<WmsWareOrderTask> wareOrderTasks=null;
            if(wareOrderTask==null){
                 wareOrderTasks = wareOrderTaskMapper.selectAll();
            }else{
                wareOrderTasks = wareOrderTaskMapper.select(wareOrderTask);
            }
            return wareOrderTasks;
    }

}
