package ng.verified.bckend.wallet.services;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
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
import ng.verified.jpa.Wallet;
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
	
	/**
	 * Retrieve basic client {@link Wallet} details.
	 * 
	 * @param bearer
	 * @param useridstring
	 * @return {@link Response} containing basic properties of wallet data
	 */
	@GET
	@Path(value = "/wallserv")
	@Produces(MediaType.APPLICATION_JSON)
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
		jsonObject.addProperty("walletBalance", getClientWalletBalance(clientUser));
		jsonObject.addProperty("lastTopUp", getClientWalletLastTopup(clientUser));
		jsonObject.addProperty("averageDailyTransactionCost", getClientWalletAverageDailyTxnCost(clientUser));
		
		return Response.ok(new Gson().toJson(jsonObject)).build();
	}
	
	/**
	 * Retrieve paginated list of transaction logs for client invocations.
	 * 
	 * @param bearer
	 * @param useridstring
	 * @param startPostion
	 * @param maxResults
	 * @param sort
	 * @return Response contains a list of {@link TransactionLogs}
	 */
	@GET
	@Path(value = "/wallserv/log")
	@Produces(MediaType.APPLICATION_JSON)
	public Response doWalletServiceLog(@HeaderParam(value = "Authorization") String bearer, @HeaderParam(value = "userid") String useridstring, 
			@QueryParam(value = "page") String startPostion, @QueryParam(value = "size") String maxResults, @QueryParam(value = "sort") String sort){
		
		ClientUser clientUser = null;

		try {
			long userid = Long.parseLong(useridstring);
			clientUser = queryManager.getClientUserDataByUseridAndEagerProperties(userid, "client");
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			log.error("", e);
			return Response.serverError().entity(e.getClass()).build();
		}
		
		if (startPostion == null || startPostion.isEmpty()){
			startPostion = "0";
			maxResults = "10";
		}

		int index = 0;
		int max = 10;

		JsonObject jsonObject = new JsonObject();

		try {
			index = Integer.parseInt(startPostion);
			max = Integer.parseInt(maxResults);
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			log.error("", e);
			jsonObject.addProperty("message", "Invalid pagination values found in request query parameter");
			return Response.serverError().entity(new Gson().toJson(jsonObject)).build();
		}
		
		JsonArray jsonArray = new JsonArray();
		
		List<TransactionLogs> transactionLogs = getTransactionLogByClient(clientUser.getClient(), index, max, sort);
		if (transactionLogs != null)
			for (TransactionLogs transactionLog : transactionLogs){
				JsonObject element = new JsonObject();
				element.addProperty("cost", transactionLog.getCost());
				element.addProperty("transactionRef", transactionLog.getReferenceNo());
				element.addProperty("serviceKey", transactionLog.getKey());
				element.addProperty("serviceName", transactionLog.getServiceName());
				element.addProperty("lastUsed", transactionLog.getTimestamp().toString());
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
	 * @param index
	 * @param max
	 * @param sort
	 * @return {@link TransactionLogs}
	 */
	private List<TransactionLogs> getTransactionLogByClient(Client client, int index, int max, String sort) {
		// TODO Auto-generated method stub
		
		List<ClientKeys> clientKeys = queryManager.getClientKeysByClient(client);
		if (clientKeys == null)
			return null;
		
		List<TransactionLogs> transactionLogs = new ArrayList<>();
		
		for (ClientKeys clientKey : clientKeys){
			Wrapper wrapper = clientKey.getWrapper();
			if (wrapper == null)
				continue;
			
			List<TransactionLogs> txnLogs = queryManager.getTransactionLogsByWrapper(wrapper, sort);
			
			if (txnLogs != null)
				transactionLogs.addAll(txnLogs);
		}
		
		for (TransactionLogs transactionLog : transactionLogs){
			Document document = documentService.getInvocationDocumentByTransactionReference(transactionLog.getReferenceNo());
			if (document != null)
				transactionLog.setServiced(document.getBoolean(Transactions.is_serviced.name()));
		}
		
		int maxResults = index + max ;
		maxResults = (transactionLogs.size() >= maxResults) ? maxResults : transactionLogs.size() ;
		
		transactionLogs.subList(index, maxResults);
		
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
		if (cost == null || cost.compareTo(BigDecimal.ZERO) == 0)
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