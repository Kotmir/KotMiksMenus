package tech.kotmir.kotmiksmenus.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** MiniMessage-утилиты и форматирование чисел. */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private Text() {
    }

    /** Компонент для названия/лора предмета (курсив выключен по умолчанию). */
    public static Component item(String mini) {
        return MM.deserialize(mini == null ? "" : mini)
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static List<Component> items(List<String> lines) {
        return lines.stream().map(Text::item).collect(Collectors.toList());
    }

    /** Компонент для чата / заголовка инвентаря. */
    public static Component chat(String mini) {
        return MM.deserialize(mini == null ? "" : mini);
    }

    public static String plain(String mini) {
        return PlainTextComponentSerializer.plainText().serialize(chat(mini));
    }

    public static String legacy(String mini) {
        return LEGACY.serialize(chat(mini));
    }

    public static String apply(String s, Map<String, String> placeholders) {
        if (s == null) {
            return "";
        }
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            s = s.replace("{" + e.getKey() + "}", e.getValue());
        }
        return s;
    }

    public static Map<String, String> map(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    /** 150000 -> "150.000". Дробные значения - с двумя знаками. */
    public static String num(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return group((long) value);
        }
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    public static String group(long value) {
        String s = Long.toString(Math.abs(value));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) {
                sb.append('.');
            }
            sb.append(s.charAt(i));
        }
        return (value < 0 ? "-" : "") + sb;
    }

    /** Русское склонение: plural(5, "маяк", "маяка", "маяков"). */
    public static String plural(long n, String one, String few, String many) {
        long a = Math.abs(n) % 100;
        long b = a % 10;
        if (a > 10 && a < 20) {
            return many;
        }
        if (b > 1 && b < 5) {
            return few;
        }
        if (b == 1) {
            return one;
        }
        return many;
    }

    public static String plural(long n, List<String> forms) {
        if (forms == null || forms.size() < 3) {
            return "";
        }
        return plural(n, forms.get(0), forms.get(1), forms.get(2));
    }

    public static String duration(long millis) {
        long totalSec = Math.max(0, millis / 1000);
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        if (h > 0) {
            return h + "ч " + m + "м";
        }
        if (m > 0) {
            return m + "м " + s + "с";
        }
        return s + "с";
    }
}
