package com.company.bitstampclient;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.company.bitstampclient.messages.liveorder.LiveOrder;
import com.company.bitstampclient.messages.liveorder.LiveOrderData;
import com.google.gson.JsonIOException;


public class CollectReformatAndStoreLiveOrder {
	
	static int count = 0;
	static int fileNum = 0;
	
	@SuppressWarnings("unchecked")
	public static void reformatAndStore(LiveOrder liveorder,
			JSONArray exCorDataList) throws JsonIOException, IOException {

		LiveOrderData liveOrderData = liveorder.getData();
		count++;
		if (count < 100) {
			exCorDataList.add(writeJson(liveOrderData));
		} else {
			count = 0;
			System.out.println(count);
			fileNum++;
			System.out.println(fileNum);
			recordJson(exCorDataList, fileNum);
			exCorDataList.clear();
			System.out.println(exCorDataList);
		}
	}
	
	@SuppressWarnings("unchecked")
	private static JSONObject writeJson (LiveOrderData liveOrderData) {
		
		JSONObject exCorDataDetails = new JSONObject();
		exCorDataDetails.put("uid", GenerateRandomUsersId());
		exCorDataDetails.put("orderId", liveOrderData.getId());
		exCorDataDetails.put("price", Math.round(liveOrderData.getPrice() * 10000));
		exCorDataDetails.put("reservePrice", Math.round((liveOrderData.getPrice() * 1.05)) * 10000);
		exCorDataDetails.put("size", (long) liveOrderData.getAmount());
		exCorDataDetails.put("action", liveOrderData.getOrder_type());
		exCorDataDetails.put("orderType", 0);
		exCorDataDetails.put("symbol", (int)241);
		
		JSONObject exCorDataObject = new JSONObject(); 
		exCorDataObject.put("exCoreData", exCorDataDetails);
        
        return exCorDataObject;
         
	}
	
	public static void recordJson(JSONArray exCorDataList, int fileNum) {
		//Write JSON file
        try (FileWriter file = new FileWriter("logs/liveOrderLogFile" + fileNum + ".json", true)) {
 
            file.write(exCorDataList.toJSONString());
            file.flush();
 
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	public static int GenerateRandomUsersId() {

		Random randomUsersId = new Random();
		int low = 1;
		int high = 10000000;
		int userId = randomUsersId.nextInt(high - low) + low;
		
		return userId;

	}

}
