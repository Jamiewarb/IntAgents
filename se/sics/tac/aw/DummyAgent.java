/**
 * TODO 27/10
 * ~~~~~~~~~~~~~~
 * Make Clients aware of the costs of their choice packages. Pick best cost wise solution
 ** Changing dates doubles cost, but using owned, unallocated goods is free
 ** Make Clients choose entertainment themselves - base it on the current market cost
 ** Keep entertainment as a per client thing - record which client OWNS which piece of entertainment
 ** If an entertainment increases in bid price above what the client will pay, sell that shit
 * Consider modeling if a holiday package was extended or retracted 1 day in each possible direction
 ** Will this make the package increase or decrease in price
 ** Work from the ideal package, until we find the most cost effective for current costs
 * If you implement the above point, consider, also remodel entertainment with these extra days in mind
 ** It shouldn't really change much though, unless we already own some entertainment potentially
 *
 *
 * TAC AgentWare
 * http://www.sics.se/tac        tac-dev@sics.se
 *
 * Copyright (c) 2001-2005 SICS AB. All rights reserved.
 *
 * SICS grants you the right to use, modify, and redistribute this
 * software for noncommercial purposes, on the conditions that you:
 * (1) retain the original headers, including the copyright notice and
 * this text, (2) clearly document the difference between any derived
 * software and the original, and (3) acknowledge your use of this
 * software in pertaining publications and reports.  SICS provides
 * this software "as is", without any warranty of any kind.  IN NO
 * EVENT SHALL SICS BE LIABLE FOR ANY DIRECT, SPECIAL OR INDIRECT,
 * PUNITIVE, INCIDENTAL OR CONSEQUENTIAL LOSSES OR DAMAGES ARISING OUT
 * OF THE USE OF THE SOFTWARE.
 *
 * -----------------------------------------------------------------
 *
 * Author  : Joakim Eriksson, Niclas Finne, Sverker Janson
 * Created : 23 April, 2002
 * Updated : $Date: 2005/06/07 19:06:16 $
 *	     $Revision: 1.1 $
 * ---------------------------------------------------------
 * DummyAgent is a simplest possible agent for TAC. It uses
 * the TACAgent agent ware to interact with the TAC server.
 *
 * Important methods in TACAgent:
 *
 * Retrieving information about the current Game
 * ---------------------------------------------
 * int getGameID()
 *  - returns the id of current game or -1 if no game is currently plaing
 *
 * getServerTime()
 *  - returns the current server time in milliseconds
 *
 * getGameTime()
 *  - returns the time from start of game in milliseconds
 *
 * getGameTimeLeft()
 *  - returns the time left in the game in milliseconds
 *
 * getGameLength()
 *  - returns the game length in milliseconds
 *
 * int getAuctionNo()
 *  - returns the number of auctions in TAC
 *
 * int getClientPreference(int client, int type)
 *  - returns the clients preference for the specified type
 *   (types are TACAgent.{ARRIVAL, DEPARTURE, HOTEL_VALUE, E1, E2, E3}
 *
 * int getAuctionFor(int category, int type, int day)
 *  - returns the auction-id for the requested resource
 *   (categories are TACAgent.{CAT_FLIGHT, CAT_HOTEL, CAT_ENTERTAINMENT
 *    and types are TACAgent.TYPE_INFLIGHT, TACAgent.TYPE_OUTFLIGHT, etc)
 *
 * int getAuctionCategory(int auction)
 *  - returns the category for this auction (CAT_FLIGHT, CAT_HOTEL,
 *    CAT_ENTERTAINMENT)
 *
 * int getAuctionDay(int auction)
 *  - returns the day for this auction.
 *
 * int getAuctionType(int auction)
 *  - returns the type for this auction (TYPE_INFLIGHT, TYPE_OUTFLIGHT, etc).
 *
 * int getOwn(int auction)
 *  - returns the number of items that the agent own for this
 *    auction
 *
 * Submitting Bids
 * ---------------------------------------------
 * void submitBid(Bid)
 *  - submits a bid to the tac server
 *
 * void replaceBid(OldBid, Bid)
 *  - replaces the old bid (the current active bid) in the tac server
 *
 *   Bids have the following important methods:
 *    - create a bid with new Bid(AuctionID)
 *
 *   void addBidPoint(int quantity, float price)
 *    - adds a bid point in the bid
 *
 * Help methods for remembering what to buy for each auction:
 * ----------------------------------------------------------
 * int getAllocation(int auctionID)
 *   - returns the allocation set for this auction
 * void setAllocation(int auctionID, int quantity)
 *   - set the allocation for this auction
 *
 *
 * Callbacks from the TACAgent (caused via interaction with server)
 *
 * bidUpdated(Bid bid)
 *  - there are TACAgent have received an answer on a bid query/submission
 *   (new information about the bid is available)
 * bidRejected(Bid bid)
 *  - the bid has been rejected (reason is bid.getRejectReason())
 * bidError(Bid bid, int error)
 *  - the bid contained errors (error represent error status - commandStatus)
 *
 * quoteUpdated(Quote quote)
 *  - new information about the quotes on the auction (quote.getAuction())
 *    has arrived
 * quoteUpdated(int category)
 *  - new information about the quotes on all auctions for the auction
 *    category has arrived (quotes for a specific type of auctions are
 *    often requested at once).
 *
 * auctionClosed(int auction)
 *  - the auction with id "auction" has closed
 *
 * transaction(Transaction transaction)
 *  - there has been a transaction
 *
 * gameStarted()
 *  - a TAC game has started, and all information about the
 *    game is available (preferences etc).
 *
 * gameStopped()
 *  - the current game has ended
 *
 */

