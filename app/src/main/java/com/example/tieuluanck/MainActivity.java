package com.example.tieuluanck;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.DragEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Activity chính Quản Lý Sách dành cho Admin (Hỗ trợ Kéo - Thả / Nạp File sách tự động)
 */
public class MainActivity extends AppCompatActivity {

    // Khai báo các thành phần giao diện
    EditText edtma, edtten, edtgia, edtAnhUrl, edtNoiDung, edtSearch;
    ImageView imgPreview;
    ListView lv;
    ArrayList<String[]> mylist = new ArrayList<>();
    SachAdapter myadapter;
    DatabaseHelper db;
    String orderBy = "";

    // Bộ chọn và nạp tự động nội dung sách từ File (.txt, .doc, tài liệu)
    ActivityResultLauncher<String> pickFileLauncher = registerForActivityResult(
        new ActivityResultContracts.GetContent(),
        uri -> {
            if (uri != null) {
                docFileVaNapNoiDung(uri);
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Khởi tạo và kết nối dữ liệu và liên kết giao diện
        db = new DatabaseHelper(this);
        edtma = findViewById(R.id.edtmasach);
        edtten = findViewById(R.id.edttensach);
        edtgia = findViewById(R.id.edtgia);
        edtAnhUrl = findViewById(R.id.edtAnhUrl);
        edtNoiDung = findViewById(R.id.edtNoiDung);
        edtSearch = findViewById(R.id.edtSearch);
        imgPreview = findViewById(R.id.imgPreview);
        lv = findViewById(R.id.lv);

        // Khởi tạo ListView và Adapter
        myadapter = new SachAdapter(this, mylist);
        lv.setAdapter(myadapter);
        loadData();

        // Nút Thống kê Dashboard Kho Sách cho Admin
        findViewById(R.id.btnDashboard).setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle("📊 BÁO CÁO KHO SÁCH ADMIN")
                .setMessage(db.getDashboardStatsText())
                .setPositiveButton("Đóng", null)
                .show()
        );

        // [TÍNH NĂNG MỚI NỔI BẬT]: Khung Kéo - Thả / Bấm vào để nạp file sách tự động
        View layoutDropzone = findViewById(R.id.layoutDropzone);
        layoutDropzone.setOnClickListener(v -> pickFileLauncher.launch("*/*"));

        // Xử lý sự kiện KÉO & THẢ (Drag & Drop) trực tiếp lên Khung Dropzone
        layoutDropzone.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    layoutDropzone.setAlpha(0.6f);
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                case DragEvent.ACTION_DRAG_ENDED:
                    layoutDropzone.setAlpha(1.0f);
                    return true;
                case DragEvent.ACTION_DROP:
                    ClipData clipData = event.getClipData();
                    if (clipData != null && clipData.getItemCount() > 0) {
                        Uri uri = clipData.getItemAt(0).getUri();
                        if (uri != null) {
                            docFileVaNapNoiDung(uri);
                        }
                    }
                    return true;
            }
            return false;
        });

        // Nút Bấm "Tải ảnh" từ link URL để xem trước
        findViewById(R.id.btnTaiAnh).setOnClickListener(v -> {
            String url = edtAnhUrl.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "Vui lòng dán link URL ảnh!", Toast.LENGTH_SHORT).show();
            } else {
                SachAdapter.loadAnhTuUrl(url, imgPreview);
            }
        });

        // Sự kiện Click chọn 1 dòng sách trong ListView -> Điền thông tin lên Form (kèm link URL, ảnh & nội dung sách)
        lv.setOnItemClickListener((p, v, pos, id) -> {
            String[] s = mylist.get(pos); // {masach, tensach, gia, anhUrl, noidung, diem_sao}
            edtma.setText(s[0]);
            edtten.setText(s[1]);
            edtgia.setText(s[2]);
            edtAnhUrl.setText(s[3]);
            edtNoiDung.setText(s[4]);
            SachAdapter.loadAnhTuUrl(s[3], imgPreview);
        });

        // Sự kiện Bấm nút THÊM sách
        findViewById(R.id.btninsert).setOnClickListener(v -> {
            String[] in = getInput();
            if (in == null) return;
            String url = edtAnhUrl.getText().toString().trim();
            String noidung = edtNoiDung.getText().toString().trim();
            if (db.insertSach(in[0], in[1], Integer.parseInt(in[2]), url, noidung)) {
                Toast.makeText(this, "Thêm thành công!", Toast.LENGTH_SHORT).show();
                resetForm();
            } else {
                Toast.makeText(this, "Mã sách đã tồn tại!", Toast.LENGTH_SHORT).show();
            }
        });

        // Sự kiện Bấm nút CẬP NHẬT sách (Cập nhật cả nội dung sách)
        findViewById(R.id.btnupdate).setOnClickListener(v -> {
            String[] in = getInput();
            if (in == null) return;
            String url = edtAnhUrl.getText().toString().trim();
            String noidung = edtNoiDung.getText().toString().trim();
            if (db.updateSach(in[0], in[1], Integer.parseInt(in[2]), url, noidung) > 0) {
                Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                resetForm();
            } else {
                Toast.makeText(this, "Không tìm thấy mã sách!", Toast.LENGTH_SHORT).show();
            }
        });

        // Sự kiện Bấm nút XÓA sách (có Dialog hỏi xác nhận)
        findViewById(R.id.btndelete).setOnClickListener(v -> {
            String ma = edtma.getText().toString().trim();
            if (ma.isEmpty()) { Toast.makeText(this, "Nhập mã sách cần xóa", Toast.LENGTH_SHORT).show(); return; }
            new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Xóa sách mã [" + ma + "]?")
                .setPositiveButton("Xóa", (d, w) -> {
                    if (db.deleteSach(ma) > 0) {
                        Toast.makeText(this, "Đã xóa!", Toast.LENGTH_SHORT).show();
                        resetForm();
                    } else Toast.makeText(this, "Không tìm thấy mã sách!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null).show();
        });

        // Sự kiện Bấm nút LÀM MỚI Form
        findViewById(R.id.btnquery).setOnClickListener(v -> {
            edtSearch.setText(""); orderBy = ""; resetForm();
        });

        // Lắng nghe sự kiện TÌM KIẾM realtime khi người dùng gõ từ khóa
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { loadData(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Sự kiện Bấm nút SẮP XẾP danh sách (hiện PopupMenu chọn tiêu chí)
        findViewById(R.id.btnSort).setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(this, v);
            menu.getMenu().add(0, 1, 0, "Tên A -> Z");
            menu.getMenu().add(0, 2, 1, "Giá tăng dần");
            menu.getMenu().add(0, 3, 2, "Giá giảm dần");
            menu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) orderBy = "tensach ASC";
                else if (item.getItemId() == 2) orderBy = "gia ASC";
                else orderBy = "gia DESC";
                loadData();
                return true;
            });
            menu.show();
        });

        // Sự kiện Bấm nút ĐĂNG XUẤT (quay về LoginActivity)
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // Xử lý System Bars EdgeToEdge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    // Hàm trích xuất nội dung từ File và nạp vào Form
    private void docFileVaNapNoiDung(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            if (sb.length() > 0) {
                edtNoiDung.setText(sb.toString().trim());
                Toast.makeText(this, "🎉 Đã tự động trích xuất & nạp nội dung file thành công!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "File rỗng hoặc không thể đọc văn bản!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khi đọc file!", Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm lấy và kiểm tra hợp lệ dữ liệu nhập từ Form
    private String[] getInput() {
        String ma = edtma.getText().toString().trim();
        String ten = edtten.getText().toString().trim();
        String gia = edtgia.getText().toString().trim();
        if (ma.isEmpty() || ten.isEmpty() || gia.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
            return null;
        }
        try { Integer.parseInt(gia); } catch (Exception e) {
            Toast.makeText(this, "Giá tiền không hợp lệ!", Toast.LENGTH_SHORT).show();
            return null;
        }
        return new String[]{ma, ten, gia};
    }

    // Hàm xóa trắng Form nhập liệu và tải lại danh sách
    private void resetForm() {
        edtma.setText(""); edtten.setText(""); edtgia.setText(""); edtAnhUrl.setText(""); edtNoiDung.setText("");
        imgPreview.setImageResource(android.R.drawable.ic_menu_gallery);
        loadData();
    }

    // Hàm tải danh sách sách từ CSDL lên Adapter
    private void loadData() {
        mylist.clear();
        mylist.addAll(db.getDanhSachSach(edtSearch.getText().toString().trim(), orderBy));
        myadapter.notifyDataSetChanged();
    }
}
