import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Builds the DuGO launcher icons from the app logo.
 *
 * The logo is a red bar with "DuGO" knocked out of it and, at the right, a
 * square badge holding a red blood drop with two hands reaching into it. The
 * icon uses that drop, drawn in white on the brand red, so it stays legible
 * at small sizes.
 *
 * Run with: java tools/LogoTool.java &lt;logo.png&gt; &lt;res-dir&gt;
 */
public class LogoTool {

    static final Color BRAND = new Color(0xED, 0x1C, 0x24);

    static BufferedImage logo;
    static int W, H;

    /** The logo is a PNG with an alpha channel: the cut-out parts are see-through. */
    static boolean cutOut(int x, int y) {
        return ((logo.getRGB(x, y) >>> 24) & 255) < 140;
    }

    /** An opaque red pixel — the blood drop. */
    static boolean isDrop(int x, int y) {
        if (cutOut(x, y)) return false;
        int c = logo.getRGB(x, y);
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        return r > 170 && g < 110 && b < 110;
    }

    /** Flood fill over the pixels that match the test. */
    static boolean[][] flood(int sx, int sy, java.util.function.IntPredicate test) {
        boolean[][] seen = new boolean[H][W];
        Deque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{sx, sy});
        seen[sy][sx] = true;
        int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty()) {
            int[] c = q.poll();
            int x = c[0], y = c[1];
            for (int[] dd : d) {
                int nx = x + dd[0], ny = y + dd[1];
                if (nx < 0 || ny < 0 || nx >= W || ny >= H || seen[ny][nx]) continue;
                if (test.test(ny * W + nx)) {
                    seen[ny][nx] = true;
                    q.add(new int[]{nx, ny});
                }
            }
        }
        return seen;
    }

    static int[] bbox(boolean[][] mask) {
        int minX = W, maxX = -1, minY = H, maxY = -1;
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++)
                if (mask[y][x]) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
        return new int[]{minX, minY, maxX, maxY};
    }

    public static void main(String[] args) throws Exception {
        logo = ImageIO.read(new File(args[0]));
        W = logo.getWidth();
        H = logo.getHeight();
        File res = new File(args[1]);

        // The badge is a square cut out of the red bar, on the right of the
        // wordmark. Cut-out pixels that the bar encloses are the badge.
        boolean[][] outside = flood(0, 0, k -> cutOut(k % W, k / W));

        boolean[][] badgeSquare = new boolean[H][W];
        for (int y = 0; y < H; y++)
            for (int x = 460; x < W; x++)
                if (cutOut(x, y) && !outside[y][x]) badgeSquare[y][x] = true;

        int[] badgeBox = bbox(badgeSquare);
        System.out.printf("badge square: x %d..%d y %d..%d (%dx%d)%n",
                badgeBox[0], badgeBox[2], badgeBox[1], badgeBox[3],
                badgeBox[2] - badgeBox[0] + 1, badgeBox[3] - badgeBox[1] + 1);

        // The drop is the opaque red inside the badge. Flood it from a point
        // inside the drop so the surrounding panel red stays out.
        boolean[][] drop = flood(502, 120, k -> isDrop(k % W, k / W));

        int[] dropBox = bbox(drop);
        System.out.printf("drop: x %d..%d y %d..%d (%dx%d)%n",
                dropBox[0], dropBox[2], dropBox[1], dropBox[3],
                dropBox[2] - dropBox[0] + 1, dropBox[3] - dropBox[1] + 1);

        // The hands are cut-outs inside the drop. Anything in the drop's box that
        // is not the drop and cannot be reached from the edge is a hand.
        boolean[][] outsideDrop = new boolean[H][W];
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) outsideDrop[y][x] = !drop[y][x];

        boolean[][] reachable = new boolean[H][W];
        Deque<int[]> q = new ArrayDeque<>();
        int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int x = dropBox[0]; x <= dropBox[2]; x++) {
            seed(q, reachable, outsideDrop, x, dropBox[1]);
            seed(q, reachable, outsideDrop, x, dropBox[3]);
        }
        for (int y = dropBox[1]; y <= dropBox[3]; y++) {
            seed(q, reachable, outsideDrop, dropBox[0], y);
            seed(q, reachable, outsideDrop, dropBox[2], y);
        }
        while (!q.isEmpty()) {
            int[] c = q.poll();
            int x = c[0], y = c[1];
            for (int[] dd : d) {
                int nx = x + dd[0], ny = y + dd[1];
                if (nx < 0 || ny < 0 || nx >= W || ny >= H || reachable[ny][nx]) continue;
                if (outsideDrop[ny][nx]) {
                    reachable[ny][nx] = true;
                    q.add(new int[]{nx, ny});
                }
            }
        }

        // The solid drop: the drop plus the hands that reach into it.
        int dw = dropBox[2] - dropBox[0] + 1;
        int dh = dropBox[3] - dropBox[1] + 1;
        BufferedImage solid = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < dh; y++)
            for (int x = 0; x < dw; x++) {
                int sx = dropBox[0] + x, sy = dropBox[1] + y;
                boolean on = drop[sy][sx] || !reachable[sy][sx];
                solid.setRGB(x, y, on ? 0xFFFFFFFF : 0x00000000);
            }

        ImageIO.write(solid, "png", new File("C:/Users/Joseph/AppData/Local/Temp/solid_drop.png"));
        BufferedImage preview = new BufferedImage(dw + 20, dh + 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D pg = preview.createGraphics();
        pg.setColor(BRAND);
        pg.fillRect(0, 0, preview.getWidth(), preview.getHeight());
        pg.drawImage(solid, 10, 10, null);
        pg.dispose();
        ImageIO.write(preview, "png", new File("C:/Users/Joseph/AppData/Local/Temp/solid_preview.png"));

        writeAdaptiveForeground(solid, res);
        writeLegacyIcons(solid, res);
        System.out.println("done " + dw + "x" + dh);
    }

    static void seed(Deque<int[]> q, boolean[][] reachable, boolean[][] outsideDrop, int x, int y) {
        if (x < 0 || y < 0 || x >= W || y >= H || reachable[y][x]) return;
        if (!outsideDrop[y][x]) return;
        reachable[y][x] = true;
        q.add(new int[]{x, y});
    }

    /** The adaptive icon foreground: the drop inside the 66/108 safe zone. */
    static void writeAdaptiveForeground(BufferedImage solid, File res) throws Exception {
        String[] densities = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
        int[] dpis = {48, 72, 96, 144, 192};
        for (int i = 0; i < densities.length; i++) {
            int canvas = Math.round(dpis[i] * 108f / 48f);
            int safe = Math.round(canvas * 66f / 108f);
            int w = Math.round(solid.getWidth() * (safe / (float) solid.getHeight()));
            if (w > safe) w = safe;

            BufferedImage scaled = new BufferedImage(w, safe, BufferedImage.TYPE_INT_ARGB);
            Graphics2D sg = scaled.createGraphics();
            sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            sg.drawImage(solid, 0, 0, w, safe, null);
            sg.dispose();

            BufferedImage fg = new BufferedImage(canvas, canvas, BufferedImage.TYPE_INT_ARGB);
            Graphics2D fg2 = fg.createGraphics();
            fg2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            fg2.drawImage(scaled, (canvas - w) / 2, (canvas - safe) / 2, null);
            fg2.dispose();

            File dir = new File(res, "mipmap-" + densities[i]);
            dir.mkdirs();
            ImageIO.write(fg, "png", new File(dir, "ic_launcher_foreground.png"));
        }
        System.out.println("adaptive foreground written");
    }

    /** The legacy icon: the drop in white on a rounded brand-red plate. */
    static void writeLegacyIcons(BufferedImage solid, File res) throws Exception {
        String[] densities = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
        int[] dpis = {48, 72, 96, 144, 192};
        for (int i = 0; i < densities.length; i++) {
            int s = dpis[i];
            BufferedImage icon = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = icon.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(BRAND);
            g.fillRoundRect(0, 0, s, s, s / 4, s / 4);
            int inset = Math.round(s * 0.22f);
            int box = s - inset * 2;
            int w = Math.round(solid.getWidth() * (box / (float) solid.getHeight()));
            if (w > box) w = box;
            int h = Math.round(w * (solid.getHeight() / (float) solid.getWidth()));
            g.drawImage(solid, (s - w) / 2, (s - h) / 2, w, h, null);
            g.dispose();

            File dir = new File(res, "mipmap-" + densities[i]);
            dir.mkdirs();
            ImageIO.write(icon, "png", new File(dir, "ic_launcher.png"));
            ImageIO.write(icon, "png", new File(dir, "ic_launcher_round.png"));
        }
        System.out.println("legacy icons written");
    }
}
