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
 * Lớp trợ giúp quản lý CSDL SQLite cho ứng dụng Quản Lý Sách (Không dùng thể loại, không thống kê)
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public DatabaseHelper(Context context) {
        // Đặt version lên 10 để tự động nâng cấp/làm mới CSDL nếu máy ảo chứa phiên bản cũ
        super(context, "qlsach_gon.db", null, 10);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbluser (username TEXT PRIMARY KEY, password TEXT, role TEXT)");
        db.execSQL("CREATE TABLE tblsach (masach TEXT PRIMARY KEY, tensach TEXT, gia INTEGER, anh TEXT, noidung TEXT)");

        // Tài khoản Admin mặc định
        db.execSQL("INSERT INTO tbluser VALUES ('admin', '123456', 'admin')");

        // Dữ liệu sách mẫu
        db.execSQL("INSERT INTO tblsach VALUES " +
                "('S01', 'Lập trình Android', 145000, 'https://covers.openlibrary.org/b/id/8259443-M.jpg', 'Sách hướng dẫn lập trình ứng dụng Android từ cơ bản đến nâng cao với Java, SQLite và Jetpack Compose.'), " +
                "('S02', 'Cấu trúc dữ liệu', 180000, 'https://covers.openlibrary.org/b/id/10521270-M.jpg', 'Tổng quan về mảng, danh sách liên kết, cây, đồ thị và các thuật toán sắp xếp, tìm kiếm tối ưu.'), " +
                "('S03', 'Đắc Nhân Tâm', 86000, 'https://covers.openlibrary.org/b/id/12547191-M.jpg', 'Cuốn sách nghệ thuật thu phục lòng người, giao tiếp và ứng xử nghệ thuật hàng đầu thế giới của Dale Carnegie.')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
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

    private String boDauTiengViet(String str) {
        if (str == null) return "";
        String temp = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase();
    }

    // Trả về mảng String[5]: {masach, tensach, gia, anh, noidung}
    public ArrayList<String[]> getDanhSachSach(String keyword, String orderBy) {
        ArrayList<String[]> list = new ArrayList<>();
        String sql = "SELECT masach, tensach, gia, anh, noidung FROM tblsach";
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
                list.add(new String[]{ma, ten, gia, anh, noidung});
            }
        }
        c.close();
        return list;
    }
}
