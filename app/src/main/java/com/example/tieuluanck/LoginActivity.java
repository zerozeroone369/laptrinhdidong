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
            String role = db.checkLoginRole(u, p);
            if ("admin".equalsIgnoreCase(role)) {
                // Đăng nhập quyền Admin -> Chuyển sang màn hình Admin (MainActivity)
                Toast.makeText(this, "Đăng nhập quyền Admin thành công!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else if ("guest".equalsIgnoreCase(role)) {
                // Đăng nhập quyền Khách -> Chuyển sang màn hình Đọc sách (GuestActivity)
                Toast.makeText(this, "Đăng nhập quyền Khách thành công!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, GuestActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
            }
        });

        // Sự kiện Bấm nút Mở hộp thoại Đăng ký tài khoản mới (Mặc định là vai trò Khách)
        findViewById(R.id.btnOpenReg).setOnClickListener(v -> {
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 20, 50, 10);

            EditText regU = new EditText(this); regU.setHint("Tên đăng nhập mới");
            EditText regP = new EditText(this); regP.setHint("Mật khẩu (>= 6 ký tự)");
            layout.addView(regU); layout.addView(regP);

            // Hiện AlertDialog đăng ký trực tiếp
            new AlertDialog.Builder(this)
                .setTitle("Đăng ký tài khoản Khách")
                .setView(layout)
                .setPositiveButton("Đăng ký", (d, w) -> {
                    String u = regU.getText().toString().trim();
                    String p = regP.getText().toString().trim();
                    if (u.isEmpty() || p.length() < 6) {
                        Toast.makeText(this, "Tài khoản rỗng hoặc mật khẩu < 6 ký tự!", Toast.LENGTH_SHORT).show();
                    } else if (db.register(u, p)) {
                        Toast.makeText(this, "Đăng ký tài khoản Khách thành công!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Tài khoản đã tồn tại!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null).show();
        });
    }
}