package se.sics.tac.aw;
import se.sics.tac.util.ArgEnumerator;
import java.util.logging.*;
import java.util.Arrays;
import java.util.ArrayList;

public class DummyAgent extends AgentImpl {

	private static final Logger log =
		Logger.getLogger(DummyAgent.class.getName());

	private static final boolean DEBUG = false;

	private float[] prices;

	private float initialBid = 50;

	protected void init(ArgEnumerator args) {
		prices = new float[agent.getAuctionNo()];
	}

	public ArrayList<Integer> availableHotels;

	private ArrayList<TACClient> clientList;

	public ArrayList<ArrayList<Object>> availableEntPrices; // [[16, 100.1, 80.2], [17, 121.23, 50.664]]

	public void quoteUpdated(Quote quote) {
		// TODO Reset allocation based on if we failed to get some hotels
		int auction = quote.getAuction();
		int auctionCategory = agent.getAuctionCategory(auction);

		if (auctionCategory == TACAgent.CAT_FLIGHT) {
			if (isWitchingHour()) {
				// TODO only allocate if we got the right hotels, otherwise don't buy for that package
				// TODO check if we changed allocations because we didn't get right hotels, then update flights accordingly
				int alloc = agent.getAllocation(auction) - agent.getOwn(auction);
				float price = -1f;
				if (alloc > 0) {
					price = 1000;
				}
				if (price > 0) {
					Bid bid = new Bid(auction);
					bid.addBidPoint(alloc, price);
					if (DEBUG) {
						log.finest("submitting bid with alloc=" + agent.getAllocation(auction)
								 + " own=" + agent.getOwn(auction));
					}
					agent.submitBid(bid);
				}
			}

		} else if (auctionCategory == TACAgent.CAT_HOTEL) {
			// Update the hotel prices for all the agents
			for (int i = 0; i < clientList.size(); i++) {
				TACClient client = clientList.get(i);
				float price = quote.getAskPrice();
				int day = agent.getAuctionDay(auction);
				if (agent.getAuctionType(auction) == TACAgent.TYPE_GOOD_HOTEL) day = day + 10;
				client.setHotelPrice(day, price);
			}

			// TODO Do something about reclaculating quotes based on the estimated price of the package (just ask price?)

		} else if (auctionCategory == TACAgent.CAT_ENTERTAINMENT) {
			// Set all the prices
			boolean priceSet = false;
			for (int i = 0; i < availableEntPrices.size(); i++) {
				ArrayList<Object> ep = availableEntPrices.get(i);
				if (ep.contains(auction)) {
					ep.set(1, quote.getAskPrice());
					ep.set(2, quote.getBidPrice());
					availableEntPrices.set(i, ep);
					priceSet = true;
				}
			}
			if (!priceSet) {
				ArrayList<Object> ep = new ArrayList<Object>(Arrays.asList(auction, quote.getAskPrice(), quote.getBidPrice()));
				availableEntPrices.add(ep);
				priceSet = true;
			}

			// Create temporary List of possible entertainment (those owned > 0 or that have an ask price)
			// Ask client's for most profitable spread of entertainment
			// 		Take cost (bid price) on each day in to account. Owned entertainment is free. Assign greedily
			// 		Return allocation wanted on each day, with max price (value of e1)
			// Send this list back to the Client, to set it as their claimed entertainment, with (e1) prices
			// 
			// Update all of these required entertainments to current Bid Price + 5
			//
			// When last Hotel auction closes, ask all Clients for their ent allocations
			// For each item that we don't own, set to their max prices now
			// For each entertainment left that's unallocated, sell for current Bid Price if any
			// For each owned entertainment where current BidPrice > e1 val, list for sale at Bid Price


			int alloc = agent.getAllocation(auction) - agent.getOwn(auction);
			if (alloc != 0) {
				Bid bid = new Bid(auction);
				if (alloc < 0)
					if (!isWitchingHour())
						prices[auction] = 200f - (agent.getGameTime() * 120f) / 720000;
					else {
						if (quote.getBidPrice() > 85 || quote.getBidPrice() == 0.0)
							prices[auction] = 85;
					}
				else
					prices[auction] = 50f + (agent.getGameTime() * 100f) / 720000;
				bid.addBidPoint(alloc, prices[auction]);
				if (DEBUG) {
					log.finest("submitting bid with alloc="
							 + agent.getAllocation(auction)
							 + " own=" + agent.getOwn(auction));
				}
				agent.submitBid(bid);
			}
		}
	}

