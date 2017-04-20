package com.example.liuyuqi.fontshow;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.view.WindowManager;
import android.graphics.drawable.ColorDrawable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class MainActivity extends Activity {

    private final static String TAG = MainActivity.class.getSimpleName();
    // 蓝牙相关
    private BluetoothAdapter adapter;
    //private boolean mScanning;
    private Handler mHandler = new Handler();
    //private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    // 列表显示蓝牙设备
    ArrayList<HashMap<String, String>> mylist = new ArrayList<HashMap<String, String>>();
    // 列表存储蓝牙设备
    Set<BluetoothDevice> bluetoothDevicesSet = new HashSet<BluetoothDevice>();
    List<BluetoothDevice> bluetoothDevicesList = new ArrayList<BluetoothDevice>();
    EditText inputText;
    ListView listView;
    PaintView paintView;
    SimpleAdapter simpleAdapter;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 3000;

    private byte pow_b(byte a,byte b){// a^b
        if(b==0){
            return 1;
        }
        byte result = (byte) (a * pow_b(a,(byte)(b-1)));
        return result;
    }
    // 数据格式化
    private byte[] formatData(int i,int flag){
        List data_return = new ArrayList();
        //for(int i=0;i<viewNum;i++){
        int[][] tmp = dataSaveds[i].getData();
        int tmp_color = dataSaveds[i].getColor();
        switch(tmp_color){//-65536 -256 -1 -16711936 -16711681 -16776961 -65281
            case -65536:
                data_return.add((byte)1);
                break;
            case -256:
                data_return.add((byte)2);
                break;
            case -1:
                data_return.add((byte)4);
                break;
            case -16711936:
                data_return.add((byte)8);
                break;
            case -16711681:
                data_return.add((byte)16);
                break;
            case -16776961:
                data_return.add((byte)32);
                break;
            case -65281:
                data_return.add((byte)64);
                break;
            default:
                System.err.println("***********color error!!!***********");
                data_return.add((byte)64);
                break;
        }
        for(int n=0;n<data_format;n++){
            byte temp1 = 0;
            byte temp2 = 0;
            for (int m=0;m<data_format;m++){
                if(m<8){
                    if(tmp[m][n]==0){
                        temp1 = (byte)(temp1 + pow_b((byte)2,(byte)m));
                    }else if(tmp[m][n]==1){
                        //pass
                    }else{
                        System.err.println("***********data error!!!***********");
                    }
                }else{
                    if(tmp[m][n]==0){
                        temp2 = (byte)(temp2 + pow_b((byte)2,(byte)(m-8)));
                    }else if(tmp[m][n]==1){
                        //pass
                    }else{
                        System.err.println("***********data error!!!***********");
                    }
                }
            }
            data_return.add(temp1);
            data_return.add(temp2);
        }
        //}
        int l = data_return.size();
        System.err.println("***********data_return size:"+l+"***********");
        byte[] data1 = new byte[20];
        byte[] data2 = new byte[l-20];
        for(int v=0;v<l;v++){
            if(v<20){
                data1[v] = (byte)data_return.get(v);
            }else{
                data2[v-20] = (byte)data_return.get(v);
            }

        }
        if (flag==1){
            return data1;
        }else{
            return data2;
        }
    }
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    System.err.println("onLeScan:"+device.getAddress()+"*******"+device.getName()+"*******");
                    bluetoothDevicesSet.add(device);
                }
            });
        }
    };


    // 建立GATT链接
    //private final static String TAG = MainActivity.class.getSimpleName();
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    int send_flag = 0;
    int data_flag = 1;
    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        //收到BLE终端写入数据回调
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            System.err.println("onCharacteristicWrite:"+status);
            if(data_flag==2){
                data_flag = 1;
                send_flag++;
            }else{
                data_flag ++;
            }
            //设置数据内容
            if(send_flag<4){
                byte[] data = formatData(send_flag,data_flag);
                characteristic.setValue(data);
                //往蓝牙模块写入数据
                System.out.println("writeCharacteristic:"+gatt.writeCharacteristic(characteristic));
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            //String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                //broadcastUpdate(intentAction);
                Log.i(TAG, "*********Connected to GATT server*********");
                Log.i(TAG, "*********Attempting to start service discovery:" +
                        bluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                //intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "*********Disconnected from GATT server*********");
                //broadcastUpdate(intentAction);
            }
        }
        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                Log.i(TAG, "***********BluetoothGatt.GATT_SUCCES:" + status);
                //broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                // 连接成功
                List<BluetoothGattService> gattServiceList = gatt.getServices();
                UUID u1 = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
                for (BluetoothGattService gattService : gattServiceList){
                    //if(gattService.getUuid().equals("0000ffe0-0000-1000-8000-00805f9b34fb")){
                        List<BluetoothGattCharacteristic> gattCharacteristics =gattService.getCharacteristics();
                        for (final BluetoothGattCharacteristic gattCharacteristic: gattCharacteristics) {
                            //UUID_KEY_DATA是可以跟蓝牙模块串口通信的Characteristic 00002a03-0000-1000-8000-00805f9b34fb 00002a02-0000-1000-8000-00805f9b34fb
                            if(gattCharacteristic.getUuid().equals(u1)){
                                //接受Characteristic被写的通知,收到蓝牙模块的数据后会触发mOnDataAvailable.onCharacteristicWrite()
                                System.out.println("Characteristic_uuid:"+gattCharacteristic.getUuid());
                                gatt.setCharacteristicNotification(gattCharacteristic, true);
                                //设置数据内容
                                send_flag = 0;
                                data_flag = 1;
                                byte[] data = formatData(send_flag,data_flag);
                                gattCharacteristic.setValue(data);
                                //往蓝牙模块写入数据
                                System.out.println("writeCharacteristic:"+gatt.writeCharacteristic(gattCharacteristic));
                            }
                        }
                    //}
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }
    };

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            System.err.println("rabbitLeScan");
        }
    };

    // 扫描ble设备函数
    private void scanLeDevice(boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //mScanning = false;
                    adapter.stopLeScan(mLeScanCallback);
                    System.err.println("*************停止BLE扫描*************");
                    for(BluetoothDevice btd : bluetoothDevicesSet){
                        HashMap<String, String> mapName = new HashMap<String, String>();
                        mapName.put("ItemTitle", btd.getName()+"/address:"+btd.getAddress());
                        System.err.println("*************ItemTitle*************"+btd.getName());
                        mylist.add(mapName);
                        bluetoothDevicesList.add(btd);
                        simpleAdapter = new SimpleAdapter(getApplicationContext(), mylist, R.layout.listview_layout, new String[] {"ItemTitle"}, new int[] {R.id.ItemTitle});
                        //加载SimpleAdapter到ListView中
                        listView.setAdapter(simpleAdapter);
                    }
                    //mBluetoothLeScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);

            //mScanning = true;
            //mBluetoothLeScanner= adapter.getBluetoothLeScanner();
            //mBluetoothLeScanner.startScan(mScanCallback);
            System.err.println("是否开始BLE扫描:" + adapter.startLeScan(mLeScanCallback));
        } else {
            //mScanning = false;
            //mBluetoothLeScanner.stopScan(mScanCallback);
            adapter.stopLeScan(mLeScanCallback);
        }
    }
    //private LeDeviceListAdapter mLeDeviceListAdapter;

    // 数据相关
    int[][] arr;
    int data_format = 16;
    int all_16_32 = 16;//24 3 72
    int all_2_4 = 2;
    int all_32_128 = 32;

    public int[][] drawString(String str) {
        byte[] data = null;
        int[] code = null;
        int byteCount;
        int lCount;
        arr = new int[all_16_32][all_16_32];
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) < 0x80) {
                continue;
            }
            code = getByteCode(str.substring(i, i + 1));
            data = read(code[0], code[1]);
            //System.out.print("data*************************"+data);
            byteCount = 0;
            for (int line = 0; line < all_16_32; line++) {
                lCount = 0;
                for (int k = 0; k < all_2_4; k++) {
                    for (int j = 0; j < 8; j++) {
                        if (((data[byteCount] >> (7 - j)) & 0x1) == 1) {
                            arr[line][lCount] = 1;
                            System.out.print("*");
                        } else {
                            System.out.print(" ");
                            arr[line][lCount] = 0;
                        }
                        lCount++;
                    }
                    byteCount++;
                }
                System.out.println();
            }
        }
        return arr;
    }

    protected byte[] read(int areaCode, int posCode) {
        byte[] data = null;
        try {
            int area = areaCode - 0xa0;
            int pos = posCode - 0xa0;
            InputStream in = getResources().openRawResource(R.raw.hzk16);
            long offset = all_32_128 * ((area - 1) * 94 + pos - 1);
            in.skip(offset);
            data = new byte[all_32_128];
            in.read(data, 0, all_32_128);
            in.close();
        } catch (Exception ex) {
            System.err.println("*********rabbitWrite***********SORRY,THE FILE CAN'T BE READ");
        }
        return data;

    }
    protected int[] getByteCode(String str) {
        int[] byteCode = new int[2];
        try {
            byte[] data = str.getBytes("GB2312");
            byteCode[0] = data[0] < 0 ? 256 + data[0] : data[0];
            byteCode[1] = data[1] < 0 ? 256 + data[1] : data[1];
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return byteCode;
    }

    // 其它控件
    Button sendBtn;
    int colorNum = 7;
    int viewNum = 4;
    Button[] colors = new Button[colorNum];
    int colorChoised;
    int viewChoised = 1;
    TextView toptext;
    LinearLayout color_banner;
    LinearLayout view_banner;
    Button[] views = new Button[viewNum];
    Button search_again;
    char[] color_data = new char[viewNum];
    int[] colorButtonId = { R.id.button_color1, R.id.button_color2, R.id.button_color3, R.id.button_color4, R.id.button_color5, R.id.button_color6, R.id.button_color7 }; ;
    int[] viewButtonId = {R.id.button_1,R.id.button_2,R.id.button_3,R.id.button_4};
    DataSaved[] dataSaveds = new DataSaved[viewNum];
    // 蓝牙发送函数data_flag
    protected void bleSend(){
        // 蓝牙发送start
        // 得到BluetoothAdapter对象
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        adapter = bluetoothManager.getAdapter();
        //adapter = BluetoothAdapter.getDefaultAdapter();
        // 判断BluetoothAdapter对象是否为空，如果为空，则表明本机没有合适的蓝牙设备
        if(adapter != null) {
            System.err.println("本机拥有蓝牙设备");
            if(!adapter.isEnabled()){
                //如果蓝牙设备不可用的话,创建一个intent对象,该对象用于启动一个Activity,提示用户启动蓝牙适配器
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(intent);
            }
            // 蓝牙存在并且打开 开始扫描
            System.err.println("**********start scanLeDevice**********");
            mylist.clear();
            bluetoothDevicesSet.clear();
            bluetoothDevicesList.clear();
            // 清空列表
            simpleAdapter = new SimpleAdapter(getApplicationContext(), mylist, R.layout.listview_layout, new String[] {"ItemTitle"}, new int[] {R.id.ItemTitle});
            listView.setAdapter(simpleAdapter);
            //adapter.startDiscovery();// 下面的步骤在回调函数
            scanLeDevice(true);

        }else{
            // BluetoothAdapter = null
            Toast.makeText(getApplicationContext(),"爪机木有蓝牙设备", Toast.LENGTH_SHORT).show();
            System.err.println("本机没有蓝牙设备");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 保证背景不被拉伸
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        //drawString("胡萝卜");
        // 获取控件
        view_banner = (LinearLayout)findViewById(R.id.sendBanner);
        color_banner = (LinearLayout)findViewById(R.id.colorbanner);
        toptext = (TextView)findViewById(R.id.textView);
        inputText = (EditText)findViewById(R.id.editText);
        sendBtn = (Button)findViewById(R.id.button_send);
        paintView = (PaintView)findViewById(R.id.paintView);
        listView = (ListView)findViewById(R.id.listView);
        search_again = (Button)findViewById(R.id.search_again);

        search_again.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {// 重新搜索
                // 发送
                bleSend();
            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {// 发送按钮
                // 处理数据
                String input = inputText.getText().toString();
                int[][] paint = paintView.paintData;
//                int[][] temp = new int[data_format][data_format];
//                if( (!(input.equals(""))) && paint!=temp){
//                    Toast.makeText(getApplicationContext(),"请不要同时输入文字和图案,如果您非要全都输入,那我们将只保存文字", Toast.LENGTH_LONG).show();
//                }
                //保存数据
                if(!(input.equals(""))){
                    dataSaveds[(viewChoised-1)].setData(drawString(input));
                    dataSaveds[(viewChoised-1)].word = input;
                    Toast.makeText(getApplicationContext(),viewChoised+"号文字保存", Toast.LENGTH_SHORT).show();
                }else{
                    dataSaveds[(viewChoised-1)].setData(paint);
                    Toast.makeText(getApplicationContext(),viewChoised+"号图案保存", Toast.LENGTH_SHORT).show();
                }
                // 显示和隐藏
                inputText.setVisibility(View.INVISIBLE);
                paintView.setVisibility(View.INVISIBLE);
                color_banner.setVisibility(View.INVISIBLE);
                view_banner.setVisibility(View.INVISIBLE);
                toptext.setVisibility(View.INVISIBLE);
                listView.setVisibility(View.VISIBLE);
                search_again.setVisibility(View.VISIBLE);
                // 检测ble是否支持
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                    Toast.makeText(getApplicationContext(), "BLE not supported", Toast.LENGTH_SHORT).show();
                    finish();
                }else{
                    System.err.println("BLE supported");
                }
                // 发送
                bleSend();
            }
        });
        for(int i=0;i<colorNum;i++){// 颜色按钮
            colors[i]=((Button)findViewById(colorButtonId[i]));
            colors[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    colorChoised = ((ColorDrawable)v.getBackground()).getColor();
                    System.err.println(colorChoised);// -65536 -256 -1 -16711936 -16711681 -16776961 -65281
                    inputText.setTextColor(colorChoised);
                    paintView.color_paint = colorChoised;
                    dataSaveds[viewChoised-1].setColor(colorChoised);
                    paintView.invalidate();
                }
            });
        }
        for(int i=0;i<viewNum;i++){// 1-4画板按钮
            views[i] = (Button)findViewById(viewButtonId[i]);
            if(i==0){
                views[i].setBackgroundColor(getResources().getColor(R.color.lightGray));
            }
            //
            dataSaveds[i] = new DataSaved();
            // click event
            views[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // background change
                    for(int j=0;j<viewNum;j++){
                        views[j].setBackgroundColor(getResources().getColor(R.color.white));
                        ((Button)v).setBackgroundColor(getResources().getColor(R.color.lightGray));
                    }
                    String input = inputText.getText().toString();
                    int[][] paint = paintView.paintData;
//                    int[][] temp = new int[data_format][data_format];
//                    if( (!(input.equals(""))) && paint!=temp){
//                        Toast.makeText(getApplicationContext(),"请不要同时输入文字和图案,如果您非要全都输入,那我们将只保存文字", Toast.LENGTH_LONG).show();
//                    }
                    int choised_new = Integer.parseInt((((Button)v).getText().toString()));
                    //System.err.println("choised_new"+choised_new);
                    //保存数据
                    if(!(input.equals(""))){
                        dataSaveds[(viewChoised-1)].setData(drawString(input));
                        dataSaveds[(viewChoised-1)].word = input;
                        Toast.makeText(getApplicationContext(),viewChoised+"号文字保存", Toast.LENGTH_SHORT).show();
                    }else{
                        dataSaveds[(viewChoised-1)].setData(paint);
                        Toast.makeText(getApplicationContext(),viewChoised+"号图案保存", Toast.LENGTH_SHORT).show();
                    }
                    //显示数据
                    System.err.println("dataSaveds[choised_new].word"+dataSaveds[choised_new-1].word);
                    paintView.paintData = dataSaveds[choised_new-1].getData();
                    paintView.color_paint = dataSaveds[choised_new-1].getColor();
                    paintView.invalidate();
                    inputText.setText(dataSaveds[choised_new-1].word);
                    viewChoised = choised_new;
                }
            });
        }

        // 添加点击事件
        // 点击列表中的设备名称
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                //System.err.println(arg2);
                // 建立链接
                BluetoothDevice btd = bluetoothDevicesList.get(arg2);
                //mDeviceAddress = btd.getAddress();
                System.err.println("mDeviceAddress:"+btd.getAddress()+"/name:"+btd.getName());

                bluetoothGatt = btd.connectGatt(getApplicationContext(), false, gattCallback);
                System.err.println(bluetoothGatt.connect()+":connect()");
                System.err.println(bluetoothGatt.discoverServices()+":discoverServices()");
            }

        });
