import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * URL shortener core: unique id -> scramble -> Base62 (7 chars).
 *
 *   java system-design/04_low_level_design/UrlShortener.java
 *
 * TRICKS:
 *  - BASE62 = [0-9a-zA-Z]; 62^7 = 3.5 trillion codes.
 *  - Encode: repeatedly take id % 62 as a digit, id /= 62, then reverse.
 *  - Sequential ids give look-alike codes -> scramble with (id * P) mod 62^7, where P is coprime with 62.
 *    That is a BIJECTION on [0, 62^7), so different ids can never collide.
 *    (It hides the order, but it is not a secret: use random pre-generated keys if codes must be unguessable.)
 *  - In production the counter is a range leased from ZooKeeper/DB per server (or a Snowflake id),
 *    and the map is a sharded key-value store with a cache in front.
 */
public class UrlShortener {

    static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    static final int CODE_LENGTH = 7;
    static final long SPACE = pow(62, CODE_LENGTH);      // 3,521,614,606,208
    static final BigInteger SCRAMBLE = BigInteger.valueOf(1_939_442_474_723L); // odd and not divisible by 31 -> coprime with 62

    private final AtomicLong counter = new AtomicLong(1);
    private final Map<String, String> codeToUrl = new ConcurrentHashMap<>();

    static long pow(long base, int exp) {
        long r = 1;
        for (int i = 0; i < exp; i++) r *= base;
        return r;
    }

    // (id * P) mod 62^7 with BigInteger so the multiplication can't overflow a long
    static long scramble(long id) {
        return BigInteger.valueOf(id).multiply(SCRAMBLE).mod(BigInteger.valueOf(SPACE)).longValue();
    }

    static String encodeBase62(long n) {
        StringBuilder sb = new StringBuilder();
        do {
            sb.append(ALPHABET.charAt((int) (n % 62)));
            n /= 62;
        } while (n > 0);
        while (sb.length() < CODE_LENGTH) sb.append('0'); // pad (reversed below, so zeros end up in front)
        return sb.reverse().toString();
    }

    static long decodeBase62(String s) {
        long n = 0;
        for (char c : s.toCharArray()) n = n * 62 + ALPHABET.indexOf(c);
        return n;
    }

    String shorten(String longUrl) {
        long id = counter.getAndIncrement();
        String code = encodeBase62(scramble(id));
        codeToUrl.put(code, longUrl);
        return code;
    }

    // custom alias: must be unique -> putIfAbsent is the in-memory version of a conditional insert
    boolean shortenWithAlias(String alias, String longUrl) {
        return codeToUrl.putIfAbsent(alias, longUrl) == null;
    }

    Optional<String> resolve(String code) {
        return Optional.ofNullable(codeToUrl.get(code));
    }

    public static void main(String[] args) {
        System.out.println("Base62 12345          -> " + encodeBase62(12345) + " -> " + decodeBase62(encodeBase62(12345)));
        System.out.println("code space (62^7)     -> " + SPACE);

        UrlShortener svc = new UrlShortener();
        String a = svc.shorten("https://example.com/articles/system-design-interview?utm_source=x");
        String b = svc.shorten("https://example.com/very/long/path/2");
        String c = svc.shorten("https://example.com/very/long/path/3");
        System.out.println("\nsequential ids 1,2,3  -> " + a + ", " + b + ", " + c + "   (consecutive ids, unrelated-looking codes)");
        System.out.println("resolve " + a + "       -> " + svc.resolve(a).orElse("404"));
        System.out.println("resolve zzzzzzz       -> " + svc.resolve("zzzzzzz").orElse("404"));
        System.out.println("alias 'sale' first    -> " + svc.shortenWithAlias("sale", "https://shop.example.com/summer"));
        System.out.println("alias 'sale' again    -> " + svc.shortenWithAlias("sale", "https://other.example.com") + " (409 conflict)");

        // uniqueness check over many ids
        java.util.Set<String> codes = new java.util.HashSet<>();
        for (long id = 1; id <= 200_000; id++) codes.add(encodeBase62(scramble(id)));
        System.out.println("\n200,000 ids -> " + codes.size() + " unique codes, all length " + CODE_LENGTH + ": "
                + codes.stream().allMatch(s -> s.length() == CODE_LENGTH));
    }
}