	public void quoteUpdated(int auctionCategory) {
		log.fine("All quotes for "
			 + agent.auctionCategoryToString(auctionCategory)
			 + " has been updated");
	}

	public void bidUpdated(Bid bid) {
		/*log.fine("Bid Updated: id=" + bid.getID() + " auction="
			 + bid.getAuction() + " state="
			 + bid.getProcessingStateAsString());
		log.fine("       Hash: " + bid.getBidHash());*/
	}

	public void bidRejected(Bid bid) {
		log.warning("Bid Rejected: " + bid.getID());
		log.warning("      Reason: " + bid.getRejectReason()
		+ " (" + bid.getRejectReasonAsString() + ')');
	}

	public void bidError(Bid bid, int status) {
		log.warning("Bid Error in auction " + bid.getAuction() + ": " + status
		+ " (" + agent.commandStatusToString(status) + ')');
	}

	public void gameStarted() {
		log.fine("Game " + agent.getGameID() + " started!");

		clientList = new ArrayList<TACClient>();
		availableHotels = new ArrayList<Integer>(Arrays.asList(1,2,3,4,11,12,13,14));
		availableEntPrices = new ArrayList<ArrayList<Object>>();
		calculateAllocation(); // Sets up clientList & bestAllocations per client
		sendInitialBids(); // Sends initial hotel bids of 50 per hotel, based on initial allocations
	}

	public boolean isWitchingHour() {
		// TODO Implement logic to include network latency here plus time it takes to complete computation cycle twice, if any
		if (agent.getGameTimeLeft() < 15000) return true;
		else return false;
	}

	public void gameStopped() {
		log.fine("Game Stopped!");
	}

