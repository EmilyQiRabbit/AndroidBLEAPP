package com.example.liuyuqi.fontshow;

/**
 * Created by liuyuqi on 16/5/9.
 */
public class DataSaved {

    int[][] data ;
    String word;
    int num = 16;
    int color;

    public DataSaved(){
        data = new int[num][num];
        color = -16776961;//默认颜色为蓝色
        word = "";
    }

    public int getNum() {
        return this.num;
    }
    public void setNum(int n){
        this.num = n;
    }
    public int[][] getData(){
        return this.data;
    }
    public void setData(int[][] d){
        this.data = d;
    }
    public int getColor(){
        return this.color;
    }
    public void setColor(int color) {
        this.color = color;
    }
}
