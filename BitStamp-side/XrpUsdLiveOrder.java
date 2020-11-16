package com.company.bitstampclient;

import com.company.bitstampclient.messages.liveorder.LiveOrder;
import com.company.bitstampclient.messages.liveorder.LiveOrderData;
import com.company.bitstampclient.observers.LiveOrderObserver;
import com.google.gson.JsonIOException;

import java.io.IOException;

import org.json.simple.JSONArray;
import org.slf4j.LoggerFactory;

public class XrpUsdLiveOrder implements LiveOrderObserver {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(XrpUsdLiveOrder.class);
	int count = 0;
	int fileNum = 0;
	JSONArray exCorDataList = new JSONArray();

	@Override
	public void receive(LiveOrder liveOrder) {

		if (liveOrder.getEvent().equals("order_created")) {

			LiveOrderData data = liveOrder.getData();

			if (data.getAmount() > 5000) {
				LOG.info("Live Order: {}", liveOrder);

				try {
					System.out.println(count);
					CollectReformatAndStoreLiveOrder.reformatAndStore(liveOrder, exCorDataList);
					count++;
				} catch (JsonIOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else {
				LOG.info("Received order {} but it was ignored.", data.getId());
			}
		}

	}

}
