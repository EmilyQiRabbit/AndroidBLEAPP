package com.example.liuyuqi.fontshow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

/**
 * Created by liuyuqi on 16/4/7.
 */
public class PaintView extends View{

    private Bitmap bitmap;
    private Canvas canvas;
    private Path path;
    private Paint paint;


    int num = 16;
    int[][] paintData = new int[num][num];
    int color_paint = Color.BLUE;
    float eachW;
    float eachH;
    public void clearCanvas(){
        for (int i=0;i<num;i++){
            for (int j=0;j<num;j++){
                paintData[i][j]=0;
            }
        }
        invalidate();
    }
    public PaintView(Context context,AttributeSet attrs) {
        super(context,attrs);

        paint=new Paint();//设置一个笔
        paint.setAntiAlias(true);//设置没有锯齿
        paint.setColor(color_paint);//设置笔的颜色
        paint.setStyle(Paint.Style.FILL);//设置填满

        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(),"clear", Toast.LENGTH_SHORT).show();
                clearCanvas();
            }
        });
        this.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    // 当触摸的时候
                    case (MotionEvent.ACTION_MOVE):
                        float x=event.getX();
                        float y=event.getY();
                        float w=v.getWidth();
                        float h=v.getHeight();
                        eachW = w/num;
                        eachH = h/num;
                        int numw = (int)Math.floor(x/eachW);
                        int numh = (int)Math.floor(y/eachH);
                        if(numw>num||numw==num){
                            numw = num-1;
                        }
                        if(numh>num||numh==num){
                            numh = num-1;
                        }
                        if(numw<0){
                           numw=0;
                        }
                        if(numh<0){
                            numh=0;
                        }
                        paintData[numh][numw] = 1;
                        //System.out.println("*********ACTION**********"+numw+"///"+numh);
                        invalidate();
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
    }
    @Override
    // 重写该方法，进行绘图
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 整张画布背景
        canvas.drawColor(Color.rgb(230,230,250));
        paint.setColor(color_paint);//设置笔的颜色
        for (int i=0;i<num;i++){
            for (int j=0;j<num;j++){
                if (paintData[i][j]==1){
                    //System.out.println(i+"~~~"+j);
                    Rect r1=new Rect();
                    r1.left=j*(int)eachW;
                    r1.top=i*(int)eachH;
                    r1.right=(j+1)*(int)eachW;
                    r1.bottom=(i+1)*(int)eachH;
                    canvas.drawRect(r1,paint);
                }
            }
        }
    }
}
