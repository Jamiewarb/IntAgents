package se.sics.tac.aw;
import java.util.Arrays;
import java.util.ArrayList;
import se.sics.tac.util.ArgEnumerator;

/* Can compute the best allocation for it based on what it knows is available and how much it costs
 * Can return information on the benefit of each part of its allocation
 * Will accept an allocation given to it, which it will remember
 * Can repeat its allocation to the master
 */

public class TACClient {
	public int inFlight, outFlight, hotel, e1, e2, e3;
	public ArrayList<Integer> allocHotels; // 1, 2, 3, 4 for Cheap, 11, 12, 13, 14 for Good
	public ArrayList<Integer> allocFlights; // iF, oF
	public ArrayList<Integer> allocEnt;
	public ArrayList<Integer> availableHotels;
	// [[1, 50.0], [2, 168.0], [3, 173.0], [4, 50.0], [11, 0.0], [12, 0.0], [13, 0.0], [14, 0.0]]
	public ArrayList<ArrayList<Object>> availableHotelsPrices;
	public ArrayList<ArrayList<Integer>> bestHotelAlloc;
	public ArrayList<Integer> setAllocation;

	public TACClient(int inFlight, int outFlight, int hotel, int e1, int e2, int e3) {
		this(inFlight, outFlight, hotel, e1, e2, e3, new ArrayList<Integer>(Arrays.asList(1,2,3,4,11,12,13,14)));
	}

	public TACClient(int inFlight, int outFlight, int hotel, int e1, int e2, int e3, 
				  				ArrayList<Integer> availableHotels) {
		this(inFlight, outFlight, hotel, e1, e2, e3, availableHotels, 50);
	}

	public TACClient(int inFlight, int outFlight, int hotel, int e1, int e2, int e3, 
				  				ArrayList<Integer> availableHotels, float startPrice) {
		this.inFlight = inFlight;
		this.outFlight = outFlight;
		this.hotel = hotel;
		this.e1 = e1; // Alligator Wrestling
		this.e2 = e2; // Amusement Park
		this.e3 = e3; // Museum
		this.availableHotels = availableHotels;
		this.availableHotelsPrices = new ArrayList<ArrayList<Object>>();
		for (int i = 0; i < this.availableHotels.size(); i++) {
			ArrayList<Object> hotelAndPrice = new ArrayList<Object>();
			hotelAndPrice.add(this.availableHotels.get(i));
			hotelAndPrice.add(startPrice);
			this.availableHotelsPrices.add(hotelAndPrice);
		}

		allocHotels = new ArrayList<Integer>();
		allocFlights = new ArrayList<Integer>();
		allocEnt = new ArrayList<Integer>();
		bestHotelAlloc = getBestAllocation();
	}

	public void setHotelPrice(int hotel, float price) {
		for (int i = 0; i < availableHotelsPrices.size(); i++) {
			ArrayList<Object> hp = availableHotelsPrices.get(i);
			if (hp.contains(hotel)) {
				hp.set(1, price);
				availableHotelsPrices.set(i, hp);
			}
		}
	}

	public float getAskPrice(int hotel, int type) {
		if (type == 1) {
			hotel = hotel + 10;
		}
		for (int i = 0; i < availableHotelsPrices.size(); i++) {
			if (availableHotelsPrices.get(i).contains(hotel)) {
				return (Float)availableHotelsPrices.get(i).get(1);
			}
		}
		return 0;
	}

	public void setAllocation(ArrayList<Integer> setAllocation) {
		this.setAllocation = setAllocation;
	}

