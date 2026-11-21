package com.example.tieuluanck;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.ArrayList;

/**
 * Activity dành cho Độc giả / Khách (Đọc sách Đa phương tiện đan xen Chữ & Ảnh, Tủ sách yêu thích, Đánh giá ⭐ & Chia sẻ)
 */
public class GuestActivity extends AppCompatActivity {

    EditText edtSearch;
    ListView lv;
    Button btnFavFilter;
    ArrayList<String[]> mylist = new ArrayList<>();
    SachAdapter myadapter;
    DatabaseHelper db;
    String orderBy = "";
    boolean showingOnlyFavorites = false;
    String currentUsername = "khach_demo"; // Tên tài khoản độc giả hiện tại
    float currentTextSize = 15f; // Cỡ chữ đọc sách
    int currentTextColor = Color.BLACK; // Màu chữ đọc sách

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_guest);

        db = new DatabaseHelper(this);
        edtSearch = findViewById(R.id.edtSearch);
        btnFavFilter = findViewById(R.id.btnFavFilter);
        lv = findViewById(R.id.lv);

        myadapter = new SachAdapter(this, mylist);
        lv.setAdapter(myadapter);
        loadData();

        // Lọc Tủ sách Yêu thích cá nhân
        btnFavFilter.setOnClickListener(v -> {
            showingOnlyFavorites = !showingOnlyFavorites;
            if (showingOnlyFavorites) {
                btnFavFilter.setText("📚 Tất cả");
                Toast.makeText(this, "Đang xem Tủ sách Yêu thích của bạn", Toast.LENGTH_SHORT).show();
            } else {
                btnFavFilter.setText("❤️ Tủ sách");
                Toast.makeText(this, "Đang xem Tất cả kho sách", Toast.LENGTH_SHORT).show();
            }
            loadData();
        });

        // Click dòng sách -> Mở Giao diện Đọc sách Đa phương tiện (Đan xen Chữ & Ảnh)
        lv.setOnItemClickListener((p, v, pos, id) -> {
            String[] s = mylist.get(pos); // {masach, tensach, gia, anhUrl, noidung, diem_sao}
            showDialogDocSachNangCao(s);
        });

        // Tìm kiếm realtime
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { loadData(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Sắp xếp danh sách
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

        // Đăng xuất
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.guest_main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    // Hiển thị Dialog Đọc sách Nâng cao Đa phương tiện (Chữ đan xen Ảnh, Cỡ chữ, Đổi nền, Yêu thích, Đánh giá ⭐ & Chia sẻ)
    private void showDialogDocSachNangCao(String[] s) {
        String masach = s[0];
        String tensach = s[1];
        String gia = s[2];
        String anhUrl = s[3];
        String noidung = s[4] != null && !s[4].trim().isEmpty() ? s[4] : "(Chưa có nội dung chi tiết)";

        ScrollView layout = new ScrollView(this);
        LinearLayout mainContent = new LinearLayout(this);
        mainContent.setOrientation(LinearLayout.VERTICAL);
        mainContent.setPadding(30, 20, 30, 30);

        // Header Sách
        View headerView = LayoutInflater.from(this).inflate(R.layout.item_sach, null);
        ((TextView) headerView.findViewById(R.id.tvTen)).setText(tensach);
        ((TextView) headerView.findViewById(R.id.tvThongTin)).setText("Mã: " + masach + " | ⭐ " + s[5]);
        ((TextView) headerView.findViewById(R.id.tvGia)).setText(String.format("%,d VNĐ", Integer.parseInt(gia)));
        SachAdapter.loadAnhTuUrl(anhUrl, headerView.findViewById(R.id.imgItemSach));
        mainContent.addView(headerView);

        // Thanh công cụ đọc sách (Cỡ chữ + Đổi màu nền)
        LinearLayout toolbarLayout = new LinearLayout(this);
        toolbarLayout.setOrientation(LinearLayout.HORIZONTAL);
        toolbarLayout.setPadding(0, 10, 0, 10);

        Button btnFontSub = new Button(this); btnFontSub.setText("A-"); btnFontSub.setLayoutParams(new LinearLayout.LayoutParams(0, 100, 1));
        Button btnFontAdd = new Button(this); btnFontAdd.setText("A+"); btnFontAdd.setLayoutParams(new LinearLayout.LayoutParams(0, 100, 1));
        Button btnBgWhite = new Button(this); btnBgWhite.setText("⚪ Sáng"); btnBgWhite.setLayoutParams(new LinearLayout.LayoutParams(0, 100, 1.2f));
        Button btnBgSepia = new Button(this); btnBgSepia.setText("🟡 Vàng"); btnBgSepia.setLayoutParams(new LinearLayout.LayoutParams(0, 100, 1.2f));
        Button btnBgDark = new Button(this); btnBgDark.setText("🌙 Tối"); btnBgDark.setLayoutParams(new LinearLayout.LayoutParams(0, 100, 1.2f));

        toolbarLayout.addView(btnFontSub); toolbarLayout.addView(btnFontAdd);
        toolbarLayout.addView(btnBgWhite); toolbarLayout.addView(btnBgSepia); toolbarLayout.addView(btnBgDark);
        mainContent.addView(toolbarLayout);

        // Nút Yêu thích & Nút Chia sẻ
        LinearLayout actionLayout = new LinearLayout(this);
        actionLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button btnFav = new Button(this);
        boolean isFav = db.isFavorite(currentUsername, masach);
        btnFav.setText(isFav ? "❤️ Đã lưu Tủ sách" : "🤍 Thêm Tủ sách");
        btnFav.setLayoutParams(new LinearLayout.LayoutParams(0, 110, 1));

        Button btnShare = new Button(this);
        btnShare.setText("📤 Chia sẻ sách");
        btnShare.setLayoutParams(new LinearLayout.LayoutParams(0, 110, 1));

        actionLayout.addView(btnFav);
        actionLayout.addView(btnShare);
        mainContent.addView(actionLayout);

        // Tiêu đề khung Nội dung
        TextView tvTitleNoiDung = new TextView(this);
        tvTitleNoiDung.setText("\n📖 NỘI DUNG SÁCH (ĐA PHƯƠNG TIỆN):");
        tvTitleNoiDung.setTypeface(null, Typeface.BOLD);
        tvTitleNoiDung.setPadding(0, 10, 0, 10);
        mainContent.addView(tvTitleNoiDung);

        // Khung Nội dung đọc sách đa phương tiện đan xen Chữ & Ảnh
        LinearLayout readingContainer = new LinearLayout(this);
        readingContainer.setOrientation(LinearLayout.VERTICAL);
        readingContainer.setPadding(20, 20, 20, 20);

        renderMultiMediaContent(noidung, readingContainer, currentTextColor, currentTextSize);
        mainContent.addView(readingContainer);

        // Tùy chỉnh Cỡ chữ
        btnFontSub.setOnClickListener(v -> {
            currentTextSize = Math.max(12f, currentTextSize - 2);
            renderMultiMediaContent(noidung, readingContainer, currentTextColor, currentTextSize);
        });
        btnFontAdd.setOnClickListener(v -> {
            currentTextSize = Math.min(26f, currentTextSize + 2);
            renderMultiMediaContent(noidung, readingContainer, currentTextColor, currentTextSize);
        });

        // Tùy chỉnh Màu nền đọc sách
        btnBgWhite.setOnClickListener(v -> {
            layout.setBackgroundColor(Color.WHITE);
            currentTextColor = Color.BLACK;
            tvTitleNoiDung.setTextColor(Color.BLACK);
            renderMultiMediaContent(noidung, readingContainer, currentTextColor, currentTextSize);
        });
        btnBgSepia.setOnClickListener(v -> {
            layout.setBackgroundColor(0xFFFFF8E1);
            currentTextColor = 0xFF3E2723;
            tvTitleNoiDung.setTextColor(0xFF3E2723);
            renderMultiMediaContent(noidung, readingContainer, currentTextColor, currentTextSize);
        });
        btnBgDark.setOnClickListener(v -> {
            layout.setBackgroundColor(0xFF212121);
            currentTextColor = Color.WHITE;
            tvTitleNoiDung.setTextColor(Color.WHITE);
            renderMultiMediaContent(noidung, readingContainer, currentTextColor, currentTextSize);
        });

        // Nút Yêu thích
        btnFav.setOnClickListener(v -> {
            boolean favState = db.toggleFavorite(currentUsername, masach);
            btnFav.setText(favState ? "❤️ Đã lưu Tủ sách" : "🤍 Thêm Tủ sách");
            Toast.makeText(this, favState ? "Đã lưu vào Tủ sách cá nhân!" : "Đã bỏ khỏi Tủ sách!", Toast.LENGTH_SHORT).show();
            loadData();
        });

        // Nút Chia sẻ qua Zalo / Messenger / Facebook / SMS
        btnShare.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Sách hay: " + tensach);
            intent.putExtra(Intent.EXTRA_TEXT, "📚 Đang đọc cuốn sách hay: \"" + tensach + "\"\nGiá: " + String.format("%,d VNĐ", Integer.parseInt(gia)));
            startActivity(Intent.createChooser(intent, "Chia sẻ sách qua"));
        });

        // Khung Đánh giá ⭐ & Bình luận
        TextView tvLabelRating = new TextView(this);
        tvLabelRating.setText("\n⭐ ĐÁNH GIÁ & BÌNH LUẬN:");
        tvLabelRating.setTypeface(null, Typeface.BOLD);
        tvLabelRating.setPadding(0, 20, 0, 10);
        mainContent.addView(tvLabelRating);

        EditText edtComment = new EditText(this);
        edtComment.setHint("Viết nhận xét của bạn về cuốn sách...");
        mainContent.addView(edtComment);

        Button btnSendReview = new Button(this);
        btnSendReview.setText("Gửi đánh giá 5⭐");
        mainContent.addView(btnSendReview);

        // Hiển thị danh sách các Bình luận đã có
        TextView tvCommentsList = new TextView(this);
        tvCommentsList.setPadding(0, 15, 0, 0);
        ArrayList<String> reviews = db.getRatingsTextList(masach);
        StringBuilder reviewsText = new StringBuilder();
        for (String r : reviews) reviewsText.append(r).append("\n---\n");
        tvCommentsList.setText(reviewsText.length() > 0 ? reviewsText.toString() : "(Chưa có bình luận nào)");
        mainContent.addView(tvCommentsList);

        btnSendReview.setOnClickListener(v -> {
            String cmt = edtComment.getText().toString().trim();
            if (cmt.isEmpty()) { Toast.makeText(this, "Nhập nhận xét của bạn!", Toast.LENGTH_SHORT).show(); return; }
            db.addRating(masach, currentUsername, 5.0f, cmt);
            Toast.makeText(this, "Cảm ơn bạn đã gửi đánh giá!", Toast.LENGTH_SHORT).show();
            edtComment.setText("");
            loadData();
        });

        layout.addView(mainContent);

        new AlertDialog.Builder(this)
                .setTitle("📖 Đọc sách & Đánh giá")
                .setView(layout)
                .setPositiveButton("Đóng", null)
                .show();
    }

    // Hàm nạp đan xen Chữ & Ảnh minh họa trong Nội dung sách
    private void renderMultiMediaContent(String noidung, LinearLayout container, int textColor, float textSize) {
        container.removeAllViews();
        if (noidung == null || noidung.trim().isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("(Chưa có nội dung chi tiết)");
            tvEmpty.setPadding(30, 20, 30, 20);
            tvEmpty.setTextColor(textColor);
            container.addView(tvEmpty);
            return;
        }

        String[] lines = noidung.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Kiểm tra thẻ ảnh minh họa [img]URL[/img] hoặc đường dẫn/Base64 ảnh
            if (trimmed.startsWith("[img]") && trimmed.endsWith("[/img]")) {
                String imgUrl = trimmed.substring(5, trimmed.length() - 6).trim();
                ImageView imgView = new ImageView(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 450);
                lp.setMargins(0, 15, 0, 15);
                imgView.setLayoutParams(lp);
                imgView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                SachAdapter.loadAnhTuUrl(imgUrl, imgView);
                container.addView(imgView);
            } else if ((trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("data:image/"))
                    && (trimmed.contains(".jpg") || trimmed.contains(".jpeg") || trimmed.contains(".png") || trimmed.contains(".webp") || trimmed.contains("base64"))) {
                ImageView imgView = new ImageView(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 450);
                lp.setMargins(0, 15, 0, 15);
                imgView.setLayoutParams(lp);
                imgView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                SachAdapter.loadAnhTuUrl(trimmed, imgView);
                container.addView(imgView);
            } else {
                // Văn bản thông thường
                TextView tvText = new TextView(this);
                tvText.setPadding(10, 6, 10, 6);
                tvText.setTextSize(textSize);
                tvText.setTextColor(textColor);
                tvText.setText(trimmed);
                container.addView(tvText);
            }
        }
    }

    private void loadData() {
        mylist.clear();
        mylist.addAll(db.getDanhSachSach(edtSearch.getText().toString().trim(), orderBy, showingOnlyFavorites, currentUsername));
        myadapter.notifyDataSetChanged();
    }
}
