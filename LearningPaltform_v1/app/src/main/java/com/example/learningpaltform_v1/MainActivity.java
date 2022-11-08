package com.example.learningpaltform_v1;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.Image;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout screen = null;
    private boolean is_set_model = true;  //檢查是不是可以設定
    private Button[][] map = new Button[6][5];
    private ImageView player = null; //玩家物件  要隨時可以控制變換照片
    private ImageView finished_line = null;
    private int face_direction = 0;  //0: 上  1: 右  2:下  3: 左  順時鐘 跟 逆時鐘
    private int player_i = 5, player_j = 0;
    private boolean over_range = false;  //如果前面有step已經造成這個遊戲結束  直接結束

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //--------------------動作愈設
        getSupportActionBar().hide();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //--------------------

        /*
        * 設定 Lock 和 Set 兩種 model
        * */
        Button lock_btn = (Button)findViewById(R.id.lock_btn);
        lock_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                is_set_model = false;
            }
        });

        Button set_btn = (Button)findViewById(R.id.set_btn);
        set_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                is_set_model = true;
            }
        });


        //取得背景畫面
        screen = (RelativeLayout) findViewById(R.id.fullscreen);   //720x1458


        float x = 10, y = 0; //用來計算btn的位址
        //生成6 X 5的格子數量
        for(int i = 0; i < 6; i++){
            x = 10;
            for(int j = 0; j < 5; j++)
            {
                map[i][j] = new Button(this);
                map[i][j].setText("");
                map[i][j].setX(x);
                map[i][j].setY(y);


                //檢查是不是為XX
                map[i][j].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(is_set_model){
                            if (((Button)view).getText() == "X")
                            {
                                ((Button)view).setText("");
                            }else{
                                ((Button)view).setText("X");
                            }
                        }else{
                            Toast.makeText(getApplicationContext(),"現在是lock model",Toast.LENGTH_SHORT).show();
                        }

                    }
                });
                screen.addView(map[i][j], 720/6, 120);

                x += (720/6 + 20);
            }
            y += 720/6;
        }

        //建立玩家物件
        player = new ImageView(this);
        player.setImageDrawable(getResources().getDrawable(R.drawable.forward));
        player.setY(map[5][0].getY());
        player.setX(10);
        player.setTranslationZ(90);
        screen.addView(player, 720/6, 120);


        //建立旗幟物件
        finished_line = new ImageView(this);
        finished_line.setImageDrawable(getResources().getDrawable(R.drawable.finished_flag));
        finished_line.setY(map[5][0].getY());
        finished_line.setX(map[5][4].getX());
        finished_line.setTranslationZ(80);
        screen.addView(finished_line, 720/6, 120);
    }

    public void QR_code_scanner(View V)
    {
        ScanOptions options = new ScanOptions();
        options.setPrompt("掃描中.....");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        barLaucher.launch(options);
    }

    ActivityResultLauncher<ScanOptions> barLaucher = registerForActivityResult(new ScanContract(), result->
    {
        if(result.getContents() !=null)
        {
            alertbox("Result", result.getContents());

            //把掃描到的內容存起來到Edittext裡面
            EditText bar = (EditText) findViewById(R.id.scan_result_text);

            bar.setText(bar.getText() + " " + result.getContents().toString());
        }
    });

    private void alertbox(String title, String content){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(title);
        builder.setMessage(content);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                dialogInterface.dismiss();
            }
        }).show();
    }

    private String get_path(){
        /*
        * 取得目前所有的掃描值
        * */

        EditText path = (EditText) findViewById(R.id.scan_result_text);

        return path.getText().toString();
    }

    public void undo_click(View view){
        /*
        * 取得Path裡面每一筆，然後切出來後  在把最後一筆拿掉
        * */

        String reg = get_path();
        String str[] = reg.split(" ");

        String new_str = "";
        for(int i = 0;i < str.length - 1; i++){
            new_str += (str[i] + " ");
        }



        //重新擺放回去，因為後面會多放一個空白  先把它拿掉
        EditText path = (EditText) findViewById(R.id.scan_result_text);
        path.setText(new_str.substring(0, new_str.length()));
    }

    public void clear_click(View view){
        //重新設定
        reset();
    }

    private void reset(){
        /*
        * 重新設定所有的內容  不需要重新啟動
        * */

        //重新設定 方向
        face_direction = 0;

        //reset player position
        player.setX(10);
        player.setY(map[5][0].getY());
        player.setImageDrawable(getResources().getDrawable(R.drawable.forward));
        player_i = 5;
        player_j = 0;

        //重新來一局
        over_range = false;



        //reset scan result
        EditText path = (EditText) findViewById(R.id.scan_result_text);
        path.setText("");

    }

    private void trun_face(){
        switch (face_direction){
            case 0:
                player.setImageDrawable(getResources().getDrawable(R.drawable.forward));
                break;
            case 1:
                player.setImageDrawable(getResources().getDrawable(R.drawable.right));
                break;
            case 2:
                player.setImageDrawable(getResources().getDrawable(R.drawable.backward));
                break;
            case 3:
                player.setImageDrawable(getResources().getDrawable(R.drawable.left));
                break;
        }
    }

    private void player_turn_right(){
        /*
        * player 要右轉
        * */
        face_direction++;
        face_direction %= 4;
        trun_face();
    }

    private void player_turn_left(){
        /*
         * player 要左轉
         * */
        face_direction--;
        if (face_direction < 0){
            face_direction+=4;
        }
    //        face_direction %= 4;
        trun_face();
    }

    private void player_move(int step){
        switch (face_direction){
            case 0:
                //往上動
                for (int i = 0; i < step; i++){
                    player_i--;

                    if(player_i < 0){
                        //假如越界了  直接結束
                        over_range = true;
                        break;
                    }
                    player.setY(map[player_i][player_j].getY());

                    //每動一步就檢查又沒有撞到或成功
                    check_crush();
                    check_finished();
                }
                break;
            case 1:

                //往右動
                for (int i = 0; i < step; i++){
                    player_j++;

                    if(player_j > 4){
                        //假如越界了  直接結束
                        over_range = true;
                        break;
                    }

                    player.setX(map[player_i][player_j].getX());

                    //每動一步就檢查又沒有撞到或成功
                    check_crush();
                    check_finished();
                }
                break;
            case 2:

                //往下動
                for (int i = 0; i < step; i++){
                    player_i++;

                    if(player_i > 5){
                        //假如越界了  直接結束
                        over_range = true;
                        break;
                    }
                    player.setY(map[player_i][player_j].getY());

                    //每動一步就檢查又沒有撞到或成功
                    check_crush();
                    check_finished();
                }
                break;
            case 3:
                //往左動
                for (int i = 0;i < step; i++){
                    player_j--;

                    if(player_j < 0){
                        //假如越界了  直接結束
                        over_range = true;
                        break;
                    }

                    player.setX(map[player_i][player_j].getX());

                    //每動一步就檢查又沒有撞到或成功
                    check_crush();
                    check_finished();
                }
                break;
        }
    }



    public void run(View view){
        //取得 所有的掃瞄結果
        String scan_result = get_path();

        //個別獨立讀取出來
        String str[] = scan_result.split(" ");

        for (int i = 0; i < str.length; i++){
            switch (str[i]){
                case "R":
                    player_turn_right();
                    break;
                case "L":
                    player_turn_left();
                    break;
                case "1":
                    player_move(1);
                    break;
                case "2":
                    player_move(2);
                    break;
                case "3":
                    player_move(3);
                    break;
                case "4":
                    player_move(4);
                    break;
                case "5":
                    player_move(5);
                    break;
            }

            //假如跳出的迴圈有表示越界
            if(over_range){
                alertbox("Mission Fail！", "看起來越界了！");
                //直接跳出結束
                break;
            }
        }
    }

    private void check_crush(){
        for (int i = 0; i < 6; i++){
            for (int j = 0; j < 5; j++){
                if (map[i][j].getX() == player.getX() && map[i][j].getY() == player.getY() && map[i][j].getText() == "X")
                {
                    alertbox("Mission: Fail", "你撞到XX了");
                }
            }
        }
    }

    private void check_finished(){
        if(player.getX() == finished_line.getX() && player.getY() == finished_line.getY()){
            alertbox("Mission Complete", "成功惹！！");
        }
    }
}