	public ArrayList<ArrayList<Integer>> getBestAllocation() {
		// TODO Consider extending/reducing a night in either direction based on cost of flights (such as when it's 600) and available hotels
		// Need a hotel on all nights except outFlight
		boolean fullGoodAvailable = true;
		boolean fullCheapAvailable = true;
		int d;
		// There's only max 2 poss holiday lengths based on what's available (11121314, 1112-14, 11-1314, 11--14)
		ArrayList<Integer> goodHol1 = new ArrayList<Integer>();
		ArrayList<Integer> goodHol2 = new ArrayList<Integer>();
		ArrayList<Integer> goodHolFinal = new ArrayList<Integer>();
		boolean goodSplit = false; // Did it split in to two possible trips?
		int goodVal1; // Value of goodHol1 and 2
		int goodVal2;
		int goodValFinal;
		// There's only max 2 poss holiday lengths based on what's available (1234, 12-4, 1-34, 1--4)
		ArrayList<Integer> cheapHol1 = new ArrayList<Integer>();
		ArrayList<Integer> cheapHol2 = new ArrayList<Integer>();
		ArrayList<Integer> cheapHolFinal = new ArrayList<Integer>();
		boolean cheapSplit = false; // Did it split in to two possible trips?
		int cheapVal1; // Value of cheapHol1 and 2
		int cheapVal2;
		int cheapValFinal;

		int tPenalty;

		float estiPrice;

		// [[1, 50.0], [2, 168.0], [3, 173.0], [4, 50.0], [11, 0.0], [12, 0.0], [13, 0.0], [14, 0.0]]
		// availableHotelsPrices;

		for (d = inFlight; d < outFlight; d++) {
			if (!availableHotels.contains(d+10)) {
				fullGoodAvailable = false;
				break;
			} else {
				goodHol1.add(d+10);
			}
		}
		goodValFinal = -1; // If -1, there's no possible holiday in the good hotels
		if (fullGoodAvailable == false) {
			for (d = d; d < outFlight; d++) {
				if (availableHotels.contains(d+10)) {
					goodSplit = true;
					goodHol2.add(d+10);
				}
			}
			// Calculate if goodHol1 or goodHol2 is more profitable
			
			if (goodHol1.size() > 0) {
				tPenalty = getPenalty(inFlight, outFlight, goodHol1.get(0) - 10, goodHol1.get(goodHol1.size() - 1) - 10);
				estiPrice = getEstiPrice(goodHol1);
				goodVal1 = (1000 - tPenalty) - java.lang.Math.round(estiPrice);
				if (goodVal1 > goodValFinal) {
					goodValFinal = goodVal1;
					goodHolFinal = new ArrayList<Integer>(goodHol1);
				}
			}
			if (goodHol2.size() > 0) {
				tPenalty = getPenalty(inFlight, outFlight, goodHol2.get(0) - 10, goodHol2.get(goodHol2.size() - 1) - 10);
				estiPrice = getEstiPrice(goodHol1);
				goodVal2 = (1000 - tPenalty) - java.lang.Math.round(estiPrice);
				if (goodVal2 > goodValFinal) {
					goodValFinal = goodVal2;
					goodHolFinal = new ArrayList<Integer>(goodHol2);
				}
			}
		} else {
			tPenalty = getPenalty(inFlight, outFlight, goodHol1.get(0) - 10, goodHol1.get(goodHol1.size() - 1) - 10);
			estiPrice = getEstiPrice(goodHol1);
			goodVal1 = (1000 - tPenalty) - java.lang.Math.round(estiPrice);
			if (goodVal1 > goodValFinal) {
				goodValFinal = goodVal1;
				goodHolFinal = new ArrayList<Integer>(goodHol1);
			}
		}
		if (goodValFinal != -1) {
			goodValFinal = goodValFinal + this.hotel;
		}

		for (d = inFlight; d < outFlight; d++) {
			if (!availableHotels.contains(d)) {
				fullCheapAvailable = false;
				break;
			} else {
				cheapHol1.add(d);
			}
		}
		cheapValFinal = -1; // If -1, there's no possible holiday in the cheap hotels
		if (fullCheapAvailable == false) {
			for (d = d; d < outFlight; d++) {
				if (availableHotels.contains(d)) {
					cheapSplit = true;
					cheapHol2.add(d);
				}
			}
			// Calculate if cheapHol1 or cheapHol2 is more profitable
			if (cheapHol1.size() > 0) {
				tPenalty = getPenalty(inFlight, outFlight, cheapHol1.get(0), cheapHol1.get(cheapHol1.size() - 1));
				estiPrice = getEstiPrice(cheapHol1);
				cheapVal1 = (1000 - tPenalty) - java.lang.Math.round(estiPrice);
				if (cheapVal1 > cheapValFinal) {
					cheapValFinal = cheapVal1;
					cheapHolFinal = new ArrayList<Integer>(cheapHol1);
				}
			}
			if (cheapHol2.size() > 0) {
				tPenalty = getPenalty(inFlight, outFlight, cheapHol2.get(0), cheapHol2.get(cheapHol2.size() - 1));
				estiPrice = getEstiPrice(cheapHol2);
				cheapVal2 = (1000 - tPenalty) - java.lang.Math.round(estiPrice);
				if (cheapVal2 > cheapValFinal) {
					cheapValFinal = cheapVal2;
					cheapHolFinal = new ArrayList<Integer>(cheapHol2);
				}
			}
		} else {
			tPenalty = getPenalty(inFlight, outFlight, cheapHol1.get(0), cheapHol1.get(cheapHol1.size() - 1));
			estiPrice = getEstiPrice(cheapHol1);
			cheapVal1 = (1000 - tPenalty) - java.lang.Math.round(estiPrice);
			if (cheapVal1 > cheapValFinal) {
				cheapValFinal = cheapVal1;
				cheapHolFinal = new ArrayList<Integer>(cheapHol1);
			}
		}
		// Calculate the final stuff and return the chosen holiday
		ArrayList<ArrayList<Integer>> chosenAlloc = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> chosenHotels;
		ArrayList<Integer> chosenVal;
		if (cheapValFinal == -1 && goodValFinal == -1) {
			chosenHotels = new ArrayList<Integer>(Arrays.asList(-1));
			chosenVal = new ArrayList<Integer>(Arrays.asList(-1));
		}
		if (cheapValFinal >= goodValFinal) {
			chosenHotels = new ArrayList<Integer>(cheapHolFinal);
			chosenVal = new ArrayList<Integer>(Arrays.asList(cheapValFinal));
		} else {
			chosenHotels = new ArrayList<Integer>(goodHolFinal);
			chosenVal = new ArrayList<Integer>(Arrays.asList(goodValFinal));
		}
		chosenAlloc.add(chosenHotels);
		chosenAlloc.add(chosenVal);
		// ArrayList<ArrayList<Integer>> chosenAlloc = [[1,2,3],[utility]] 
		// where utility is what the hotels earn, cheap hotels 1, 2, good hotels 11, 12 etc
		return chosenAlloc;
	}

