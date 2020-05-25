package com.iflytek.webapi.sort;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *  用于对到达的乱序数据包进行排序
 * @author sjliu7
 * @Date 2020/1/16
 * @param <T>
 */
public class IatSorter<T> implements Sorter<T> {

    private No<T> no; // 获取数据包的序号接口
    private Map<Integer, T> cache; // 存放提前到达的结果
    private int next; // 期待到达的序号
    private int maxNo; // 当前到达的最大序号
    private BlockingQueue<T> queue; // 存放排序后的结果
    private int cacheSize;  //

    /**
     *
     * @param no  获取数据包序号的接口
     * @param startNo 数据包开始的序号
     */
    public IatSorter(No<T> no, int startNo) {
        this.no = no;
        cache = new HashMap<>();
        this.next = startNo;
        this.queue = new LinkedBlockingQueue<>();
        this.cacheSize = 5;
    }

    public IatSorter(No<T> no, int startNo, int cacheSize) {
        this(no, startNo);
        this.cacheSize = cacheSize;
    }

    private void putIntoQ(T o) throws InterruptedException {
        queue.put(o);
        this.next++;
    }

    /**
     * 将缓存的乱序数据包按照顺序全部插入排序队列
     * @throws InterruptedException
     */
    private void clearAll() throws InterruptedException {
        for (int i = this.next; i <= this.maxNo; i++) {
            T bm = this.cache.get(i);
            if (bm != null) {
                this.putIntoQ(bm);
                this.cache.remove(i);
            }
        }
        this.next = this.maxNo + 1;
    }

    @Override
    public synchronized void put(T o, boolean last) throws InterruptedException {
        int no = this.no.getNo(o);
        if (no > this.maxNo) {
            this.maxNo = no;
        }
        // 当到达的包序号和期待的相等，直接插入队列
        if (no == this.next) {
            this.putIntoQ(o);
            // 当到达的最大序号 大于等于 期待的序号时，说明存在乱序的数据包，将乱序的数据包取出来插入队列
            if (this.maxNo >= this.next) {
                for (int i = this.next; i <= this.maxNo; i++) {
                    T bm = this.cache.get(i);
                    if (bm != null) {
                        this.putIntoQ(bm);
                        this.cache.remove(i);
                    } else {
                        break;
                    }
                }
            }
        } else {
            // 丢弃过期的数据包
            if (no < this.next) {
                return;
            }
            this.cache.put(no, o);
            // 乱序的数据包达到一定的数量时，可能存在丢包了，此时丢弃乱序的包，取出数据包放入队列
            if (last || this.cache.size() > this.cacheSize) {
                this.clearAll();
            }
        }
    }

    @Override
    public T get() throws Exception {
        return this.queue.take();
    }

    public static void main(String[] args) throws Exception {
        Sorter<Response> sorter = new IatSorter<Response>(o -> {return o.getSn();}, 1,5);
        new Thread(() -> {
            try {
                int[] arr = {1, 2, 3, 5, 4, 7, 6, 8, 9, 11,  12, 10,13, 15, 14};
//                arr = new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10,11,  12, 13, 15, 14};
//                arr =new int[] {1,2,5,4,3,7,6};

                for (int i = 0; i < arr.length; i++) {
                    sorter.put(new Response().setSn(arr[i]), false);
                    Thread.sleep(500);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();


        while (true) {
            Response o = sorter.get();
            System.out.println(String.format("%d time:%d",o.getSn(),System.currentTimeMillis()));
            if (o.getSn() == 15) {
                return;
            }
        }
    }
}
