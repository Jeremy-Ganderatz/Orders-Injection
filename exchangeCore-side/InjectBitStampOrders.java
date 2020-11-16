package exchange.core2.tests.examples;

import exchange.core2.core.ExchangeApi;
import exchange.core2.core.ExchangeCore;
import exchange.core2.core.IEventsHandler;
import exchange.core2.core.SimpleEventsProcessor;
import exchange.core2.core.common.*;
import exchange.core2.core.common.api.*;
import exchange.core2.core.common.api.binary.BatchAddSymbolsCommand;
import exchange.core2.core.common.cmd.CommandResultCode;
import exchange.core2.core.common.config.ExchangeConfiguration;
import lombok.extern.slf4j.Slf4j;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Slf4j
public class InjectBitStampOrders {

	@Test
	public void sampleTest() throws Exception {

		SimpleEventsProcessor eventsProcessor = new SimpleEventsProcessor(new IEventsHandler() {
			@Override
			public void tradeEvent(TradeEvent tradeEvent) {
				System.out.println("Trade event: " + tradeEvent);
			}

			@Override
			public void reduceEvent(ReduceEvent reduceEvent) {
				System.out.println("Reduce event: " + reduceEvent);
			}

			@Override
			public void rejectEvent(RejectEvent rejectEvent) {
				System.out.println("Reject event: " + rejectEvent);
			}

			@Override
			public void commandResult(ApiCommandResult commandResult) {
				System.out.println("Command result: " + commandResult);
			}

			@Override
			public void orderBook(OrderBook orderBook) {
				System.out.println("OrderBook event: " + orderBook);
			}
		});

		ExchangeConfiguration conf = ExchangeConfiguration.defaultBuilder().build();

		// build exchange core
		ExchangeCore exchangeCore = ExchangeCore.builder().resultsConsumer(eventsProcessor).exchangeConfiguration(conf)
				.build();

		// start up disruptor threads
		exchangeCore.startup();

		// get exchange API for publishing commands
		ExchangeApi api = exchangeCore.getApi();
		final int currencyCodeXbt = 11;

		// symbol constants
		final int symbolXbtLtc = 241;

		Future<CommandResultCode> future;

		// create symbol specification and publish it
		CoreSymbolSpecification symbolSpecXbtLtc = CoreSymbolSpecification.builder().symbolId(symbolXbtLtc)
				.type(SymbolType.CURRENCY_EXCHANGE_PAIR).build();

		future = api.submitBinaryDataAsync(new BatchAddSymbolsCommand(symbolSpecXbtLtc));
		System.out.println("BatchAddSymbolsCommand result: " + future.get());

		// read Json file
		JSONArray result = readJson();

		for (Object o : result) {
			JSONObject jsonObj = parseExchangeCoreObject((JSONObject) o);

			// create user
			future = api.submitCommandAsync(ApiAddUser.builder().uid((long) jsonObj.get("uid")).build());
			System.out.println("ApiAddUser" + (long) jsonObj.get("uid") + "result: " + future.get());

			// user place order

			long action = (long) jsonObj.get("action"); // weird thing-cannot directly cast

			// checking ASK or BID
			if ((int) action == 1) {
				long price = (long) jsonObj.get("price");
				long reservedPrice = (long) (price * 1.0128);

				// user deposit
				future = api.submitCommandAsync(
						ApiAdjustUserBalance.builder().uid((long) jsonObj.get("uid")).currency(currencyCodeXbt)
								.amount(reservedPrice).transactionId((long) GenerateRandomId()).build());

				future = api.submitCommandAsync(ApiPlaceOrder.builder().uid((long) jsonObj.get("uid"))
						.orderId((long) jsonObj.get("orderId")).price((long) jsonObj.get("price"))
						.reservePrice(reservedPrice).size((long) jsonObj.get("size"))
						.action(OrderAction.of((byte) action)).orderType(OrderType.GTC).symbol(symbolXbtLtc).build());
			} else {
				// user deposit
				future = api.submitCommandAsync(
						ApiAdjustUserBalance.builder().uid((long) jsonObj.get("uid")).currency(currencyCodeXbt)
								.amount((long) jsonObj.get("price")).transactionId((long) GenerateRandomId()).build());

				future = api.submitCommandAsync(ApiPlaceOrder.builder().uid((long) jsonObj.get("uid"))
						.orderId((long) jsonObj.get("orderId")).price((long) jsonObj.get("price"))
						.size((long) jsonObj.get("size")).action(OrderAction.of((byte) action)).orderType(OrderType.GTC)
						.symbol(symbolXbtLtc).build());
			}

			System.out.println("ApiPlaceOrder result: " + future.get());

		}
		// request order book
		CompletableFuture<L2MarketData> orderBookFuture = api.requestOrderBookAsync(symbolXbtLtc, 10);
		System.out.println("ApiOrderBookRequest result: " + orderBookFuture.get());
	}

	private JSONArray readJson() {

		// JSON parser object to parse read file
		JSONParser jsonParser = new JSONParser();
		JSONArray exchangeCoreDataList = null;

		try (FileReader reader = new FileReader(
				"/Users/jeremyganderatz/Desktop/Test-MES-Maarkt/java-bitstamp-api-client/logs/liveOrderLogFile1.json")) {
			// Read JSON file
			Object obj = jsonParser.parse(reader);

			exchangeCoreDataList = (JSONArray) obj;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return exchangeCoreDataList;
	}

	private static JSONObject parseExchangeCoreObject(JSONObject exhangeCoreData) {
		// Get exhangeCoreData object within list
		JSONObject exchangeCoreObject = (JSONObject) exhangeCoreData.get("exCoreData");
		return exchangeCoreObject;
	}

	public static int GenerateRandomId() {

		Random randomId = new Random();
		int low = 1;
		int high = 10000;
		int id = randomId.nextInt(high - low) + low;

		return id;

	}
}
