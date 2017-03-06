package ng.verified.bckend.wallet.jbeans;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class TransactionLogs {
	
	private String referenceNo ;
	private String key ;
	private String serviceName ;
	
	private Timestamp timestamp ;
	
	private boolean serviced ;
	
	private BigDecimal cost ;
	
	public TransactionLogs() {
		// TODO Auto-generated constructor stub
	}
	
	public TransactionLogs(String referenceNo, String key, String serviceName, 
			Timestamp timestamp, boolean serviced, BigDecimal cost) {
		// TODO Auto-generated constructor stub
		
		this.cost = cost;
		this.key = key;
		this.referenceNo = referenceNo;
		this.serviced = serviced;
		this.serviceName = serviceName;
		this.timestamp = timestamp;
	}

	public String getReferenceNo() {
		return referenceNo;
	}

	public void setReferenceNo(String referenceNo) {
		this.referenceNo = referenceNo;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public Timestamp getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Timestamp timestamp) {
		this.timestamp = timestamp;
	}

	public boolean isServiced() {
		return serviced;
	}

	public void setServiced(boolean serviced) {
		this.serviced = serviced;
	}

	public BigDecimal getCost() {
		return cost;
	}

	public void setCost(BigDecimal cost) {
		this.cost = cost;
	}
	
}