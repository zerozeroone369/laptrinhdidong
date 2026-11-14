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
 * Lớp trợ giúp quản lý CSDL SQLite cho ứng dụng Quản Lý Sách
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // Khởi tạo CSDL tên "qlsach_gon.db", phiên bản 3
    public DatabaseHelper(Context context) {
        super(context, "qlsach_gon.db", null, 3);
    }

    // Được gọi lần đầu khi ứng dụng được cài đặt để khởi tạo cấu trúc bảng và dữ liệu ban đầu
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tạo bảng tài khoản người dùng
        db.execSQL("CREATE TABLE tbluser (username TEXT PRIMARY KEY, password TEXT)");
        // Tạo bảng thể loại sách
        db.execSQL("CREATE TABLE tblloaisach (maloai TEXT PRIMARY KEY, tenloai TEXT)");
        // Tạo bảng thông tin sách (chứa cột anh lưu link URL)
        db.execSQL("CREATE TABLE tblsach (masach TEXT PRIMARY KEY, tensach TEXT, gia INTEGER, maloai TEXT REFERENCES tblloaisach(maloai), anh TEXT)");

        // Nạp sẵn dữ liệu mẫu mặc định
        db.execSQL("INSERT INTO tbluser VALUES ('admin', '123456')");
        db.execSQL("INSERT INTO tblloaisach VALUES ('CNTT', 'Công nghệ thông tin'), ('KT', 'Kinh tế'), ('VH', 'Văn học'), ('KN', 'Kỹ năng sống')");
        db.execSQL("INSERT INTO tblsach VALUES " +
                "('S01', 'Lập trình Android', 145000, 'CNTT', 'https://covers.openlibrary.org/b/id/8259443-M.jpg'), " +
                "('S02', 'Cấu trúc dữ liệu', 180000, 'CNTT', 'https://covers.openlibrary.org/b/id/10521270-M.jpg'), " +
                "('S03', 'Đắc Nhân Tâm', 86000, 'KN', 'https://covers.openlibrary.org/b/id/12547191-M.jpg')");
    }

    // Được gọi khi nâng cấp phiên bản CSDL (tự động xóa bảng cũ và tạo lại)
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS tblsach");
        db.execSQL("DROP TABLE IF EXISTS tblloaisach");
        db.execSQL("DROP TABLE IF EXISTS tbluser");
        onCreate(db);
    }

    // Kiểm tra thông tin đăng nhập tài khoản
    public boolean checkLogin(String u, String p) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM tbluser WHERE username=? AND password=?", new String[]{u, p});
        boolean ok = c.getCount() > 0;
        c.close();
        return ok;
    }

    // Đăng ký tài khoản người dùng mới
    public boolean register(String u, String p) {
        ContentValues v = new ContentValues();
        v.put("username", u);
        v.put("password", p);
        return getWritableDatabase().insert("tbluser", null, v) != -1;
    }

    // Lấy danh sách thể loại để nạp vào Spinner
    public ArrayList<String> getLoaiSach() {
        ArrayList<String> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT maloai, tenloai FROM tblloaisach", null);
        while (c.moveToNext()) list.add(c.getString(0) + " - " + c.getString(1));
        c.close();
        return list;
    }

    // Thêm một cuốn sách mới vào CSDL
    public boolean insertSach(String ma, String ten, int gia, String maloai, String anhUrl) {
        ContentValues v = new ContentValues();
        v.put("masach", ma); v.put("tensach", ten); v.put("gia", gia); v.put("maloai", maloai); v.put("anh", anhUrl);
        return getWritableDatabase().insert("tblsach", null, v) != -1;
    }

    // Cập nhật thông tin cuốn sách theo mã sách
    public int updateSach(String ma, String ten, int gia, String maloai, String anhUrl) {
        ContentValues v = new ContentValues();
        v.put("tensach", ten); v.put("gia", gia); v.put("maloai", maloai); v.put("anh", anhUrl);
        return getWritableDatabase().update("tblsach", v, "masach=?", new String[]{ma});
    }

    // Xóa cuốn sách khỏi CSDL theo mã sách
    public int deleteSach(String ma) {
        return getWritableDatabase().delete("tblsach", "masach=?", new String[]{ma});
    }

    // Hàm chuyển chữ tiếng Việt có dấu thành không dấu (phục vụ tìm kiếm thông minh)
    private String boDauTiengViet(String str) {
        if (str == null) return "";
        String temp = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase();
    }

    // Lấy danh sách sách kết hợp lọc tìm kiếm không dấu và sắp xếp dữ liệu
    public ArrayList<String[]> getDanhSachSach(String keyword, String orderBy) {
        ArrayList<String[]> list = new ArrayList<>();
        String sql = "SELECT s.masach, s.tensach, s.gia, s.maloai, l.tenloai, s.anh FROM tblsach s " +
                     "LEFT JOIN tblloaisach l ON s.maloai = l.maloai";
        if (!orderBy.isEmpty()) sql += " ORDER BY " + orderBy;

        String keyKhongDau = boDauTiengViet(keyword.trim());

        Cursor c = getReadableDatabase().rawQuery(sql, null);
        while (c.moveToNext()) {
            String ma = c.getString(0);
            String ten = c.getString(1);
            if (keyKhongDau.isEmpty()
                    || boDauTiengViet(ma).contains(keyKhongDau)
                    || boDauTiengViet(ten).contains(keyKhongDau)) {
                String anh = c.getString(5) != null ? c.getString(5) : "";
                list.add(new String[]{ma, ten, String.valueOf(c.getInt(2)), c.getString(3), c.getString(4), anh});
            }
        }
        c.close();
        return list;
    }

    // Tính toán và tổng hợp chuỗi văn bản thống kê kho sách
    public String getThongKeText() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*), SUM(gia), MAX(gia), MIN(gia) FROM tblsach", null);
        String kq = "";
        if (c.moveToFirst()) {
            kq = "• Tổng số đầu sách: " + c.getInt(0) + " cuốn\n\n" +
                 "• Tổng giá trị kho: " + String.format("%,d", c.getInt(1)) + " VNĐ\n\n" +
                 "• Sách giá cao nhất: " + String.format("%,d", c.getInt(2)) + " VNĐ\n\n" +
                 "• Sách giá thấp nhất: " + String.format("%,d", c.getInt(3)) + " VNĐ";
        }
        c.close();
        return kq;
    }
}