//        // view监听-> PaintView
//        // 按钮监听
//        paintBtn.setOnClickListener(new View.OnClickListener(){ // paintMode
//            @Override
//            public void onClick(View v) {
//                if (repaint){
//                    // 清空view
//                    paintView.clearCanvas();
//                }else{
//                    listView.setVisibility(View.INVISIBLE);
//                    paintView.setVisibility(View.VISIBLE);
//                    confirmBtn.setText("画好了:)");
//                    paintMode=true;
//                    paintBtn.setText("我要重画");
//                    repaint=true;
//                }
//            }
//        });
//        confirmBtn.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View v) {
//                if (paintMode){// 绘画模式
//                    // 得到数据
//                    int[][] paintData = paintView.paintData;
//                    // 改变界面
//                    confirmBtn.setText(R.string.main_button);//输入好啦:)搜索设备!
//                    paintMode=false;
//                    paintView.setVisibility(View.INVISIBLE);
//                    paintBtn.setText("切换到绘图模式");
//                    repaint=false;
//                    // 在光标处添加字符
//                    int index = getInputText.getSelectionStart();//获取光标所在位置
//                    String text="☺";
//                    Editable edit = getInputText.getEditableText();//获取EditText的文字
//                    if (index < 0 || index >= edit.length() ){
//                        edit.append(text);
//                    }else{
//                        edit.insert(index,text);//光标所在位置插入文字
//                    }
//                }else{//发送模式
//                    paintBtn.setText("切换到绘图模式");
//                    repaint=false;
//                    paintView.setVisibility(View.INVISIBLE);
//                    listView.setVisibility(View.VISIBLE);
//                    String text = getInputText.getText().toString();
//                    // 判断字符串是否为中文 判断字数是否超标
//                    int textLength = text.length();
//                    if(textLength>5){
//                        Toast.makeText(getApplicationContext(),"太多字了，我懒~", Toast.LENGTH_SHORT).show();
//                    }else if(textLength==0){
//                        Toast.makeText(getApplicationContext(),"请至少输入一个字吖", Toast.LENGTH_SHORT).show();
//                    }else{
//                        int flag = 1;
//                        for (int i=0;i<textLength;i++){
//                            Pattern p=Pattern.compile("[\u4e00-\u9fa5]");
//                            Matcher m=p.matcher(text.substring(i,i+1));
//                            if(!m.matches()){
//                                Toast.makeText(getApplicationContext(),"请输入中文>.<", Toast.LENGTH_SHORT).show();
//                                flag = 0;
//                            }
//                        }
//                        if(flag==1){
//                            // Use this check to determine whether BLE is supported on the device.  Then you can selectively disable BLE-related features.
//                            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
//                                Toast.makeText(getApplicationContext(), "*********rabbitWrite***********BLE not supported", Toast.LENGTH_SHORT).show();
//                                finish();
//                            }else{
//                                System.err.println("*********rabbitWrite***********BLE supported");
//                            }
//                            // 提示消息
//                            Toast.makeText(getApplicationContext(),"开始发送^O^", Toast.LENGTH_SHORT).show();
//                            Toast.makeText(getApplicationContext(),"发送完成后字体将会变成桃子的粉色，请耐心等待~", Toast.LENGTH_SHORT).show();
//                            getInputText.setTextColor(Color.BLACK);
//                            boolean [][] sendData = drawString(text);
//                            // 蓝牙发送start
//                            // 得到BluetoothAdapter对象
//
//                            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
//                            adapter = bluetoothManager.getAdapter();
//                            //adapter = BluetoothAdapter.getDefaultAdapter();
//                            // 判断BluetoothAdapter对象是否为空，如果为空，则表明本机没有合适的蓝牙设备
//                            if(adapter != null) {
//                                System.err.println("*********rabbitWrite***********本机拥有蓝牙设备");
//                                if(!adapter.isEnabled()){
//                                    //如果蓝牙设备不可用的话,创建一个intent对象,该对象用于启动一个Activity,提示用户启动蓝牙适配器
//                                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                                    startActivity(intent);
//                                }
//                                // 蓝牙存在并且打开 开始扫描
////                            // 注册用以接收到已搜索到的蓝牙设备的receiver
////                            IntentFilter mFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
////                            registerReceiver(mReceiver, mFilter);
////                            // 注册搜索完时的receiver
////                            mFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
////                            registerReceiver(mReceiver, mFilter);
////                            if (adapter.isDiscovering()) {
////                                adapter.cancelDiscovery();
////                            }
//                                System.err.println("*********rabbitWrite*******start**********");
//                                mylist.clear();
//                                bluetoothDevicesSet.clear();
//                                bluetoothDevicesList.clear();
//                                // 清空列表
//                                simpleAdapter = new SimpleAdapter(getApplicationContext(), mylist, R.layout.listview_layout, new String[] {"ItemTitle"}, new int[] {R.id.ItemTitle});
//                                listView.setAdapter(simpleAdapter);
//                                //adapter.startDiscovery();// 下面的步骤在回调函数
//                                scanLeDevice(true);
//
//                            }else{
//                                // BluetoothAdapter = null
//                                Toast.makeText(getApplicationContext(),"爪机木有蓝牙设备", Toast.LENGTH_SHORT).show();
//                                System.err.println("*********rabbitWrite***********本机没有蓝牙设备");
//                            }
//                        }
//                    }
//                }
//            }
//        });
    }
}
