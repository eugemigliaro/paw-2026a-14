import java.text.MessageFormat;
import java.math.BigDecimal;
import java.util.Locale;

public class TestLocale {
    public static void main(String[] args) {
        BigDecimal price = new BigDecimal("40000.00");
        
        Locale en = Locale.ENGLISH;
        Locale es = new Locale("es");
        Locale ar = new Locale("es", "AR");
        
        String patternCurrency = "{0,number,currency}";
        String patternHardcoded = "${0,number,#,##0.00}";
        
        System.out.println("Locale EN (en):");
        System.out.println("  Currency: " + MessageFormat.format(patternCurrency, price));
        System.out.println("  Hardcoded: " + MessageFormat.format(patternHardcoded, price));
        
        System.out.println("\nLocale ES (es):");
        System.out.println("  Currency: " + new MessageFormat(patternCurrency, es).format(new Object[]{price}));
        System.out.println("  Hardcoded: " + new MessageFormat(patternHardcoded, es).format(new Object[]{price}));

        System.out.println("\nLocale AR (es_AR):");
        System.out.println("  Currency: " + new MessageFormat(patternCurrency, ar).format(new Object[]{price}));
        System.out.println("  Hardcoded: " + new MessageFormat(patternHardcoded, ar).format(new Object[]{price}));
    }
}
