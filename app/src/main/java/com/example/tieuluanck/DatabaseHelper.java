package com.example.tieuluanck;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Lớp nâng cấp CSDL SQLite thực tế: Hỗ trợ Yêu thích, Đánh giá ⭐, Bình luận & Thống kê kho
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public DatabaseHelper(Context context) {
        // Nâng cấp version 11 bổ sung các tính năng ứng dụng thực tế cao
        super(context, "qlsach_gon.db", null, 11);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbluser (username TEXT PRIMARY KEY, password TEXT, role TEXT)");
        db.execSQL("CREATE TABLE tblsach (masach TEXT PRIMARY KEY, tensach TEXT, gia INTEGER, anh TEXT, noidung TEXT)");
        db.execSQL("CREATE TABLE tblfavorite (username TEXT, masach TEXT, PRIMARY KEY (username, masach))");
        db.execSQL("CREATE TABLE tblrating (id INTEGER PRIMARY KEY AUTOINCREMENT, masach TEXT, username TEXT, sao REAL, binhluan TEXT)");

        // Tài khoản Admin mặc định
        db.execSQL("INSERT INTO tbluser VALUES ('admin', '123456', 'admin')");

        // Dữ liệu sách mẫu thực tế
        db.execSQL("INSERT INTO tblsach VALUES " +
                "('S01', 'Lập trình Android', 145000, 'https://covers.openlibrary.org/b/id/8259443-M.jpg', 'Sách hướng dẫn lập trình ứng dụng Android chuyên nghiệp với Java, SQLite và Jetpack Compose.'), " +
                "('S02', 'Cấu trúc dữ liệu', 180000, 'https://covers.openlibrary.org/b/id/10521270-M.jpg', 'Tổng quan về mảng, danh sách liên kết, cây, đồ thị và các thuật toán sắp xếp, tìm kiếm tối ưu.'), " +
                "('S03', 'Đắc Nhân Tâm', 86000, 'https://covers.openlibrary.org/b/id/12547191-M.jpg', 'Cuốn sách nghệ thuật thu phục lòng người, giao tiếp và ứng xử nghệ thuật hàng đầu thế giới của Dale Carnegie.')");

        // Dữ liệu mẫu đánh giá sao
        db.execSQL("INSERT INTO tblrating (masach, username, sao, binhluan) VALUES " +
                "('S01', 'admin', 5.0, 'Sách viết rất hay và trực quan!'), " +
                "('S01', 'khach01', 4.5, 'Nội dung thực tế, dễ học.'), " +
                "('S03', 'khach02', 5.0, 'Cuốn sách gối đầu giường rất xuất sắc.')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS tblrating");
        db.execSQL("DROP TABLE IF EXISTS tblfavorite");
        db.execSQL("DROP TABLE IF EXISTS tblsach");
        db.execSQL("DROP TABLE IF EXISTS tbluser");
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public String checkLoginRole(String u, String p) {
        Cursor c = getReadableDatabase().rawQuery("SELECT role FROM tbluser WHERE username=? AND password=?", new String[]{u, p});
        String role = null;
        if (c.moveToFirst()) {
            role = c.getString(0);
        }
        c.close();
        return role;
    }

    public boolean register(String u, String p) {
        ContentValues v = new ContentValues();
        v.put("username", u);
        v.put("password", p);
        v.put("role", "guest");
        return getWritableDatabase().insert("tbluser", null, v) != -1;
    }

    public boolean insertSach(String ma, String ten, int gia, String anhUrl, String noidung) {
        ContentValues v = new ContentValues();
        v.put("masach", ma); v.put("tensach", ten); v.put("gia", gia); v.put("anh", anhUrl); v.put("noidung", noidung);
        return getWritableDatabase().insert("tblsach", null, v) != -1;
    }

    public int updateSach(String ma, String ten, int gia, String anhUrl, String noidung) {
        ContentValues v = new ContentValues();
        v.put("tensach", ten); v.put("gia", gia); v.put("anh", anhUrl); v.put("noidung", noidung);
        return getWritableDatabase().update("tblsach", v, "masach=?", new String[]{ma});
    }

    public int deleteSach(String ma) {
        return getWritableDatabase().delete("tblsach", "masach=?", new String[]{ma});
    }

    // --- TÍNH NĂNG MỚI 1: TỦ SÁCH YÊU THÍCH ---
    public boolean toggleFavorite(String username, String masach) {
        if (isFavorite(username, masach)) {
            getWritableDatabase().delete("tblfavorite", "username=? AND masach=?", new String[]{username, masach});
            return false;
        } else {
            ContentValues v = new ContentValues();
            v.put("username", username);
            v.put("masach", masach);
            getWritableDatabase().insert("tblfavorite", null, v);
            return true;
        }
    }

    public boolean isFavorite(String username, String masach) {
        Cursor c = getReadableDatabase().rawQuery("SELECT 1 FROM tblfavorite WHERE username=? AND masach=?", new String[]{username, masach});
        boolean fav = c.getCount() > 0;
        c.close();
        return fav;
    }

    // --- TÍNH NĂNG MỚI 2: ĐÁNH GIÁ ⭐ VÀ BÌNH LUẬN ---
    public void addRating(String masach, String username, float sao, String binhluan) {
        ContentValues v = new ContentValues();
        v.put("masach", masach);
        v.put("username", username);
        v.put("sao", sao);
        v.put("binhluan", binhluan);
        getWritableDatabase().insert("tblrating", null, v);
    }

    public float getAverageRating(String masach) {
        Cursor c = getReadableDatabase().rawQuery("SELECT AVG(sao) FROM tblrating WHERE masach=?", new String[]{masach});
        float avg = 5.0f;
        if (c.moveToFirst() && !c.isNull(0)) {
            avg = c.getFloat(0);
        }
        c.close();
        return avg;
    }

    public ArrayList<String> getRatingsTextList(String masach) {
        ArrayList<String> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT username, sao, binhluan FROM tblrating WHERE masach=? ORDER BY id DESC", new String[]{masach});
        while (c.moveToNext()) {
            list.add("👤 " + c.getString(0) + " (⭐ " + String.format("%.1f", c.getFloat(1)) + "):\n\"" + c.getString(2) + "\"");
        }
        c.close();
        return list;
    }

    private String boDauTiengViet(String str) {
        if (str == null) return "";
        String temp = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase();
    }

    // Trả về mảng String[6]: {masach, tensach, gia, anh, noidung, diem_sao}
    public ArrayList<String[]> getDanhSachSach(String keyword, String orderBy, boolean onlyFavorite, String username) {
        ArrayList<String[]> list = new ArrayList<>();
        String sql = "SELECT s.masach, s.tensach, s.gia, s.anh, s.noidung, IFNULL(AVG(r.sao), 5.0) as avg_sao " +
                     "FROM tblsach s " +
                     "LEFT JOIN tblrating r ON s.masach = r.masach ";
        
        if (onlyFavorite) {
            sql += "INNER JOIN tblfavorite f ON s.masach = f.masach AND f.username = '" + username + "' ";
        }
        
        sql += "GROUP BY s.masach ";
        if (!orderBy.isEmpty()) sql += " ORDER BY " + orderBy;

        String keyKhongDau = boDauTiengViet(keyword.trim());

        Cursor c = getReadableDatabase().rawQuery(sql, null);
        while (c.moveToNext()) {
            String ma = c.getString(0);
            String ten = c.getString(1);
            if (keyKhongDau.isEmpty()
                    || boDauTiengViet(ma).contains(keyKhongDau)
                    || boDauTiengViet(ten).contains(keyKhongDau)) {
                String gia = String.valueOf(c.getInt(2));
                String anh = c.getString(3) != null ? c.getString(3) : "";
                String noidung = c.getString(4) != null ? c.getString(4) : "";
                String sao = String.format("%.1f", c.getFloat(5));
                list.add(new String[]{ma, ten, gia, anh, noidung, sao});
            }
        }
        c.close();
        return list;
    }

    // Overload gọn cho Admin
    public ArrayList<String[]> getDanhSachSach(String keyword, String orderBy) {
        return getDanhSachSach(keyword, orderBy, false, "");
    }

    // --- TÍNH NĂNG MỚI 3: THỐNG KÊ DASHBOARD THỰC TẾ FOR ADMIN ---
    public String getDashboardStatsText() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*), SUM(gia), AVG(gia) FROM tblsach", null);
        String kq = "📊 DASHBOARD KHO SÁCH\n\n";
        if (c.moveToFirst()) {
            kq += "• Tổng số đầu sách: " + c.getInt(0) + " cuốn\n" +
                  "• Tổng giá trị kho: " + String.format("%,d", c.getInt(1)) + " VNĐ\n" +
                  "• Giá trung bình/cuốn: " + String.format("%,d", c.getInt(2)) + " VNĐ";
        }
        c.close();
        return kq;
    }
}
