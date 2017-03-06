package ng.verified.bckend.wallet.services;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.bson.Document;
import org.jboss.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import ng.verified.bckend.wallet.jbeans.TransactionLogs;
import ng.verified.bckend.wallet.tools.QueryManager;
import ng.verified.jpa.Client;
import ng.verified.jpa.ClientKeys;
import ng.verified.jpa.ClientUser;
import ng.verified.jpa.WalletStatement;
import ng.verified.jpa.Wrapper;
import ng.verified.jpa.enums.TransactionType;
import ng.verified.mongotool.DocumentService;
import ng.verified.mongotool.enums.documents.Transactions;

@Path(value = "/api")
public class WalletService {
	
	private Logger log = Logger.getLogger(getClass());
	
	@Inject
	private QueryManager queryManager ;
	
	@Inject
	private DocumentService documentService ;
	
	@GET
	@Path(value = "/wallserv/test")
	public Response test(){
		
		return Response.ok().entity("Backend service hit successfully").build();
	}
	
	@GET
	@Path(value = "/wallserv")
	public Response doWalletService(@HeaderParam(value = "Authorization") String bearer, 
			@HeaderParam(value = "userid") String useridstring){
		
		ClientUser clientUser = null;

		try {
			long userid = Long.parseLong(useridstring);
			clientUser = queryManager.getClientUserDataByUseridAndEagerProperties(userid, "client");
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			log.error("", e);
			return Response.serverError().entity(e.getClass()).build();
		}
		
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("Authorization", bearer);
		jsonObject.addProperty("walletBalance", getClientWalletBalance(clientUser));
		jsonObject.addProperty("lastTopUp", getClientWalletLastTopup(clientUser));
		jsonObject.addProperty("averageDailyTransactionCost", getClientWalletAverageDailyTxnCost(clientUser));
		
		return Response.ok(new Gson().toJson(jsonObject)).build();
	}
	
	@GET
	@Path(value = "/wallserv/log")
	public Response doWalletServiceLog(@HeaderParam(value = "Authorization") String bearer, 
			@HeaderParam(value = "userid") String useridstring){
		
		ClientUser clientUser = null;

		try {
			long userid = Long.parseLong(useridstring);
			clientUser = queryManager.getClientUserDataByUseridAndEagerProperties(userid, "client");
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			log.error("", e);
			return Response.serverError().entity(e.getClass()).build();
		}
		
		JsonArray jsonArray = new JsonArray();
		
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("Authorization", bearer);
		
		List<TransactionLogs> transactionLogs = getTransactionLogByClient(clientUser.getClient());
		if (transactionLogs != null)
			for (TransactionLogs transactionLog : transactionLogs){
				JsonObject element = new JsonObject();
				element.addProperty("cost", transactionLog.getCost());
				element.addProperty("transactionRef", transactionLog.getReferenceNo());
				element.addProperty("serviceKey", transactionLog.getKey());
				element.addProperty("serviceName", transactionLog.getServiceName());
				element.addProperty("lastUsed", transactionLog.getTimestamp().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
				element.addProperty("status", transactionLog.isServiced());
				jsonArray.add(element);
			}
		
		jsonObject.add("logs", jsonArray);
		
		return Response.ok(new Gson().toJson(jsonObject)).build();
	}

	/**
	 * Create TransactionLog data from {@link WalletStatement} and Transaction document in Mongodb using referenceNo shared property.
	 * 
	 * @param client
	 * @return {@link TransactionLogs}
	 */
	private List<TransactionLogs> getTransactionLogByClient(Client client) {
		// TODO Auto-generated method stub
		
		List<ClientKeys> clientKeys = queryManager.getClientKeysByClient(client);
		if (clientKeys == null)
			return null;
		
		List<TransactionLogs> transactionLogs = new ArrayList<>();
		
		for (ClientKeys clientKey : clientKeys){
			Wrapper wrapper = clientKey.getWrapper();
			transactionLogs.addAll(queryManager.getTransactionLogsByWrapper(wrapper));
		}
		
		for (TransactionLogs transactionLog : transactionLogs){
			Document document = documentService.getInvocationDocumentByTransactionReference(transactionLog.getReferenceNo());
			if (document != null)
				transactionLog.setServiced(document.getBoolean(Transactions.is_serviced.name()));
		}
		
		return transactionLogs;
	}

	/**
	 * Compute Client daily average expense on API invocations.
	 * 
	 * @param clientUser
	 * @return cost
	 */
	private BigDecimal getClientWalletAverageDailyTxnCost(ClientUser clientUser) {
		// TODO Auto-generated method stub
		
		BigDecimal cost = queryManager.calculateTotalClientTransactionCost(clientUser);
		if (cost.compareTo(BigDecimal.ZERO) == 0)
			return BigDecimal.ZERO;
		
		Timestamp begin = queryManager.getFirstWalletStatementTimestampByClientUserAndTransactiontype(clientUser, TransactionType.DEBIT);
		Timestamp end = queryManager.getLastWalletStatementTimestampByClientUserAndTransactiontype(clientUser, TransactionType.DEBIT);
		
		long days = ChronoUnit.DAYS.between(begin.toLocalDateTime(), end.toLocalDateTime());
		
		return cost.divide(BigDecimal.valueOf(days));
	}

	/**
	 * Fetch latest wallet top-up by client.
	 * 
	 * @param clientUser
	 * @return top-up
	 */
	private BigDecimal getClientWalletLastTopup(ClientUser clientUser) {
		// TODO Auto-generated method stub
		
		return queryManager.getClientWalletLastTopup(clientUser);
	}

	/**
	 * Fetch wallet balance by client.
	 * 
	 * @param clientUser
	 * @return balance
	 */
	private BigDecimal getClientWalletBalance(ClientUser clientUser) {
		// TODO Auto-generated method stub
		
		return queryManager.getClientWalletBalance(clientUser.getUserid());
	}

}