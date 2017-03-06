package ng.verified.bckend.wallet.services;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import ng.verified.bckend.wallet.tools.QueryManager;
import ng.verified.jpa.ClientUser;
import ng.verified.jpa.enums.TransactionType;

@Path(value = "/api")
public class WalletService {
	
	private Logger log = Logger.getLogger(getClass());
	
	@Inject
	private QueryManager queryManager ;
	
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