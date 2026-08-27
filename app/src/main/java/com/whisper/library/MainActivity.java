package com.whisper.library;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private static final String CATALOG_URL = "https://raw.githubusercontent.com/whispermmepub/whisper-library/android-app/catalog.json";
    private static final String PREFS = "whisper_library";
    private static final String PREF_CATALOG = "cached_catalog";

    private static final int BG = Color.rgb(247, 245, 239);
    private static final int PAPER = Color.WHITE;
    private static final int INK = Color.rgb(30, 38, 43);
    private static final int MUTED = Color.rgb(100, 107, 110);
    private static final int BRAND = Color.rgb(25, 48, 64);
    private static final int ACCENT = Color.rgb(184, 113, 61);
    private static final int LINE = Color.rgb(226, 223, 214);

    private final List<Book> books = new ArrayList<>();
    private String currentCategory = "အားလုံး";
    private String currentQuery = "";
    private boolean showingDetail = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(BRAND);
        window.setNavigationBarColor(BG);

        String initial = loadBundledCatalog();
        String cached = getSharedPreferences(PREFS, MODE_PRIVATE).getString(PREF_CATALOG, "");
        if (!cached.isEmpty()) initial = cached;
        applyCatalog(initial);
        showHome();
        fetchRemoteCatalog();
    }

    private void showHome() {
        showingDetail = false;
        setContentView(buildHome());
    }

    private View buildHome() {
        LinearLayout root = vertical(BG);

        LinearLayout hero = vertical(BRAND);
        hero.setPadding(dp(20), dp(22), dp(20), dp(20));

        TextView brand = text("WHISPER LIBRARY", 12, Color.rgb(218, 198, 179), Typeface.BOLD);
        brand.setLetterSpacing(0.18f);
        hero.addView(brand);

        TextView title = text("မြန်မာ Ebook Library", 27, Color.WHITE, Typeface.BOLD);
        title.setPadding(0, dp(7), 0, dp(4));
        hero.addView(title);

        TextView subtitle = text("စာအုပ်ကိုရှာ • အကြောင်းအရာဖတ် • Google Drive မှရယူ", 14, Color.rgb(220, 226, 229), Typeface.NORMAL);
        hero.addView(subtitle);
        root.addView(hero, matchWrap());

        LinearLayout body = vertical(BG);
        body.setPadding(dp(16), dp(16), dp(16), dp(26));

        EditText search = new EditText(this);
        search.setTextSize(16);
        search.setTextColor(INK);
        search.setHintTextColor(Color.rgb(145, 145, 145));
        search.setHint("စာအုပ်နာမည် / စာရေးသူ ရှာရန်…");
        search.setSingleLine(true);
        search.setPadding(dp(16), 0, dp(16), 0);
        search.setBackground(roundRect(PAPER, 14, LINE, 1));
        search.setText(currentQuery);
        body.addView(search, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        TextView catLabel = text("စာပေအမျိုးအစား", 15, INK, Typeface.BOLD);
        catLabel.setPadding(dp(2), dp(20), 0, dp(10));
        body.addView(catLabel);

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(0, 0, dp(6), dp(2));
        for (String category : categories()) {
            TextView chip = chip(category, category.equals(currentCategory));
            chip.setOnClickListener(v -> {
                currentCategory = category;
                showHome();
            });
            chips.addView(chip);
        }
        hsv.addView(chips);
        body.addView(hsv, matchWrap());

        List<Book> filtered = filteredBooks();
        TextView count = text("စာအုပ် " + filtered.size() + " အုပ်", 14, MUTED, Typeface.NORMAL);
        count.setPadding(dp(2), dp(18), 0, dp(10));
        body.addView(count);

        ScrollView listScroll = new ScrollView(this);
        listScroll.setFillViewport(true);
        LinearLayout list = vertical(BG);

        if (filtered.isEmpty()) {
            LinearLayout empty = vertical(PAPER);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(24), dp(42), dp(24), dp(42));
            empty.setBackground(roundRect(PAPER, 16, LINE, 1));
            empty.addView(text("ရှာမတွေ့ပါ", 20, INK, Typeface.BOLD));
            TextView hint = text("အခြားစာလုံးဖြင့် ရှာကြည့်ပါ သို့မဟုတ် အမျိုးအစားပြောင်းပါ။", 14, MUTED, Typeface.NORMAL);
            hint.setGravity(Gravity.CENTER);
            hint.setPadding(0, dp(8), 0, 0);
            empty.addView(hint);
            list.addView(empty, matchWrap());
        } else {
            for (Book book : filtered) {
                list.addView(bookCard(book), matchWrapWithBottom(dp(12)));
            }
        }

        TextView footer = text("Online catalog ကို app ဖွင့်တိုင်း update စစ်ပေးသည်။", 12, MUTED, Typeface.NORMAL);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(10), 0, dp(4));
        list.addView(footer);

        listScroll.addView(list);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        body.addView(listScroll, scrollParams);
        root.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String q = s.toString();
                if (!q.equals(currentQuery)) {
                    currentQuery = q;
                    search.postDelayed(() -> {
                        if (!showingDetail && currentQuery.equals(q)) showHome();
                    }, 180);
                }
            }
        });

        search.post(() -> {
            search.setSelection(search.length());
            if (!currentQuery.isEmpty()) search.requestFocus();
        });
        return root;
    }

    private View bookCard(Book book) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackground(roundRect(PAPER, 16, LINE, 1));
        card.setClickable(true);
        card.setFocusable(true);

        View cover = coverView(book, dp(82), dp(116));
        card.addView(cover);

        LinearLayout info = vertical(Color.TRANSPARENT);
        info.setPadding(dp(14), 0, 0, 0);
        TextView category = text(book.category.isEmpty() ? "စာအုပ်" : book.category, 12, ACCENT, Typeface.BOLD);
        info.addView(category);

        TextView title = text(book.title, 18, INK, Typeface.BOLD);
        title.setPadding(0, dp(5), 0, dp(4));
        title.setMaxLines(2);
        info.addView(title);

        TextView author = text(book.author.isEmpty() ? "စာရေးသူ မသတ်မှတ်ရသေး" : book.author, 14, MUTED, Typeface.NORMAL);
        info.addView(author);

        String meta = metaLine(book);
        if (!meta.isEmpty()) {
            TextView metaView = text(meta, 12, Color.rgb(125, 125, 125), Typeface.NORMAL);
            metaView.setPadding(0, dp(8), 0, 0);
            info.addView(metaView);
        }

        card.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.setOnClickListener(v -> showBook(book));
        return card;
    }

    private void showBook(Book book) {
        showingDetail = true;
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout root = vertical(BG);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(12), dp(10), dp(12), dp(10));
        top.setBackgroundColor(BRAND);

        TextView back = text("‹  စာအုပ်များ", 16, Color.WHITE, Typeface.BOLD);
        back.setGravity(Gravity.CENTER_VERTICAL);
        back.setPadding(dp(8), dp(9), dp(12), dp(9));
        back.setOnClickListener(v -> showHome());
        top.addView(back, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView share = text("မျှဝေ", 14, Color.WHITE, Typeface.BOLD);
        share.setPadding(dp(14), dp(9), dp(14), dp(9));
        share.setBackground(roundRect(Color.argb(35, 255, 255, 255), 12, Color.TRANSPARENT, 0));
        share.setOnClickListener(v -> shareBook(book));
        top.addView(share);
        root.addView(top, matchWrap());

        LinearLayout content = vertical(BG);
        content.setPadding(dp(20), dp(22), dp(20), dp(34));

        FrameLayout coverHolder = new FrameLayout(this);
        coverHolder.setForegroundGravity(Gravity.CENTER);
        View cover = coverView(book, dp(180), dp(252));
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(dp(180), dp(252), Gravity.CENTER);
        coverHolder.addView(cover, cp);
        content.addView(coverHolder, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(270)));

        TextView cat = text(book.category.isEmpty() ? "စာအုပ်" : book.category, 13, ACCENT, Typeface.BOLD);
        cat.setGravity(Gravity.CENTER);
        content.addView(cat);

        TextView title = text(book.title, 27, INK, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(dp(8), dp(8), dp(8), dp(7));
        content.addView(title);

        TextView author = text(book.author, 16, MUTED, Typeface.NORMAL);
        author.setGravity(Gravity.CENTER);
        content.addView(author);

        String meta = metaLine(book);
        if (!meta.isEmpty()) {
            TextView mv = text(meta, 13, MUTED, Typeface.NORMAL);
            mv.setGravity(Gravity.CENTER);
            mv.setPadding(0, dp(7), 0, 0);
            content.addView(mv);
        }

        content.addView(spacer(18));
        content.addView(section("စာအုပ်အကြောင်း", safe(book.description, "စာအုပ်အကြောင်း မထည့်ရသေးပါ။")));
        content.addView(spacer(14));
        content.addView(section("စာရေးသူအကြောင်း", safe(book.authorBio, "စာရေးသူအကြောင်း မထည့်ရသေးပါ။")));
        content.addView(spacer(18));

        TextView dlTitle = text("ဖိုင်ရယူရန်", 18, INK, Typeface.BOLD);
        dlTitle.setPadding(dp(2), 0, 0, dp(10));
        content.addView(dlTitle);

        boolean hasLink = false;
        for (FormatLink f : book.formats) {
            if (!f.url.trim().isEmpty()) hasLink = true;
            Button b = actionButton(f.label + "  •  Google Drive", !f.url.trim().isEmpty());
            b.setOnClickListener(v -> openUrl(f.url));
            content.addView(b, matchWrapWithBottom(dp(10)));
        }
        if (book.formats.isEmpty() || !hasLink) {
            TextView note = text("ဒီနမူနာစာအုပ်မှာ download link မထည့်ရသေးပါ။ catalog.json ထဲ Google Drive link ထည့်လိုက်ရင် button က အလုပ်လုပ်ပါမယ်။", 13, MUTED, Typeface.NORMAL);
            note.setPadding(dp(14), dp(13), dp(14), dp(13));
            note.setBackground(roundRect(PAPER, 12, LINE, 1));
            content.addView(note);
        }

        scroll.addView(root);
        root.addView(content, matchWrap());
        setContentView(scroll);
    }

    private View section(String heading, String body) {
        LinearLayout box = vertical(PAPER);
        box.setPadding(dp(16), dp(16), dp(16), dp(17));
        box.setBackground(roundRect(PAPER, 15, LINE, 1));
        box.addView(text(heading, 18, INK, Typeface.BOLD));
        TextView b = text(body, 15, Color.rgb(67, 72, 74), Typeface.NORMAL);
        b.setPadding(0, dp(9), 0, 0);
        b.setLineSpacing(0, 1.35f);
        box.addView(b);
        return box;
    }

    private Button actionButton(String label, boolean enabled) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(enabled ? Color.WHITE : Color.rgb(145, 145, 145));
        b.setEnabled(enabled);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(14), dp(7), dp(14), dp(7));
        b.setBackground(roundRect(enabled ? BRAND : Color.rgb(232, 232, 230), 13, Color.TRANSPARENT, 0));
        b.setMinHeight(dp(52));
        return b;
    }

    private TextView chip(String label, boolean selected) {
        TextView t = text(label, 13, selected ? Color.WHITE : INK, selected ? Typeface.BOLD : Typeface.NORMAL);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(15), dp(9), dp(15), dp(9));
        t.setBackground(roundRect(selected ? BRAND : PAPER, 22, selected ? BRAND : LINE, 1));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, dp(8), 0);
        t.setLayoutParams(p);
        return t;
    }

    private View coverView(Book book, int width, int height) {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackground(roundRect(coverColor(book.id), 12, Color.argb(30, 0, 0, 0), 1));
        frame.setClipToOutline(true);

        TextView letter = text(firstLetter(book.title), width > dp(100) ? 46 : 30, Color.WHITE, Typeface.BOLD);
        letter.setGravity(Gravity.CENTER);
        frame.addView(letter, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (!book.coverUrl.trim().isEmpty()) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setContentDescription(book.title + " cover");
            frame.addView(iv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            loadImage(book.coverUrl, iv);
        }
        frame.setLayoutParams(new LinearLayout.LayoutParams(width, height));
        return frame;
    }

    private int coverColor(String id) {
        int[] colors = {
                Color.rgb(73, 89, 99), Color.rgb(117, 77, 68), Color.rgb(65, 104, 96),
                Color.rgb(116, 92, 57), Color.rgb(82, 77, 112), Color.rgb(109, 72, 89)
        };
        int h = id == null ? 0 : id.hashCode();
        return colors[Math.abs(h == Integer.MIN_VALUE ? 0 : h) % colors.length];
    }

    private void loadImage(String urlString, ImageView imageView) {
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URL(urlString).openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(10000);
                c.setInstanceFollowRedirects(true);
                c.connect();
                if (c.getResponseCode() >= 200 && c.getResponseCode() < 300) {
                    Bitmap bitmap = BitmapFactory.decodeStream(c.getInputStream());
                    if (bitmap != null) runOnUiThread(() -> imageView.setImageBitmap(bitmap));
                }
            } catch (Exception ignored) {
            } finally {
                if (c != null) c.disconnect();
            }
        }).start();
    }

    private void fetchRemoteCatalog() {
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URL(CATALOG_URL).openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(10000);
                c.setRequestProperty("Accept", "application/json");
                c.setRequestProperty("User-Agent", "WhisperLibrary/1.0");
                int code = c.getResponseCode();
                if (code >= 200 && code < 300) {
                    String json = readStream(c.getInputStream());
                    new JSONObject(json);
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(PREF_CATALOG, json).apply();
                    runOnUiThread(() -> {
                        applyCatalog(json);
                        if (!showingDetail) showHome();
                    });
                }
            } catch (Exception ignored) {
            } finally {
                if (c != null) c.disconnect();
            }
        }).start();
    }

    private String loadBundledCatalog() {
        try {
            return readStream(getAssets().open("catalog.json"));
        } catch (Exception e) {
            return "{\"books\":[]}";
        }
    }

    private void applyCatalog(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONArray arr = root.optJSONArray("books");
            if (arr == null) return;
            List<Book> parsed = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Book b = new Book();
                b.id = o.optString("id", "book-" + i);
                b.title = o.optString("title", "စာအုပ်");
                b.author = o.optString("author", "");
                b.category = o.optString("category", "အခြား");
                b.description = o.optString("description", "");
                b.authorBio = o.optString("authorBio", "");
                b.year = o.optString("year", "");
                b.coverUrl = o.optString("coverUrl", "");
                JSONArray fa = o.optJSONArray("formats");
                if (fa != null) {
                    for (int j = 0; j < fa.length(); j++) {
                        JSONObject f = fa.optJSONObject(j);
                        if (f != null) b.formats.add(new FormatLink(f.optString("label", "FILE"), f.optString("url", "")));
                    }
                }
                if (!b.title.trim().isEmpty()) parsed.add(b);
            }
            if (!parsed.isEmpty() || arr.length() == 0) {
                books.clear();
                books.addAll(parsed);
            }
        } catch (Exception ignored) {
        }
    }

    private List<String> categories() {
        Set<String> set = new LinkedHashSet<>();
        set.add("အားလုံး");
        for (Book b : books) if (!b.category.trim().isEmpty()) set.add(b.category.trim());
        if (!set.contains(currentCategory)) currentCategory = "အားလုံး";
        return new ArrayList<>(set);
    }

    private List<Book> filteredBooks() {
        String q = currentQuery.trim().toLowerCase(Locale.ROOT);
        List<Book> out = new ArrayList<>();
        for (Book b : books) {
            boolean categoryOk = currentCategory.equals("အားလုံး") || currentCategory.equals(b.category);
            String hay = (b.title + " " + b.author + " " + b.category + " " + b.description).toLowerCase(Locale.ROOT);
            boolean queryOk = q.isEmpty() || hay.contains(q);
            if (categoryOk && queryOk) out.add(b);
        }
        return out;
    }

    private String metaLine(Book b) {
        List<String> bits = new ArrayList<>();
        if (!b.year.trim().isEmpty()) bits.add(b.year.trim());
        if (!b.formats.isEmpty()) {
            List<String> labels = new ArrayList<>();
            for (FormatLink f : b.formats) if (!f.label.trim().isEmpty()) labels.add(f.label.trim());
            if (!labels.isEmpty()) bits.add(String.join(" • ", labels));
        }
        return String.join("  |  ", bits);
    }

    private void openUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(this, "Download link မထည့်ရသေးပါ။", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url.trim()));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Link ကို ဖွင့်လို့မရပါ။", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareBook(Book b) {
        StringBuilder s = new StringBuilder();
        s.append(b.title);
        if (!b.author.trim().isEmpty()) s.append("\n").append(b.author);
        if (!b.description.trim().isEmpty()) s.append("\n\n").append(b.description);
        for (FormatLink f : b.formats) {
            if (!f.url.trim().isEmpty()) {
                s.append("\n\n").append(f.label).append(": ").append(f.url);
                break;
            }
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, s.toString());
        startActivity(Intent.createChooser(send, "စာအုပ်ကို မျှဝေရန်"));
    }

    @Override
    public void onBackPressed() {
        if (showingDetail) showHome();
        else super.onBackPressed();
    }

    private LinearLayout vertical(int color) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackgroundColor(color);
        return l;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView t = new TextView(this);
        t.setText(value == null ? "" : value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create(Typeface.DEFAULT, style));
        return t;
    }

    private Space spacer(int dp) {
        Space s = new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(1, dp(dp)));
        return s;
    }

    private GradientDrawable roundRect(int fill, int radiusDp, int stroke, int strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0 && Color.alpha(stroke) > 0) d.setStroke(dp(strokeDp), stroke);
        return d;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams matchWrapWithBottom(int bottom) {
        LinearLayout.LayoutParams p = matchWrap();
        p.setMargins(0, 0, 0, bottom);
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String firstLetter(String s) {
        if (s == null || s.trim().isEmpty()) return "W";
        String t = s.trim();
        int cp = t.codePointAt(0);
        return new String(Character.toChars(cp));
    }

    private String safe(String s, String fallback) {
        return s == null || s.trim().isEmpty() ? fallback : s;
    }

    private String readStream(InputStream input) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append('\n');
        reader.close();
        return sb.toString();
    }

    private static class FormatLink {
        final String label;
        final String url;
        FormatLink(String label, String url) {
            this.label = label == null ? "FILE" : label;
            this.url = url == null ? "" : url;
        }
    }

    private static class Book {
        String id = "";
        String title = "";
        String author = "";
        String category = "";
        String description = "";
        String authorBio = "";
        String year = "";
        String coverUrl = "";
        final List<FormatLink> formats = new ArrayList<>();
    }
}
