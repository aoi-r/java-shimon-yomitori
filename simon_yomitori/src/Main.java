import com.fazecast.jSerialComm.SerialPort;

public class Main {
    public static void main(String[] args) throws Exception {
        SerialPort port = SerialPort.getCommPort("COM5"); // 適宜COMポートを調整
        port.setComPortParameters(57600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 1000, 0);

        if (!port.openPort()) {
            System.out.println("❌ ポートを開けませんでした。");
            return;
        }

        System.out.println("✅ COM5を開きました。照合したい指を置いてください...");
        Thread.sleep(2000); // 指を置く時間の確保

        // ==== GenImg ====
        byte[] cmd1 = {
                (byte) 0xEF, 0x01, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                0x01, 0x00, 0x03, 0x01, 0x00, 0x05
        };
        port.writeBytes(cmd1, cmd1.length);
        byte[] res1 = new byte[12];
        port.readBytes(res1, res1.length);
        System.out.println("[GenImg] 応答: " + bytesToHex(res1));
        if (res1[9] != 0x00) {
            System.out.println("❌ 指画像取得失敗（ステータスコード: " + String.format("%02X", res1[9]) + ")");
            port.closePort();
            return;
        }

        // ==== Img2Tz (バッファ1) ====
        byte[] cmd2 = {
                (byte) 0xEF, 0x01, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                0x01, 0x00, 0x04, 0x02, 0x01, 0x00, 0x08
        };
        port.writeBytes(cmd2, cmd2.length);
        byte[] res2 = new byte[12];
        port.readBytes(res2, res2.length);
        System.out.println("[Img2Tz] 応答: " + bytesToHex(res2));
        if (res2[9] != 0x00) {
            System.out.println("❌ 特徴抽出失敗（ステータスコード: " + String.format("%02X", res2[9]) + ")");
            port.closePort();
            return;
        }

        // ==== Search (範囲：0～255) ====
        byte[] cmd3 = {
                (byte) 0xEF, 0x01,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                0x01,
                0x00, 0x08,
                0x04,
                0x01,
                0x00, 0x00,
                0x00, (byte) 0x64,
                0x00, 0x72
        };
        port.writeBytes(cmd3, cmd3.length);
        byte[] res3 = new byte[16];
        int bytesRead = port.readBytes(res3, res3.length);
        System.out.println("[Search] 応答: " + bytesToHex(res3));

        if (bytesRead >= 12 && res3[9] == 0x00) {
            int matchedIndex = ((res3[10] & 0xFF) << 8) | (res3[11] & 0xFF);
            int score = ((res3[12] & 0xFF) << 8) | (res3[13] & 0xFF);
            System.out.println("✅ 一致しました！インデックス: " + matchedIndex + "　スコア: " + score);
        } else {
            String status = bytesRead >= 12 ? String.format("%02X", res3[9]) : "N/A";
            System.out.println("❌ 一致する指紋が見つかりませんでした（ステータス: " + status + "）");
        }

        port.closePort();
    }

    private static String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}
