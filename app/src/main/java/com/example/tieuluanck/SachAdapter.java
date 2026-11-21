package com.example.tieuluanck;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;

/**
 * Custom Adapter để hiển thị thông tin từng dòng sách lên ListView (Kèm điểm ⭐ đánh giá)
 */
public class SachAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<String[]> list;
    private static final HashMap<String, Bitmap> cacheAnh = new HashMap<>();

    public SachAdapter(Context context, ArrayList<String[]> list) {
        this.context = context;
        this.list = list;
    }

    @Override public int getCount() { return list.size(); }
    @Override public Object getItem(int i) { return list.get(i); }
    @Override public long getItemId(int i) { return i; }

    @Override
    public View getView(int i, View view, ViewGroup parent) {
        if (view == null) view = LayoutInflater.from(context).inflate(R.layout.item_sach, parent, false);
        String[] s = list.get(i); // {masach, tensach, gia, anhUrl, noidung, diem_sao}

        // Đổ thông tin text lên TextView
        ((TextView) view.findViewById(R.id.tvTen)).setText(s[1]);
        
        String saoText = s.length >= 6 ? " | ⭐ " + s[5] : "";
        ((TextView) view.findViewById(R.id.tvThongTin)).setText("Mã: " + s[0] + saoText);
        ((TextView) view.findViewById(R.id.tvGia)).setText(String.format("%,d VNĐ", Integer.parseInt(s[2])));

        // Tải và hiển thị ảnh bìa từ URL
        ImageView img = view.findViewById(R.id.imgItemSach);
        loadAnhTuUrl(s[3], img);
        return view;
    }

    public static void loadAnhTuUrl(String url, ImageView img) {
        img.setTag(url);
        if (url == null || url.trim().isEmpty()) {
            img.setImageResource(android.R.drawable.ic_menu_gallery);
            return;
        }
        if (cacheAnh.containsKey(url)) {
            img.setImageBitmap(cacheAnh.get(url));
            return;
        }
        img.setImageResource(android.R.drawable.ic_menu_gallery);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                InputStream is = new URL(url).openStream();
                Bitmap bmp = BitmapFactory.decodeStream(is);
                if (bmp != null) {
                    cacheAnh.put(url, bmp);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (url.equals(img.getTag())) img.setImageBitmap(bmp);
                    });
                }
            } catch (Exception ignored) {}
        });
    }
}