	public float getEstiPrice(ArrayList<Integer> holList) {
		float estiPrice = 0f;
		for (int p = 0; p < holList.size(); p++) {
			for (int q = 0; q < availableHotelsPrices.size(); q++) {
				if (availableHotelsPrices.get(q).get(0) == holList.get(p)) {
					estiPrice = estiPrice + (Float)availableHotelsPrices.get(q).get(1);
				}
			}
		}
		return estiPrice;
	}

	public int getPenalty(int inFlight, int outFlight, int firstHotel, int lastHotel) {
		int penalty = 200 * (
				java.lang.Math.abs(firstHotel - inFlight) 
				+ java.lang.Math.abs(lastHotel - (outFlight-1))
				);
		return penalty;
	}

	public void hotelAvailable(int day, int type) {
		// type is 0 for cheap, 1 for good
		if (type == 1) {
			day = day + 10;
		}
		this.hotelAvailable(day);
	}
	public void hotelAvailable(int day) {
		// day is 1, 2, 3, 4 for cheap or 11, 12, 13, 14 for good
		if (!availableHotels.contains(day)) 
			availableHotels.add(availableHotels.indexOf(day));
	}
	public void hotelNotAvailable(int day, int type) {
		// type is 0 for cheap, 1 for good
		if (type == 1) {
			day = day + 10;
		}
		this.hotelNotAvailable(day);
	}
	public void hotelNotAvailable(int day) {
		// day is 1, 2, 3, 4 for cheap or 11, 12, 13, 14 for good
		if (availableHotels.contains(day)) 
			availableHotels.remove(availableHotels.indexOf(day));
	}

	public void setAvailableHotels(ArrayList<Integer> hotels) {
		availableHotels = new ArrayList<Integer>(hotels);
	}

	public void setAvailableHotelsLong(ArrayList<ArrayList<Integer>> hotels) {
		ArrayList<Integer> hotelList = new ArrayList<Integer>();
		for (int i = 0; i < hotels.size(); i++) {
			hotelList.add((hotels.get(i)).get(0));
		}
		availableHotels = new ArrayList<Integer>(hotelList);
	}

	public int getMaxUtility() {
		return 1000 + hotel + e1 + e2 + e3;
	}
}