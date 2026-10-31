# Crawlscape – Live Screensaver

(tên cũ: Màn Chờ Kiến Bò)

App Android màn chờ kiểu Windows: để yên máy một lúc thì kiến, gián, chuột & hamster, cá Koi, rắn, mèo đuổi chuột, bướm, đom đóm, bong bóng hoặc mây mù hiện ra phủ lên màn hình. Giao diện tiếng Anh (mặc định) và tiếng Việt. Có thể chọn chạm 1 lần là mở, hoặc chạm 1 lần để trêu con vật và chạm 2 lần liền mới mở. Hoạt cảnh chạy đủ thời gian thì màn tối dần rồi tắt như bình thường.

## Dựng APK bằng GitHub

1. Tạo repo mới trên GitHub, ví dụ `ManChoKienBo`.
2. Giải nén file zip, kéo thả **toàn bộ nội dung bên trong** (gồm cả thư mục `.github`) vào repo, bấm Commit.
   - Nếu giao diện web không nhận thư mục `.github`: bấm *Add file → Create new file*, gõ tên `.github/workflows/build.yml`, dán nội dung file đó vào.
3. Vào tab **Actions**, đợi chạy xong (khoảng 3–5 phút), bấm vào lần chạy, kéo xuống mục **Artifacts**, tải `ManChoKienBo-apk`.
4. Giải nén, cài file `.apk` lên điện thoại.

## Bản AAB cho Google Play (khi cần)

Thêm 4 Secrets trong *Settings → Secrets and variables → Actions*:

- `KEYSTORE_BASE64`: file khóa `.jks` mã hóa base64
- `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

Có đủ thì Actions dựng thêm `ManChoKienBo-aab` đã ký.

## Cách app hoạt động

- Một dịch vụ chạy nền nghe mọi lần chạm để biết máy có đang được dùng, và giữ màn sáng trong lúc đếm.
- Để yên đủ thời gian (mặc định 30 giây) thì phủ hoạt cảnh lên toàn màn hình. Cú chạm vào hoạt cảnh không lọt xuống icon bên dưới.
- Hoạt cảnh chạy đủ thời gian (mặc định 1 phút) thì tối dần, nhả giữ sáng để hệ thống tắt màn.
- Ở màn khóa, hoặc khi đang phát nhạc/video, app không hiện gì.
