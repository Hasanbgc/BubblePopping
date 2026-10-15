import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

/**
 * Converts Figma-exported SVGs into Kotlin ImageVector builders.
 * Usage: java SvgToKotlin.java out.kt name=file.svg name2=file2.svg ...
 * Supports path, g (opacity; filters are dropped), linear and radial gradients
 * (userSpaceOnUse, translate/scale transform). Output is centred in a square viewport.
 */
public class SvgToKotlin {
    static Map<String, Element> defs = new HashMap<>();
    static StringBuilder out = new StringBuilder();

    public static void main(String[] args) throws Exception {
        StringBuilder body = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            String[] kv = args[i].split("=", 2);
            body.append(convert(kv[0], new File(kv[1])));
        }
        String header = """
            package com.apps.bubblepopping.ui.icons

            import androidx.compose.ui.geometry.Offset
            import androidx.compose.ui.graphics.Brush
            import androidx.compose.ui.graphics.Color
            import androidx.compose.ui.graphics.SolidColor
            import androidx.compose.ui.graphics.StrokeCap
            import androidx.compose.ui.graphics.StrokeJoin
            import androidx.compose.ui.graphics.vector.ImageVector
            import androidx.compose.ui.graphics.vector.addPathNodes
            import androidx.compose.ui.graphics.vector.group
            import androidx.compose.ui.unit.dp

            // GENERATED from the SVGs in composeResources/drawable (Figma exports).
            // Edit the SVG and regenerate rather than editing this file by hand.
            // Blur filters are dropped and elliptical radial gradients become circles.

            """;
        Files.writeString(new File(args[0]).toPath(), header + body);
    }

    static String convert(String name, File file) throws Exception {
        var dbf = DocumentBuilderFactory.newInstance();
        Document doc = dbf.newDocumentBuilder().parse(file);
        Element svg = doc.getDocumentElement();
        defs.clear();
        for (String tag : List.of("linearGradient", "radialGradient")) {
            NodeList nl = doc.getElementsByTagName(tag);
            for (int i = 0; i < nl.getLength(); i++) {
                Element e = (Element) nl.item(i);
                defs.put(e.getAttribute("id"), e);
            }
        }
        float w = Float.parseFloat(svg.getAttribute("width"));
        float h = Float.parseFloat(svg.getAttribute("height"));
        float side = Math.max(w, h);
        out = new StringBuilder();
        out.append("internal val ").append(name).append(": ImageVector by lazy {\n");
        out.append("    ImageVector.Builder(\n");
        out.append("        name = \"").append(name).append("\",\n");
        out.append("        defaultWidth = 24.dp,\n        defaultHeight = 24.dp,\n");
        out.append("        viewportWidth = ").append(f(side)).append(",\n");
        out.append("        viewportHeight = ").append(f(side)).append(",\n");
        out.append("    ).apply {\n");
        out.append("        group(translationX = ").append(f((side - w) / 2)).append(", translationY = ").append(f((side - h) / 2)).append(") {\n");
        walk(svg, 1f, "            ");
        out.append("        }\n    }.build()\n}\n\n");
        return out.toString();
    }

    static void walk(Element parent, float opacity, String indent) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (!(n instanceof Element e)) continue;
            switch (e.getTagName()) {
                case "g" -> walk(e, opacity * num(e, "opacity", 1f), indent);
                case "path" -> path(e, opacity, indent);
                case "defs" -> {}
                default -> throw new IllegalStateException("Unsupported element <" + e.getTagName() + ">");
            }
        }
    }

    static void path(Element e, float opacity, String in) {
        float op = opacity * num(e, "opacity", 1f);
        String fill = e.getAttribute("fill");
        String stroke = e.getAttribute("stroke");
        out.append(in).append("addPath(\n");
        out.append(in).append("    pathData = addPathNodes(\"").append(e.getAttribute("d")).append("\"),\n");
        if (!fill.isEmpty() && !fill.equals("none")) {
            out.append(in).append("    fill = ").append(paint(fill)).append(",\n");
            float a = op * num(e, "fill-opacity", 1f);
            if (a != 1f) out.append(in).append("    fillAlpha = ").append(f(a)).append(",\n");
        }
        if (!stroke.isEmpty() && !stroke.equals("none")) {
            out.append(in).append("    stroke = ").append(paint(stroke)).append(",\n");
            float a = op * num(e, "stroke-opacity", 1f);
            if (a != 1f) out.append(in).append("    strokeAlpha = ").append(f(a)).append(",\n");
            out.append(in).append("    strokeLineWidth = ").append(f(num(e, "stroke-width", 1f))).append(",\n");
            String cap = e.getAttribute("stroke-linecap");
            if (!cap.isEmpty()) out.append(in).append("    strokeLineCap = StrokeCap.").append(cap(cap)).append(",\n");
            String join = e.getAttribute("stroke-linejoin");
            if (!join.isEmpty()) out.append(in).append("    strokeLineJoin = StrokeJoin.").append(cap(join)).append(",\n");
        }
        out.append(in).append(")\n");
    }

    static String paint(String value) {
        Matcher m = Pattern.compile("url\\(#(.+)\\)").matcher(value);
        if (!m.matches()) return "SolidColor(" + color(value, 1f) + ")";
        Element g = defs.get(m.group(1));
        if (g == null) throw new IllegalStateException("Missing gradient " + value);
        if (!"userSpaceOnUse".equals(g.getAttribute("gradientUnits")))
            throw new IllegalStateException("Unsupported gradientUnits in " + value);
        StringBuilder stops = new StringBuilder();
        NodeList nl = g.getElementsByTagName("stop");
        for (int i = 0; i < nl.getLength(); i++) {
            Element s = (Element) nl.item(i);
            stops.append(f(num(s, "offset", 0f))).append(" to ")
                .append(color(s.getAttribute("stop-color"), num(s, "stop-opacity", 1f))).append(", ");
        }
        if (g.getTagName().equals("linearGradient")) {
            return "Brush.linearGradient(" + stops + "start = Offset(" + f(num(g, "x1", 0f)) + ", " + f(num(g, "y1", 0f))
                + "), end = Offset(" + f(num(g, "x2", 0f)) + ", " + f(num(g, "y2", 0f)) + "))";
        }
        // Figma radial: cx=0 cy=0 r=1 with translate(tx ty) scale(sx [sy]).
        String t = g.getAttribute("gradientTransform");
        Matcher tm = Pattern.compile("translate\\(([-\\d.e]+) ([-\\d.e]+)\\) scale\\(([-\\d.e]+)(?: ([-\\d.e]+))?\\)").matcher(t);
        float cx, cy, r;
        if (tm.matches()) {
            cx = Float.parseFloat(tm.group(1));
            cy = Float.parseFloat(tm.group(2));
            float sx = Math.abs(Float.parseFloat(tm.group(3)));
            float sy = tm.group(4) == null ? sx : Math.abs(Float.parseFloat(tm.group(4)));
            r = (sx + sy) / 2f;
        } else if (t.isEmpty()) {
            cx = num(g, "cx", 0f); cy = num(g, "cy", 0f); r = num(g, "r", 1f);
        } else throw new IllegalStateException("Unsupported gradientTransform " + t);
        return "Brush.radialGradient(" + stops + "center = Offset(" + f(cx) + ", " + f(cy) + "), radius = " + f(r) + ")";
    }

    static String color(String c, float alpha) {
        String hex;
        if (c.equals("white")) hex = "FFFFFF";
        else if (c.equals("black")) hex = "000000";
        else if (c.matches("#[0-9A-Fa-f]{6}")) hex = c.substring(1).toUpperCase();
        else if (c.matches("#[0-9A-Fa-f]{3}")) {
            String s = c.substring(1).toUpperCase();
            hex = "" + s.charAt(0) + s.charAt(0) + s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2);
        } else throw new IllegalStateException("Unsupported colour " + c);
        int a = Math.round(alpha * 255);
        return String.format("Color(0x%02X%s)", a, hex);
    }

    static float num(Element e, String attr, float def) {
        String v = e.getAttribute(attr);
        if (v.isEmpty()) return def;
        if (v.endsWith("%")) return Float.parseFloat(v.substring(0, v.length() - 1)) / 100f;
        return Float.parseFloat(v);
    }

    static String cap(String s) { return Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    static String f(float v) {
        if (v == Math.rint(v)) return (long) v + "f";
        String s = String.valueOf(v);
        return s + "f";
    }
}
