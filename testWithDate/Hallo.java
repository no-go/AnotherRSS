import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class Hallo {
	public static void main(String[] args) {
		Date zeitstempel = new Date();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.GERMAN);
		System.out.println("Jetzt: " + simpleDateFormat.format(zeitstempel));
		System.out.println("Eingabe: " + args[0]);
		
		try {
			zeitstempel = simpleDateFormat.parse("Do, 06 Jul 2017 12:05:48 +0200");
			//zeitstempel = simpleDateFormat.parse(args[0]);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
		}
		System.out.println("Eingabe reformatiert: " + simpleDateFormat.format(zeitstempel));
	}
}
