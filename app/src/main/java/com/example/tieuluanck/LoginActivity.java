package com.example.tieuluanck;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity quản lý Đăng nhập & Đăng ký tài khoản người dùng
 */
public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Khởi tạo Helper CSDL và ánh xá View
        DatabaseHelper db = new DatabaseHelper(this);
        EditText edtUser = findViewById(R.id.edtUser);
        EditText edtPass = findViewById(R.id.edtPass);

        // Sự kiện Bấm nút Đăng nhập
        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            String u = edtUser.getText().toString().trim();
            String p = edtPass.getText().toString().trim();
            // Kiểm tra thông tin trong CSDL
            if (db.checkLogin(u, p)) {
                // Chuyển sang màn hình chính MainActivity
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
            }
        });

        // Sự kiện Bấm nút Mở hộp thoại Đăng ký tài khoản mới
        findViewById(R.id.btnOpenReg).setOnClickListener(v -> {
            // Tạo layout tĩnh chứa 2 ô nhập tên và mật khẩu đăng ký
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 20, 50, 10);

            EditText regU = new EditText(this); regU.setHint("Tên đăng nhập mới");
            EditText regP = new EditText(this); regP.setHint("Mật khẩu (>= 6 ký tự)");
            layout.addView(regU); layout.addView(regP);

            // Hiện AlertDialog đăng ký trực tiếp
            new AlertDialog.Builder(this)
                .setTitle("Đăng ký tài khoản")
                .setView(layout)
                .setPositiveButton("Đăng ký", (d, w) -> {
                    String u = regU.getText().toString().trim();
                    String p = regP.getText().toString().trim();
                    if (u.isEmpty() || p.length() < 6) {
                        Toast.makeText(this, "Tài khoản rỗng hoặc mật khẩu < 6 ký tự!", Toast.LENGTH_SHORT).show();
                    } else if (db.register(u, p)) {
                        Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Tài khoản đã tồn tại!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null).show();
        });
    }
}