	public void clearHotelAllocations() {
		int auction;
		for (int d = 1; d < 5; d++) {
			auction = agent.getAuctionFor(TACAgent.CAT_HOTEL, TACAgent.TYPE_GOOD_HOTEL, d);
			agent.setAllocation(auction, 0);
			auction = agent.getAuctionFor(TACAgent.CAT_HOTEL, TACAgent.TYPE_CHEAP_HOTEL, d);
			agent.setAllocation(auction, 0);
		}
	}

	public void clearFlightAllocations() {
		int auction;
		// Clear inFlight 1
		auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_INFLIGHT, 1);
		agent.setAllocation(auction, 0);
		for (int d = 2; d < 5; d++) {
			auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_INFLIGHT, d);
			agent.setAllocation(auction, 0);
			auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_OUTFLIGHT, d);
			agent.setAllocation(auction, 0);
		}
		auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_OUTFLIGHT, 5);
		agent.setAllocation(auction, 0);
		// Clear outFlight 5
	}

	public void auctionClosed(int auction) {
		log.fine("*** Auction " + auction + " closed!");
		// TODO Logic to see if we got all the hotels we allocated here. If not we need to rejig some customers hotel allocations
		// including flights and entertainment, based on what is still available (if anything)
		// TODO Logic to see which hotel closed, and if the flights are less than 180 buy them now (+ on flight quote update)
		switch(agent.getAuctionCategory(auction)) {
			case TACAgent.CAT_HOTEL:
				int day = agent.getAuctionDay(auction);
				int type = agent.getAuctionType(auction);
				if (type == TACAgent.TYPE_GOOD_HOTEL) type = 1;
				else type = 0;
				// We only remove the hotel permanently if there's no possible way for us to ever get it
				if (agent.getOwn(auction) <= 0) {
					removeHotelAllClients(day, type);
					hotelNotAvailable(day, type);
				}
				// Do the Client recalculation based on what we own and what's still available - ignore price for now
				clearHotelAllocations();
				clearFlightAllocations();
				ArrayList<ArrayList<Integer>> availHotels = new ArrayList<ArrayList<Integer>>();
				int auctionCycle;
				int d;
				for (int h = 0; h < availableHotels.size(); h++) {
					ArrayList<Integer> hotel = new ArrayList<Integer>();
					int hNo = availableHotels.get(h);
					//type = TACAgent.TYPE_GOOD_HOTEL;
					//type = TACAgent.TYPE_CHEAP_HOTEL;
					if (hNo > 10) {
						type = TACAgent.TYPE_GOOD_HOTEL;
						d = hNo - 10;
					} else {
						type = TACAgent.TYPE_CHEAP_HOTEL;
						d = hNo;
					}
					auctionCycle = agent.getAuctionFor(TACAgent.CAT_HOTEL, type, d);
					int own = agent.getOwn(auctionCycle);
					if (own <= 0) {
						own = 16;
					} else {
						for (int i = 0; i < clientList.size(); i++) {
							TACClient client = clientList.get(i);
							client.setHotelPrice(hNo, 0f);
						}
					}
					hotel.add(hNo);
					hotel.add(own);
					availHotels.add(hotel); // [[1,16],[2,16]]
				}
				TACClient client;
				ArrayList<ArrayList<ArrayList<Integer>>> choices;
				ArrayList<TACClient> clientsNotAllocated = new ArrayList<TACClient>();
				clientsNotAllocated = new ArrayList<TACClient>(clientList);
				int loopGuard = 0;
				// Get all client's best allocations, and choose most profitable, then update hAvail and repeat until all clients done
				while (clientsNotAllocated.size() > 0 && loopGuard < 1000) { // Add LoopGuard in case of error
					choices = new ArrayList<ArrayList<ArrayList<Integer>>>();
					for (int c = 0; c < clientsNotAllocated.size(); c++) {
						client = clientsNotAllocated.get(c);
						client.setAvailableHotelsLong(availHotels);
						choices.add(client.getBestAllocation()); // [[[1,2,3],[800]],[1,2],[1000]]
					}
					int highestValue = 0;
					TACClient currentClient = clientsNotAllocated.get(0);
					ArrayList<Integer> bestAllocation = (choices.get(0)).get(0);
					// Choose the client with the highest value
					for (int i = 0; i < choices.size(); i++) {
						int choiceVal = ((choices.get(i)).get(1)).get(0);
						if (choiceVal > highestValue) {
							highestValue = choiceVal; // 800
							currentClient = clientsNotAllocated.get(i);
							bestAllocation = (choices.get(i)).get(0); //[1,2,3]
						}
					}
					// Now currentClient is the next client to set allocation for
					// And highestValue is the amount they will bring us (hopefully)
					// Set the Client's new allocation
					currentClient.setAllocation(bestAllocation);
					// Need to remove 1 from all own < 16 hotels in availHotels /
					// If own drops to 0, need to remove it from available hotels of the rest /
					for (int av = 0; av < availHotels.size(); av++) {
						int hNo = (availHotels.get(av)).get(0);
						int hOwn = (availHotels.get(av)).get(1);
						if (bestAllocation.contains(hNo)) {
							hOwn = hOwn - 1;
							if (hOwn > 0)
								availHotels.get(av).set(1, hOwn);
							else
								availHotels.remove(av);
						}
					}
					// Need to set allocation in the agent
					if (bestAllocation.size() > 0) {
						int be;
						int f = bestAllocation.get(0);
						int auctionNew;
						for (be = 0; be < bestAllocation.size(); be++) {
							f = bestAllocation.get(be);
							int ntype;
							if (f > 10) {
								ntype = TACAgent.TYPE_GOOD_HOTEL;
								f = f - 10;
							} else {
								ntype = TACAgent.TYPE_CHEAP_HOTEL;
							}
							auctionNew = agent.getAuctionFor(TACAgent.CAT_HOTEL, ntype, f);
							agent.setAllocation(auctionNew, agent.getAllocation(auctionNew) + 1);
							if (be == 0) {
								// Book inbound flight on f
								auctionNew = agent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_INFLIGHT, f);
								agent.setAllocation(auctionNew, agent.getAllocation(auctionNew) + 1);
							}
						}
						// Book f+1 outbound flight
						f = f + 1;
						auctionNew = agent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_OUTFLIGHT, f);
						agent.setAllocation(auctionNew, agent.getAllocation(auctionNew) + 1);
					} else {
						// This Client can't find a suitable allocation at all, so do nothing
					}

					clientsNotAllocated.remove(clientsNotAllocated.indexOf(currentClient));
					loopGuard++;
				}
				if (loopGuard >= 1000) {
					// We had a problem somehow...
				}
				sendBids();
				// TODO Actually update the bids on the hotels and flights... we just have the allocations
				// TODO take in to account entertainment costs and fun bonuses, and flight costs
				// when deciding who to allocate owned hotels to and who to change, for optimum allocation
			break;
		}
	}

	private void sendInitialBids() {
		for (int i = 0, n = agent.getAuctionNo(); i < n; i++) {
			int alloc = agent.getAllocation(i) - agent.getOwn(i);
			float price = -1f;
			switch (agent.getAuctionCategory(i)) {
			/*case TACAgent.CAT_FLIGHT:
				if (alloc > 0) {
					price = 1000;
				}
				break;*/
				case TACAgent.CAT_HOTEL:
					if (alloc > 0) {
						price = initialBid;
						prices[i] = initialBid;
					}
				break;
				default:
				break;
			}
			if (price > 0) {
				Bid bid = new Bid(i);
				bid.addBidPoint(alloc, price);
				if (DEBUG) {
					log.fine("submitting bid with alloc=" + agent.getAllocation(i)
							 + " own=" + agent.getOwn(i));
				}
				agent.submitBid(bid);
			}
		}
	}
	private void sendBids() {
		for (int i = 0, n = agent.getAuctionNo(); i < n; i++) {
			int alloc = agent.getAllocation(i) - agent.getOwn(i);
			float price = -1f;
			switch (agent.getAuctionCategory(i)) {
			/*case TACAgent.CAT_FLIGHT:
				if (alloc > 0) {
					price = 1000;
				}
				break;*/
				case TACAgent.CAT_HOTEL:
					if (alloc > 0 ) {
						int d = agent.getAuctionDay(i);
						int type = agent.getAuctionType(i);
						if (type == TACAgent.TYPE_GOOD_HOTEL) type = 1;
						else type = 0;
						float askPrice = clientList.get(0).getAskPrice(d, type);
						if (!isWitchingHour())
							prices[i] = askPrice + 50;
						else {
							// TODO Set this to the utility of entire package minus current flights cost and unowned entertainment cost
							prices[i] = askPrice + 205;
						}
						Bid bid = new Bid(i);
						
						bid.addBidPoint(alloc, prices[i]);
						if (DEBUG) {
							log.finest("submitting bid with alloc="
									 + agent.getAllocation(i)
									 + " own=" + agent.getOwn(i));
						}
						agent.submitBid(bid);
					}
				break;
			/*case TACAgent.CAT_ENTERTAINMENT:
				if (alloc < 0) {
					price = 200;
					prices[i] = 200f;
				} else if (alloc > 0) {
					price = 50;
					prices[i] = 50f;
				}
				break;*/
				default:
				break;
			}
			if (price > 0) {
				Bid bid = new Bid(i);
				bid.addBidPoint(alloc, price);
				if (DEBUG) {
					log.finest("submitting bid with alloc=" + agent.getAllocation(i)
							 + " own=" + agent.getOwn(i));
				}
				agent.submitBid(bid);
			}
		}
	}

	private void removeHotelAllClients(int day, int type) {
		TACClient client;
		for (int i = 0; i < clientList.size(); i++) {
			client = clientList.get(i);
			client.hotelNotAvailable(day, type);
		}
	}

	private void calculateAllocation() {
		// Testing the Client.java code
		for (int i = 0; i < 8; i++) {
			int inFlight = agent.getClientPreference(i, TACAgent.ARRIVAL);
			int outFlight = agent.getClientPreference(i, TACAgent.DEPARTURE);
			int hotel = agent.getClientPreference(i, TACAgent.HOTEL_VALUE);
			int e1 = agent.getClientPreference(i, TACAgent.E1);
			int e2 = agent.getClientPreference(i, TACAgent.E2);
			int e3 = agent.getClientPreference(i, TACAgent.E3);

			clientList.add(new TACClient(inFlight, outFlight, hotel, e1, e2, e3));
		}


		for (int i = 0; i < 8; i++) {
			int inFlight = agent.getClientPreference(i, TACAgent.ARRIVAL);
			int outFlight = agent.getClientPreference(i, TACAgent.DEPARTURE);
			int hotel = agent.getClientPreference(i, TACAgent.HOTEL_VALUE);
			int type;
			int auction;

			// Get the flight preferences auction and remember that we are
			// going to buy tickets for these days. (inflight=1, outflight=0)
			auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT,
					TACAgent.TYPE_INFLIGHT, inFlight);
			agent.setAllocation(auction, agent.getAllocation(auction) + 1);
			auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT,
						TACAgent.TYPE_OUTFLIGHT, outFlight);
			agent.setAllocation(auction, agent.getAllocation(auction) + 1);

			// TODO implement logic to see if we have any expensive or cheap hotels already, length of stay and find out utility
			if (hotel > 100) {
				type = TACAgent.TYPE_GOOD_HOTEL;
			} else {
				type = TACAgent.TYPE_CHEAP_HOTEL;
			}
			int d;
			// allocate a hotel night for each day that the agent stays
			for (d = inFlight; d < outFlight; d++) {
				auction = agent.getAuctionFor(TACAgent.CAT_HOTEL, type, d);
				log.finer("Adding hotel for day: " + d + " on " + auction);
				agent.setAllocation(auction, agent.getAllocation(auction) + 1);
			}

			// Allocate entertainment in order of preference, giving priority to already owned entertainment
			// TODO Calculate whether using pre-owned but not highest gain is better than buying one more preferred (probably in updateQuote)
			// TODO Calculate cheapest allocation of entertainment based on current market prices (probably in updateQuote)
			// TODO Calculate entertainment in optimum algorithm, rather than greedy assignment, via a global scale
			// TODO Calculate whether buying a piece of entertainment gives enough utility to make it worth it
			int[] entPref = getEntPref(i);
			int[][] entOwned = new int[3][outFlight - inFlight];
			int[] entAlloc = new int[outFlight - inFlight];
			boolean e1Assigned = false;
			boolean e2Assigned = false;
			boolean e3Assigned = false;
			// Go through each day from iF to oF
			for (d = inFlight; d < outFlight; d++) {
				int a = d - inFlight;
				// Check what days we have entertainment owned
				for (int e = 0; e < 3; e++) {
					if (entPref[e] > -1) {
						auction = agent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT, entPref[e], d);
						if (agent.getAllocation(auction) < agent.getOwn(auction)) {
							entOwned[e][a] = auction; // Fill entOwned with the auctions where we own some entertainment for this client
							//entOwned[][] = [[0,x,x],[0,x,x],[x,0,x]]
						}
					}
				}
				// If only e1 entOwned > 0 for this day
				if (entOwned[0][a] > 0 && entOwned[1][a] == 0 && entOwned[2][a] == 0) {
					// And we haven't already assigned this type of entertainment
					if (e1Assigned == false) {
						// Assign it
						entAlloc[a] = entOwned[0][a];
						e1Assigned = true;
					}
				// If only e2 entOwned > 0 for this day
				} else if (entOwned[1][a] > 0 && entOwned[0][a] == 0 && entOwned[2][a] == 0) {
					if (e2Assigned == false) {
						entAlloc[a] = entOwned[1][a];
						e2Assigned = true;
					}
				// If only e3 entOwned > 0 for this day
				} else if (entOwned[2][a] > 0 && entOwned[0][a] == 0 && entOwned[1][a] == 0) {
					if (e3Assigned == false) {
						entAlloc[a] = entOwned[2][a];
						e3Assigned = true;
					}
				// If e1 > 0 and also e2 or e3
				} else if (entOwned[0][a] > 0 && (entOwned[1][a] > 0 || entOwned[2][a] > 0)) {
					// And we haven't assigned e1
					if (e1Assigned == false) {
						// Assign e1
						entAlloc[a] = entOwned[0][a];
						e1Assigned = true;
					// Otherwise, if e2 was set and not assigned
					} else if (entOwned[1][a] > 0 && e2Assigned == false) {
						// Assign e2
						entAlloc[a] = entOwned[1][a];
						e2Assigned = true;
					// Otherwise check if e3 is set and not assigned
					} else if (entOwned[2][a] > 0 && e3Assigned == false) {
						entAlloc[a] = entOwned[2][a];
						e3Assigned = true;
					}
				// And repeat for in case e1 was not > 0
				} else if (entOwned[1][a] > 0 && entOwned[2][a] > 0) {
					if (e2Assigned == false) {
						entAlloc[a] = entOwned[1][a];
						e2Assigned = true;
					} else if (e3Assigned == false) {
						entAlloc[a] = entOwned[2][a];
						e3Assigned = true;
					}
				// And repeat for in case e1 and e2 was not > 0
				} else if (entOwned[2][a] > 0) {
					if (e3Assigned == false) {
						entAlloc[a] = entOwned[2][a];
						e3Assigned = true;
					}
				}
			}

			for (d = 0; d < entAlloc.length; d++) {
				if (entAlloc[d] == 0) {
					int nextType = -1;
					if (e1Assigned == false) {
						nextType = entPref[0];
						e1Assigned = true;
					} else if (e2Assigned == false) {
						nextType = entPref[1];
						e2Assigned = true;
					} else if (e3Assigned == false) {
						nextType = entPref[2];
						e3Assigned = true;
					}
					if (nextType != -1) {
						entAlloc[d] = agent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT, nextType, d+inFlight);
					}
				}
				if (entAlloc[d] > 0) {
					agent.setAllocation(entAlloc[d], agent.getAllocation(entAlloc[d]) + 1);
				}
			}


			
			/*int eType = -1;
      		while((eType = nextEntType(i, eType)) > 0) {
				auction = bestEntDay(inFlight, outFlight, eType);
				log.finer("Adding entertainment " + eType + " on " + auction);
				agent.setAllocation(auction, agent.getAllocation(auction) + 1);
     		 }*/

		}
	}

	private int[] getEntPref(int client) {
		int[] entPref = new int[3];

		int e1 = agent.getClientPreference(client, TACAgent.E1);
		int e2 = agent.getClientPreference(client, TACAgent.E2);
		int e3 = agent.getClientPreference(client, TACAgent.E3);
		int t1 = TACAgent.TYPE_ALLIGATOR_WRESTLING;
		int t2 = TACAgent.TYPE_AMUSEMENT;
		int t3 = TACAgent.TYPE_MUSEUM;
		int limit = 45;
		

		if (e1 > e2 && e1 > e3) {
			entPref[0] = t1;
			// Don't limit the best option, but limit the other two
			if (e2 < limit) {
				t2 = -1;
			}
			if (e3 < limit) {
				t3 = -1;
			}
			if (e2 > e3) {
				entPref[1] = t2;
				entPref[2] = t3;
			} else {
				entPref[1] = t3;
				entPref[2] = t2;
			}
		} else if (e2 > e1 && e2 > e3) {
			entPref[0] = t2;
			// Don't limit the best option, but limit the other two
			if (e1 < limit) {
				t1 = -1;
			}
			if (e3 < limit) {
				t3 = -1;
			}
			if (e1 > e3) {
				entPref[1] = t1;
				entPref[2] = t3;
			} else {
				entPref[1] = t3;
				entPref[2] = t1;
			}
		} else {
			entPref[0] = t3;
			// Don't limit the best option, but limit the other two
			if (e1 < limit) {
				t1 = -1;
			}
			if (e2 < limit) {
				t2 = -1;
			}
			if (e1 > e2) {
				entPref[1] = t1;
				entPref[2] = t2;
			} else {
				entPref[1] = t2;
				entPref[2] = t1;
			}
		}
		return entPref;
	}

	/*private int bestEntDay(int inFlight, int outFlight, int type) {
		for (int i = inFlight; i < outFlight; i++) {
			int auction = agent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT, type, i);
			if (agent.getAllocation(auction) < agent.getOwn(auction)) {
					return auction;
			}
		}
		// If no left, just take the first...
		return agent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT, type, inFlight);
	}

	private int nextEntType(int client, int lastType) {
		int e1 = agent.getClientPreference(client, TACAgent.E1);
		int e2 = agent.getClientPreference(client, TACAgent.E2);
		int e3 = agent.getClientPreference(client, TACAgent.E3);

		// At least buy what each agent wants the most!!!
		if ((e1 > e2) && (e1 > e3) && lastType == -1)
			return TACAgent.TYPE_ALLIGATOR_WRESTLING;
		if ((e2 > e1) && (e2 > e3) && lastType == -1)
			return TACAgent.TYPE_AMUSEMENT;
		if ((e3 > e1) && (e3 > e2) && lastType == -1)
			return TACAgent.TYPE_MUSEUM;
		return -1;
	}*/

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
		availableHotels = hotels;
	}



	// -------------------------------------------------------------------
	// Only for backward compability
	// -------------------------------------------------------------------

	public static void main (String[] args) {
		TACAgent.main(args);
	}

} // DummyAgent
