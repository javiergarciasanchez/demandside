package firms;

import java.math.BigDecimal;
import java.util.Comparator;

public class CompareByPerceivedQ implements Comparator<Firm> {

	@Override
	public int compare(Firm f1, Firm f2) {

		if (f1.equals(f2))
			return 0;

		BigDecimal pQ1 = f1.getPerceivedQuality();
		BigDecimal pQ2 = f2.getPerceivedQuality();

		if (pQ1.compareTo(pQ2) == 0) {
			// Note that real quality should be different.
			return f1.getQuality().compareTo(f2.getQuality());
		}

		else
			return pQ1.compareTo(pQ2);
	}

}