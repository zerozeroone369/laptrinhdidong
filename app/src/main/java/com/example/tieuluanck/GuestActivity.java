package com.example.tieuluanck;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.ArrayList;

/**
 * Activity dành riêng cho tài khoản Khách (Guest)
 * Chức năng: Xem danh sách sách, Tìm kiếm không dấu, Sắp xếp và ĐỌC NỘI DUNG SÁCH chi tiết (Không có quyền CRUD)
 */
public class GuestActivity extends AppCompatActivity {

    // Khai báo các thành phần giao diện
    EditText edtSearch;
    ListView lv;
    ArrayList<String[]> mylist = new ArrayList<>();
    SachAdapter myadapter;
    DatabaseHelper db;
    String orderBy = ""; // Tiêu chí sắp xếp danh sách

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_guest);

        // Khởi tạo DatabaseHelper và ánh xạ các View giao diện
        db = new DatabaseHelper(this);
        edtSearch = findViewById(R.id.edtSearch);
        lv = findViewById(R.id.lv);

        // Khởi tạo Custom Adapter và gắn vào ListView
        myadapter = new SachAdapter(this, mylist);
        lv.setAdapter(myadapter);
        loadData(); // Tải dữ liệu ban đầu lên danh sách

        // [SỰ KIỆN 1]: Khách chạm/bấm vào một dòng sách trong ListView -> Mở hộp thoại (Dialog) ĐỌC NỘI DUNG SÁCH
        lv.setOnItemClickListener((p, v, pos, id) -> {
            String[] s = mylist.get(pos); // Lấy mảng thông tin sách: {masach, tensach, gia, anhUrl, noidung} (Độ dài 5)
            showDialogDocSach(s); // Hiển thị nội dung chi tiết
        });

        // [SỰ KIỆN 2]: Lắng nghe sự kiện TÌM KIẾM realtime khi người dùng gõ từ khóa
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                loadData(); // Tải lại danh sách theo từ khóa tìm kiếm
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // [SỰ KIỆN 3]: Bấm nút SẮP XẾP -> Hiển thị PopupMenu cho phép Khách chọn tiêu chí sắp xếp (Tên A-Z, Giá tăng/giảm)
        findViewById(R.id.btnSort).setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(this, v);
            menu.getMenu().add(0, 1, 0, "Tên A -> Z");
            menu.getMenu().add(0, 2, 1, "Giá tăng dần");
            menu.getMenu().add(0, 3, 2, "Giá giảm dần");
            menu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) orderBy = "tensach ASC";
                else if (item.getItemId() == 2) orderBy = "gia ASC";
                else orderBy = "gia DESC";
                loadData(); // Tải lại danh sách đã sắp xếp
                return true;
            });
            menu.show();
        });

        // [SỰ KIỆN 4]: Bấm nút ĐĂNG XUẤT (Thoát) -> Quay về màn hình LoginActivity
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // Xử lý hiển thị tràn viền (EdgeToEdge) cho thanh System Bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.guest_main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    // [HÀM XỬ LÝ]: Hiển thị AlertDialog chứa giao diện xem thông tin sách và toàn bộ nội dung chi tiết để Khách đọc sách
    private void showDialogDocSach(String[] s) {
        ScrollView layout = new ScrollView(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.item_sach, null);

        // Gán thông tin đầu sách lên Header của Dialog (Tên sách, Mã, Giá, Ảnh bìa)
        // Cấu trúc s: {masach, tensach, gia, anhUrl, noidung}
        ((TextView) dialogView.findViewById(R.id.tvTen)).setText(s[1]);
        ((TextView) dialogView.findViewById(R.id.tvThongTin)).setText("Mã: " + s[0]);
        ((TextView) dialogView.findViewById(R.id.tvGia)).setText(String.format("%,d VNĐ", Integer.parseInt(s[2])));
        ImageView img = dialogView.findViewById(R.id.imgItemSach);
        SachAdapter.loadAnhTuUrl(s[3], img);

        // Tạo TextView hiển thị nội dung chi tiết sách
        TextView tvNoiDung = new TextView(this);
        tvNoiDung.setPadding(40, 20, 40, 30);
        tvNoiDung.setTextSize(15);
        tvNoiDung.setTextColor(0xFF333333);
        String nd = s[4] != null && !s[4].trim().isEmpty() ? s[4] : "(Sách này chưa có nội dung chi tiết)";
        tvNoiDung.setText("📖 NỘI DUNG SÁCH:\n\n" + nd);

        // Đưa Header sách và Nội dung vào một LinearLayout cuộn (ScrollView)
        android.widget.LinearLayout contentLayout = new android.widget.LinearLayout(this);
        contentLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        contentLayout.addView(dialogView);
        contentLayout.addView(tvNoiDung);
        layout.addView(contentLayout);

        // Hiển thị hộp thoại Đọc sách
        new AlertDialog.Builder(this)
                .setTitle("📖 Đọc nội dung sách")
                .setView(layout)
                .setPositiveButton("Đóng", null)
                .show();
    }

    // [HÀM HỖ TRỢ]: Truy vấn CSDL để lấy danh sách sách theo từ khóa tìm kiếm và tiêu chí sắp xếp, sau đó cập nhật Adapter
    private void loadData() {
        mylist.clear();
        mylist.addAll(db.getDanhSachSach(edtSearch.getText().toString().trim(), orderBy));
        myadapter.notifyDataSetChanged();
    }
}
